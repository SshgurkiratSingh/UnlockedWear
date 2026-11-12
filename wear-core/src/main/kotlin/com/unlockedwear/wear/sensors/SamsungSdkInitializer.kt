package com.unlockedwear.wear.sensors

import android.content.Context
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of the Samsung Health Sensor SDK connection on Galaxy Watch.
 *
 * Responsibilities:
 * - Initializes and maintains connection to [HealthTrackingService].
 * - Detects and reports developer mode status / policy errors ([HealthTrackerException.SDK_POLICY_ERROR]).
 * - Queries and caches tracking capabilities ([HealthTrackingService.getTrackingCapability]).
 * - Provides access to native [HealthTracker] instances for continuous and on-demand tracking.
 */
@Singleton
class SamsungSdkInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SamsungSdkInitializer"

        // Samsung SDK error code constants
        const val ERROR_NONE = 0
        const val ERROR_SDK_POLICY = -100
        const val ERROR_NOT_SUPPORTED = -1
        const val ERROR_SECURITY = -2
        const val ERROR_TIMEOUT = -3
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(
            val supportedTrackers: Set<HealthTrackerType>
        ) : ConnectionState()
        data class Error(
            val errorCode: Int,
            val message: String,
            val isDeveloperModeRequired: Boolean
        ) : ConnectionState()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var healthTrackingService: HealthTrackingService? = null
    private val isConnecting = AtomicBoolean(false)
    private val supportedTrackers = mutableSetOf<HealthTrackerType>()

    private val connectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            Log.i(TAG, "Samsung HealthTrackingService connected successfully.")
            isConnecting.set(false)
            val service = healthTrackingService
            if (service != null) {
                try {
                    val capabilities = service.trackingCapability
                    supportedTrackers.clear()
                    if (capabilities != null) {
                        supportedTrackers.addAll(capabilities.supportHealthTrackerTypes)
                    }

                    Log.i(TAG, "Available tracker types: $supportedTrackers")
                    _connectionState.value = ConnectionState.Connected(supportedTrackers.toSet())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read tracking capabilities", e)
                    _connectionState.value = ConnectionState.Connected(emptySet())
                }
            } else {
                _connectionState.value = ConnectionState.Connected(emptySet())
            }
        }

        override fun onConnectionEnded() {
            Log.w(TAG, "Samsung HealthTrackingService connection ended.")
            isConnecting.set(false)
            supportedTrackers.clear()
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun onConnectionFailed(error: HealthTrackerException) {
            isConnecting.set(false)
            val errorCode = error.errorCode
            val errorMsg = error.message ?: "Unknown Samsung Health Tracking SDK error"
            val isDeveloperModeRequired = (errorCode == ERROR_SDK_POLICY ||
                    errorMsg.contains("policy", ignoreCase = true) ||
                    errorMsg.contains("developer", ignoreCase = true))

            Log.e(TAG, "Samsung HealthTrackingService connection failed: code=$errorCode, msg=$errorMsg, developerModeNeeded=$isDeveloperModeRequired")

            _connectionState.value = ConnectionState.Error(
                errorCode = errorCode,
                message = if (isDeveloperModeRequired) {
                    "Samsung Health Developer Mode required: Open Samsung Health on Watch -> Settings -> About -> Tap version 10 times -> Toggle Developer Mode ON -> Reboot Watch."
                } else {
                    errorMsg
                },
                isDeveloperModeRequired = isDeveloperModeRequired
            )
        }
    }

    /**
     * Connects to Samsung HealthTrackingService asynchronously.
     */
    @Synchronized
    fun initialize() {
        if (_connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Already connected to HealthTrackingService.")
            return
        }
        if (isConnecting.getAndSet(true)) {
            Log.d(TAG, "Connection already in progress.")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        try {
            Log.i(TAG, "Connecting to Samsung HealthTrackingService...")
            val service = HealthTrackingService(connectionListener, context)
            healthTrackingService = service
            service.connectService()
        } catch (e: Exception) {
            isConnecting.set(false)
            Log.e(TAG, "Exception initializing Samsung HealthTrackingService", e)
            _connectionState.value = ConnectionState.Error(
                errorCode = -999,
                message = e.localizedMessage ?: "Failed to instantiate HealthTrackingService",
                isDeveloperModeRequired = false
            )
        }
    }

    /**
     * Checks if a specific Samsung HealthTrackerType is supported by the current watch hardware.
     */
    fun isTrackerSupported(trackerType: HealthTrackerType): Boolean {
        return supportedTrackers.contains(trackerType)
    }

    /**
     * Obtains a native [HealthTracker] for the requested [HealthTrackerType].
     * Returns null if service is not connected or tracker type is unsupported.
     */
    fun getTracker(trackerType: HealthTrackerType): HealthTracker? {
        val service = healthTrackingService ?: run {
            Log.w(TAG, "HealthTrackingService is null when requesting tracker $trackerType")
            return null
        }
        return try {
            service.getHealthTracker(trackerType)
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring tracker for $trackerType", e)
            null
        }
    }

    /**
     * Disconnects the Samsung Health Tracking service.
     */
    @Synchronized
    fun disconnect() {
        try {
            healthTrackingService?.disconnectService()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting HealthTrackingService", e)
        } finally {
            healthTrackingService = null
            isConnecting.set(false)
            supportedTrackers.clear()
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}
