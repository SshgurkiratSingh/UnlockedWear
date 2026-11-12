package com.unlockedwear.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.unlockedwear.wear.data.BiometricReadingEntity
import com.unlockedwear.wear.data.DataSyncService
import com.unlockedwear.wear.data.ThermalReadingEntity
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.sensors.HeartRateManager
import com.unlockedwear.wear.sensors.HeartRateReading
import com.unlockedwear.wear.sensors.SamsungSdkInitializer
import com.unlockedwear.wear.sensors.UnlockedThermalManager
import com.unlockedwear.wear.ui.MainWearActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that continuously streams thermal and biometric sensor data
 * from the Galaxy Watch's Samsung Health Sensor SDK and synchronises each reading
 * to the paired Galaxy S25 via the Wearable Data Layer.
 *
 * ## Why a foreground service?
 * Android Wear OS 4+ kills background coroutines in activities after ~30 s off-wrist.
 * A FOREGROUND_SERVICE_HEALTH keeps the sensor streams alive throughout the day and
 * night, ensuring the mobile companion always has up-to-date temperature readings.
 *
 * ## Dispatch cadence
 * - Thermal: every frame emitted by [UnlockedThermalManager.streamSkinTemperature]
 *   (typically ~1 Hz continuous, ~5 s on-demand).
 * - Heart rate + HRV: every frame emitted by [HeartRateManager.streamHeartRate]
 *   (typically ~1 Hz), combined into a [BiometricReadingEntity] with the latest
 *   thermal values and pushed via [DataSyncService.dispatchBiometricSnapshot].
 * - Both are also persisted locally into [WearDatabase] for watch-side display.
 *
 * ## Restart behaviour
 * Both sensor flows use `retryWhen` with exponential back-off so transient Samsung SDK
 * disconnections (e.g. watch lifted off wrist) don't kill the service permanently.
 */
@AndroidEntryPoint
class SensorForegroundService : Service() {

    companion object {
        private const val TAG = "SensorForegroundService"

        const val CHANNEL_ID   = "unlockedwear_sensor_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.unlockedwear.wear.START_SENSORS"
        const val ACTION_STOP  = "com.unlockedwear.wear.STOP_SENSORS"

        fun startIntent(context: Context): Intent =
            Intent(context, SensorForegroundService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context): Intent =
            Intent(context, SensorForegroundService::class.java).apply { action = ACTION_STOP }
    }

    @Inject lateinit var thermalManager: UnlockedThermalManager
    @Inject lateinit var heartRateManager: HeartRateManager
    @Inject lateinit var sdkInitializer: SamsungSdkInitializer
    @Inject lateinit var database: WearDatabase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Keep the latest thermal values so the HR job can embed them in biometric snapshots
    @Volatile private var latestSkinTempC    = 33.5f
    @Volatile private var latestAmbientTempC = 24.0f
    @Volatile private var latestThermalZScore = 0.0

    private var thermalJob: Job? = null
    private var heartRateJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "STOP_SENSORS received — stopping foreground service")
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // ACTION_START or null (system restart)
                Log.i(TAG, "START_SENSORS — launching sensor streams")
                sdkInitializer.initialize()
                startThermalStream()
                startHeartRateStream()
            }
        }
        // Sticky: restart with the last intent if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "SensorForegroundService destroyed — cancelling coroutine scope")
        serviceScope.cancel()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Thermal stream
    // ─────────────────────────────────────────────────────────────────────────

    private fun startThermalStream() {
        thermalJob?.cancel()
        thermalJob = serviceScope.launch {
            var retryDelay = 5_000L
            thermalManager.streamSkinTemperature(onDemand = false)
                .retryWhen { cause, attempt ->
                    Log.w(TAG, "Thermal stream error (attempt $attempt): ${cause.message}")
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(120_000L)
                    true  // always retry
                }
                .catch { e -> Log.e(TAG, "Thermal stream terminated fatally: ${e.message}") }
                .collect { frame ->
                    retryDelay = 5_000L  // reset back-off on successful frame

                    // Cache for biometric snapshot assembly
                    latestSkinTempC     = frame.skinTempCelsius
                    latestAmbientTempC  = frame.ambientTempCelsius
                    latestThermalZScore = thermalManager.getCurrentZScore().toDouble()

                    val baseline = thermalManager.getBaselineTemperature()
                    val delta    = frame.compensatedTempCelsius - baseline
                    val zScore   = thermalManager.getCurrentZScore()

                    // 1. Persist locally
                    val entity = ThermalReadingEntity(
                        timestamp        = frame.timestampMs,
                        skinTemp         = frame.skinTempCelsius,
                        ambientTemp      = frame.ambientTempCelsius,
                        compensatedTemp  = frame.compensatedTempCelsius,
                        deltaFromBaseline = delta,
                        zScore           = zScore,
                        isOnDemand       = false
                    )
                    database.thermalDao().insert(entity)

                    // 2. Dispatch to phone (with delta + z-score)
                    DataSyncService.dispatchThermalFrame(
                        context           = applicationContext,
                        frame             = frame,
                        deltaFromBaseline = delta,
                        zScore            = zScore
                    )

                    Log.d(TAG, "Thermal dispatched: skin=${frame.skinTempCelsius}°C " +
                            "comp=${frame.compensatedTempCelsius}°C Δ=${String.format("%.2f", delta)}°C")
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heart Rate stream — combines with latest thermal for full biometric push
    // ─────────────────────────────────────────────────────────────────────────

    private fun startHeartRateStream() {
        heartRateJob?.cancel()
        heartRateJob = serviceScope.launch {
            var retryDelay = 5_000L
            var lastDispatchMs = 0L

            heartRateManager.streamHeartRate()
                .retryWhen { cause, attempt ->
                    Log.w(TAG, "Heart rate stream error (attempt $attempt): ${cause.message}")
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(120_000L)
                    true
                }
                .catch { e -> Log.e(TAG, "HR stream terminated fatally: ${e.message}") }
                .collect { hrReading ->
                    retryDelay = 5_000L

                    val now = System.currentTimeMillis()

                    // 1. Persist HR locally on every reading
                    val entity = BiometricReadingEntity(
                        timestamp    = hrReading.timestamp,
                        heartRateBpm = hrReading.bpm,
                        rmssdMs      = hrReading.rmssdMs,
                        spo2Percent  = 0,  // SpO2 is on-demand only
                        ibiListJson  = "[]"
                    )
                    database.biometricDao().insert(entity)

                    // 2. Push full biometric snapshot to phone at most once every 30 s
                    //    to avoid flooding the Data Layer
                    if (now - lastDispatchMs >= 30_000L) {
                        lastDispatchMs = now
                        val snapshot = com.unlockedwear.protocol.BiometricSnapshot(
                            heartRateBpm     = hrReading.bpm.toDouble(),
                            hrvRmssdMs       = hrReading.rmssdMs,
                            spo2Percent      = 0,
                            skinTempCelsius  = latestSkinTempC,
                            ambientTempCelsius = latestAmbientTempC,
                            thermalZScore    = latestThermalZScore,
                            timestampMs      = now
                        )
                        DataSyncService.dispatchBiometricSnapshot(applicationContext, snapshot)
                        Log.d(TAG, "Biometric snapshot dispatched: HR=${hrReading.bpm} " +
                                "RMSSD=${String.format("%.1f", hrReading.rmssdMs)}ms " +
                                "skinTemp=${latestSkinTempC}°C")
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "UnlockedWear Sensor Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Continuous Samsung BioActive sensor streaming to companion phone"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainWearActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UnlockedWear Sensors Active")
            .setContentText("Streaming thermal & biometric data to phone")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
