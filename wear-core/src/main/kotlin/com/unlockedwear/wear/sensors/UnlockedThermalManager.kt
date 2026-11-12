package com.unlockedwear.wear.sensors

import android.content.Context
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTracker.TrackerError
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import com.unlockedwear.protocol.EwmaCalculator
import com.unlockedwear.protocol.ThermalFrame
import com.unlockedwear.protocol.ThermalSensorStatus
import com.unlockedwear.protocol.compensateSkinTemp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Samsung Health Sensor SDK skin temperature tracking on Galaxy Watch.
 *
 * Supports:
 * - Continuous background skin temperature tracking ([HealthTrackerType.SKIN_TEMPERATURE_CONTINUOUS])
 * - High-precision on-demand spot measurements ([HealthTrackerType.SKIN_TEMPERATURE_ON_DEMAND])
 * - IR heat flux compensation using [compensateSkinTemp]
 * - Baseline tracking and statistical Z-score computation for circadian/fever detection using [EwmaCalculator]
 */
@Singleton
class UnlockedThermalManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkInitializer: SamsungSdkInitializer
) {

    companion object {
        private const val TAG = "UnlockedThermalManager"
        private const val DEFAULT_BASELINE_TEMP = 34.2f
    }

    private val ewmaCalculator = EwmaCalculator(alpha = 0.05)

    /**
     * Streams skin temperature frames from Samsung Health Sensor SDK.
     *
     * @param onDemand If true, uses [HealthTrackerType.SKIN_TEMPERATURE_ON_DEMAND] for high-frequency
     *                 spot check; otherwise uses [HealthTrackerType.SKIN_TEMPERATURE_CONTINUOUS].
     */
    fun streamSkinTemperature(onDemand: Boolean = false): Flow<ThermalFrame> = callbackFlow {
        val trackerType = if (onDemand) {
            HealthTrackerType.SKIN_TEMPERATURE_ON_DEMAND
        } else {
            HealthTrackerType.SKIN_TEMPERATURE_CONTINUOUS
        }

        Log.i(TAG, "Starting skin temperature stream (onDemand=$onDemand, trackerType=$trackerType)")

        val isTrackerActive = AtomicBoolean(false)
        var localService: HealthTrackingService? = null
        var healthTracker: HealthTracker? = null

        val trackerEventListener = object : TrackerEventListener {
            override fun onDataReceived(dataPointList: List<DataPoint>) {
                for (dataPoint in dataPointList) {
                    try {
                        val skinTempRaw = dataPoint.getValue(ValueKey.SkinTemperatureSet.OBJECT_TEMPERATURE)
                        val ambientTempRaw = dataPoint.getValue(ValueKey.SkinTemperatureSet.AMBIENT_TEMPERATURE)
                        val statusRaw = dataPoint.getValue(ValueKey.SkinTemperatureSet.STATUS)
                        val timestamp = dataPoint.timestamp

                        val skinTemp = skinTempRaw.toFloat()
                        val ambientTemp = ambientTempRaw.toFloat()

                        if (skinTemp > 0f) {
                            val compensatedTemp = compensateSkinTemp(skinTemp, ambientTemp)
                            ewmaCalculator.update(compensatedTemp)

                            val status = when (statusRaw) {
                                0 -> ThermalSensorStatus.STATUS_VALID
                                1 -> ThermalSensorStatus.STATUS_ACCURACY_HIGH
                                2 -> ThermalSensorStatus.STATUS_ACCURACY_LOW
                                else -> ThermalSensorStatus.STATUS_VALID
                            }

                            val frame = ThermalFrame(
                                skinTempCelsius = skinTemp,
                                ambientTempCelsius = ambientTemp,
                                compensatedTempCelsius = compensatedTemp,
                                timestampMs = timestamp,
                                status = status
                            )

                            trySend(frame)
                        } else {
                            Log.w(TAG, "Received non-positive skin temp reading: $skinTemp, status=$statusRaw")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing SkinTemperature DataPoint", e)
                    }
                }
            }

            override fun onError(trackerError: TrackerError) {
                Log.e(TAG, "HealthTracker error received: $trackerError")
                if (trackerError == TrackerError.PERMISSION_ERROR ||
                    trackerError == TrackerError.SDK_POLICY_ERROR
                ) {
                    close(IllegalStateException("Samsung Health Tracker error: $trackerError"))
                }
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "Skin temperature flush completed")
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "HealthTrackingService connected for temperature tracking.")
                val service = localService ?: return
                try {
                    val tracker = service.getHealthTracker(trackerType)
                    if (tracker == null) {
                        Log.e(TAG, "Tracker $trackerType is not supported or null")
                        close(UnsupportedOperationException("Tracker type $trackerType not supported on this device"))
                        return
                    }
                    healthTracker = tracker
                    tracker.setEventListener(trackerEventListener)
                    isTrackerActive.set(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize health tracker listener", e)
                    close(e)
                }
            }

            override fun onConnectionEnded() {
                Log.w(TAG, "HealthTrackingService connection ended")
                isTrackerActive.set(false)
            }

            override fun onConnectionFailed(error: HealthTrackerException) {
                Log.e(TAG, "HealthTrackingService connection failed: code=${error.errorCode}, ${error.message}")
                close(error)
            }
        }

        try {
            val service = HealthTrackingService(connectionListener, context)
            localService = service
            service.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting HealthTrackingService", e)
            close(e)
        }

        awaitClose {
            Log.i(TAG, "Cleaning up skin temperature tracker stream...")
            if (isTrackerActive.getAndSet(false)) {
                try {
                    healthTracker?.unsetEventListener()
                } catch (e: Exception) {
                    Log.w(TAG, "Error unsetting tracker event listener", e)
                }
            }
            try {
                localService?.disconnectService()
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting HealthTrackingService", e)
            }
        }
    }

    /**
     * Returns current running baseline temperature from EWMA filter.
     */
    fun getBaselineTemperature(): Float = ewmaCalculator.mean.toFloat().takeIf { it > 0f } ?: DEFAULT_BASELINE_TEMP

    /**
     * Returns current running z-score of skin temperature from baseline.
     */
    fun getCurrentZScore(): Float = ewmaCalculator.zScore.toFloat()
}
