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

/**
 * Data model for Blood Oxygen (SpO2) readings from Samsung Sensor SDK.
 */
data class SpO2Reading(
    val spo2Percent: Int,
    val status: Int,
    val heartRate: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_MEASURING = 0
        const val STATUS_SUCCESS = 1
        const val STATUS_LOW_SIGNAL = 2
        const val STATUS_DEVICE_MOVING = 3
        const val STATUS_CALCULATING = -1
    }

    val isSuccessful: Boolean get() = (status == STATUS_SUCCESS || (spo2Percent in 70..100))
}

/**
 * Manages Samsung Health Sensor SDK Blood Oxygen (SpO2) measurements on Galaxy Watch.
 *
 * Responsibilities:
 * - Initiates on-demand SpO2 spot checks ([HealthTrackerType.SPO2_ON_DEMAND])
 * - Listens for Samsung [ValueKey.SpO2Set.SPO2] and [ValueKey.SpO2Set.STATUS]
 * - Emits real-time progress updates until stable calculation completes
 * - Graceful connection cleanup upon flow termination
 */
@Singleton
class SpO2Manager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sdkInitializer: SamsungSdkInitializer
) {

    companion object {
        private const val TAG = "SpO2Manager"
    }

    /**
     * Initiates an on-demand SpO2 measurement session.
     */
    fun measureSpO2(): Flow<SpO2Reading> = callbackFlow {
        val trackerType = HealthTrackerType.SPO2_ON_DEMAND
        Log.i(TAG, "Starting on-demand SpO2 measurement...")

        val isTrackerActive = AtomicBoolean(false)
        var localService: HealthTrackingService? = null
        var healthTracker: HealthTracker? = null

        val trackerEventListener = object : TrackerEventListener {
            override fun onDataReceived(dataPointList: List<DataPoint>) {
                for (dataPoint in dataPointList) {
                    try {
                        val spo2Value = dataPoint.getValue(ValueKey.SpO2Set.SPO2)
                        val status = dataPoint.getValue(ValueKey.SpO2Set.STATUS)
                        val hr = try {
                            dataPoint.getValue(ValueKey.SpO2Set.HEART_RATE)
                        } catch (e: Exception) {
                            0
                        }
                        val timestamp = dataPoint.timestamp

                        val reading = SpO2Reading(
                            spo2Percent = spo2Value,
                            status = status,
                            heartRate = hr,
                            timestamp = timestamp
                        )

                        trySend(reading)

                        if (reading.isSuccessful && spo2Value > 0) {
                            Log.i(TAG, "SpO2 measurement successful: $spo2Value%, HR: $hr")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing SpO2 DataPoint", e)
                    }
                }
            }

            override fun onError(trackerError: TrackerError) {
                Log.e(TAG, "SpO2 Tracker error received: $trackerError")
                if (trackerError == TrackerError.PERMISSION_ERROR ||
                    trackerError == TrackerError.SDK_POLICY_ERROR
                ) {
                    close(IllegalStateException("Samsung SpO2 Tracker error: $trackerError"))
                }
            }

            override fun onFlushCompleted() {
                Log.d(TAG, "SpO2 flush completed")
            }
        }

        val connectionListener = object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "HealthTrackingService connected for SpO2.")
                val service = localService ?: return
                try {
                    val tracker = service.getHealthTracker(trackerType)
                    if (tracker == null) {
                        Log.e(TAG, "Tracker $trackerType is not supported or null")
                        close(UnsupportedOperationException("SpO2 tracker not supported on this device"))
                        return
                    }
                    healthTracker = tracker
                    tracker.setEventListener(trackerEventListener)
                    isTrackerActive.set(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize SpO2 tracker listener", e)
                    close(e)
                }
            }

            override fun onConnectionEnded() {
                Log.w(TAG, "HealthTrackingService connection ended for SpO2")
                isTrackerActive.set(false)
            }

            override fun onConnectionFailed(error: HealthTrackerException) {
                Log.e(TAG, "HealthTrackingService connection failed for SpO2: ${error.errorCode}, ${error.message}")
                close(error)
            }
        }

        try {
            val service = HealthTrackingService(connectionListener, context)
            localService = service
            service.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting HealthTrackingService for SpO2", e)
            close(e)
        }

        awaitClose {
            Log.i(TAG, "Cleaning up SpO2 tracker stream...")
            if (isTrackerActive.getAndSet(false)) {
                try {
                    healthTracker?.unsetEventListener()
                } catch (e: Exception) {
                    Log.w(TAG, "Error unsetting SpO2 tracker event listener", e)
                }
            }
            try {
                localService?.disconnectService()
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting HealthTrackingService for SpO2", e)
            }
        }
    }
}
