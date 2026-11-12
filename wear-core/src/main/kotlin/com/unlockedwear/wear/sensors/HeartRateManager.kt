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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Data model for heart rate and HRV inter-beat interval readings.
 */
data class HeartRateReading(
    val bpm: Int,
    val ibiListMs: List<Int>,
    val timestamp: Long = System.currentTimeMillis(),
    val rmssdMs: Double = 0.0,
    val status: Int = 0
)

/**
 * Manages Samsung Health Sensor SDK continuous Heart Rate and HRV tracking on Galaxy Watch.
 *
 * Responsibilities:
 * - Listens to [HealthTrackerType.HEART_RATE_CONTINUOUS]
 * - Extracts instantaneous heart rate (BPM)
 * - Extracts Inter-Beat Interval (IBI) lists
 * - Calculates Root Mean Square of Successive Differences (RMSSD) for real-time parasympathetic HRV
 * - Provides lifecycle-managed [callbackFlow] with robust cleanup
 */
@Singleton
class HeartRateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkInitializer: SamsungSdkInitializer
) {

    companion object {
        private const val TAG = "HeartRateManager"

        /**
         * Computes RMSSD (Root Mean Square of Successive Differences) from IBI list in ms.
         * Formula: sqrt( 1/(N-1) * sum( (IBI[i+1] - IBI[i])^2 ) )
         */
        fun computeRmssd(ibiList: List<Int>): Double {
            if (ibiList.size < 2) return 0.0
            var sumSquaredDifferences = 0.0
            var count = 0
            for (i in 0 until ibiList.size - 1) {
                val diff = (ibiList[i + 1] - ibiList[i]).toDouble()
                sumSquaredDifferences += diff * diff
                count++
            }
            return if (count > 0) sqrt(sumSquaredDifferences / count) else 0.0
        }
    }

    /**
     * Streams continuous heart rate readings and IBI list for HRV analysis.
     */
    fun streamHeartRate(): Flow<HeartRateReading> = callbackFlow {
        val trackerType = HealthTrackerType.HEART_RATE_CONTINUOUS
        Log.i(TAG, "Starting Heart Rate continuous stream...")

        val isTrackerActive = AtomicBoolean(false)
        var localService: HealthTrackingService? = null
        var healthTracker: HealthTracker? = null

        val trackerEventListener = object : TrackerEventListener {
            override fun onDataReceived(dataPointList: List<DataPoint>) {
                for (dataPoint in dataPointList) {
                    try {
                        val bpm = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE)
                        val status = dataPoint.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS)
                        val timestamp = dataPoint.timestamp

                        val rawIbiList: List<Int> = try {
                            val list = dataPoint.getValue(ValueKey.HeartRateSet.IBI_LIST)
                            @Suppress("UNCHECKED_CAST")
                            list as? List<Int> ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }

                        val rmssd = computeRmssd(rawIbiList)

                        if (bpm > 0) {
                            val reading = HeartRateReading(
                                bpm = bpm,
                                ibiListMs = rawIbiList,
                                timestamp = timestamp,
                                rmssdMs = rmssd,
                                status = status
                            )
                            trySend(reading)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing HeartRate DataPoint", e)
                    }
                }
            }

            override fun onError(trackerError: TrackerError) {
                Log.e(TAG, "HeartRate Tracker error received: $trackerError")
                if (trackerError == TrackerError.PERMISSION_ERROR ||
                    trackerError == TrackerError.SDK_POLICY_ERROR
                ) {
                    close(IllegalStateException("Samsung Heart Rate tracker error: $trackerError"))
                }
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "HeartRate flush completed")
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "HealthTrackingService connected for HeartRate.")
                val service = localService ?: return
                try {
                    val tracker = service.getHealthTracker(trackerType)
                    if (tracker == null) {
                        Log.e(TAG, "Tracker $trackerType is not supported or null")
                        close(UnsupportedOperationException("Heart rate tracker not supported on this device"))
                        return
                    }
                    healthTracker = tracker
                    tracker.setEventListener(trackerEventListener)
                    isTrackerActive.set(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize HeartRate tracker listener", e)
                    close(e)
                }
            }

            override fun onConnectionEnded() {
                Log.w(TAG, "HealthTrackingService connection ended for HeartRate")
                isTrackerActive.set(false)
            }

            override fun onConnectionFailed(error: HealthTrackerException) {
                Log.e(TAG, "HealthTrackingService connection failed for HeartRate: ${error.errorCode}, ${error.message}")
                close(error)
            }
        }

        try {
            val service = HealthTrackingService(connectionListener, context)
            localService = service
            service.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting HealthTrackingService for HeartRate", e)
            close(e)
        }

        awaitClose {
            Log.i(TAG, "Cleaning up Heart Rate tracker stream...")
            if (isTrackerActive.getAndSet(false)) {
                try {
                    healthTracker?.unsetEventListener()
                } catch (e: Exception) {
                    Log.w(TAG, "Error unsetting HeartRate tracker event listener", e)
                }
            }
            try {
                localService?.disconnectService()
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting HealthTrackingService", e)
            }
        }
    }
}
