package com.unlockedwear.wear

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.unlockedwear.wear.sensors.SamsungSdkInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Top-level Application class for UnlockedWear on Galaxy Watch.
 */
@HiltAndroidApp
class UnlockedWearApp : Application() {

    companion object {
        private const val TAG = "UnlockedWearApp"
        const val CHANNEL_ID_TELEMETRY = "unlockedwear_telemetry_channel"
        const val CHANNEL_ID_ALERTS = "unlockedwear_alerts_channel"
    }

    @Inject
    lateinit var sdkInitializer: SamsungSdkInitializer

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing UnlockedWear Application on Galaxy Watch...")

        createNotificationChannels()
        initializeSamsungSdk()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val telemetryChannel = NotificationChannel(
                CHANNEL_ID_TELEMETRY,
                "Biometric Telemetry",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous background sensor and thermal synchronization status"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Mission Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical UV, thermal threshold, and Co-Pilot notifications"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(telemetryChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    private fun initializeSamsungSdk() {
        try {
            Log.i(TAG, "Bootstrapping Samsung Health Sensor SDK connection...")
            sdkInitializer.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Failed initial bootstrap of Samsung Health Sensor SDK", e)
        }
    }
}
