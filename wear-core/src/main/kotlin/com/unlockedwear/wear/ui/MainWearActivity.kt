package com.unlockedwear.wear.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.unlockedwear.wear.data.RingStateDataStore
import com.unlockedwear.wear.data.WatchFaceConfigDataStore
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.service.SensorForegroundService
import com.unlockedwear.wear.sensors.HeartRateManager
import com.unlockedwear.wear.sensors.SamsungSdkInitializer
import com.unlockedwear.wear.sensors.SpO2Manager
import com.unlockedwear.wear.sensors.UnlockedThermalManager
import com.unlockedwear.wear.ui.screens.CoPilotScreen
import com.unlockedwear.wear.ui.screens.HealthDetailScreen
import com.unlockedwear.wear.ui.screens.HomeScreen
import com.unlockedwear.wear.ui.screens.RingSelectorScreen
import com.unlockedwear.wear.ui.screens.ThermalDetailScreen
import com.unlockedwear.wear.ui.theme.UnlockedWearTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry activity for UnlockedWear on Galaxy Watch.
 *
 * Hosts [SwipeDismissableNavHost] supporting gesture back navigation between:
 * - [ROUTE_HOME]: Primary clock, Mission Ring, telemetry overview
 * - [ROUTE_THERMAL_DETAIL]: Deep skin/ambient thermal analysis & trend
 * - [ROUTE_COPILOT]: DeepSeek AI briefing, priority task & Gemini voice
 * - [ROUTE_RING_SELECTOR]: Crown-scrollable Mission Ring mode picker
 * - [ROUTE_HEALTH_DETAIL]: Continuous HR, HRV RMSSD & on-demand SpO2
 */
@AndroidEntryPoint
class MainWearActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainWearActivity"
        const val EXTRA_ROUTE = "extra_target_route"

        const val ROUTE_HOME = "home"
        const val ROUTE_THERMAL_DETAIL = "thermal_detail"
        const val ROUTE_COPILOT = "copilot"
        const val ROUTE_RING_SELECTOR = "ring_selector"
        const val ROUTE_HEALTH_DETAIL = "health_detail"
    }

    @Inject
    lateinit var sdkInitializer: SamsungSdkInitializer

    @Inject
    lateinit var ringStateDataStore: RingStateDataStore

    @Inject
    lateinit var watchFaceConfigDataStore: WatchFaceConfigDataStore

    @Inject
    lateinit var database: WearDatabase

    @Inject
    lateinit var thermalManager: UnlockedThermalManager

    @Inject
    lateinit var heartRateManager: HeartRateManager

    @Inject
    lateinit var spO2Manager: SpO2Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Launching MainWearActivity...")

        // Ensure Samsung Health Tracking service is initialized
        sdkInitializer.initialize()

        // Start continuous sensor → phone streaming foreground service
        val sensorServiceIntent = SensorForegroundService.startIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sensorServiceIntent)
        } else {
            startService(sensorServiceIntent)
        }
        Log.i(TAG, "SensorForegroundService start requested")

        val initialRoute = intent.getStringExtra(EXTRA_ROUTE) ?: ROUTE_HOME

        setContent {
            UnlockedWearTheme {
                UnlockedWearNavHost(
                    initialRoute = initialRoute,
                    ringStateDataStore = ringStateDataStore,
                    watchFaceConfigDataStore = watchFaceConfigDataStore,
                    database = database,
                    thermalManager = thermalManager,
                    heartRateManager = heartRateManager,
                    spO2Manager = spO2Manager,
                    sdkInitializer = sdkInitializer
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sdkInitializer.initialize()
    }
}

@Composable
fun UnlockedWearNavHost(
    initialRoute: String,
    ringStateDataStore: RingStateDataStore,
    watchFaceConfigDataStore: WatchFaceConfigDataStore,
    database: WearDatabase,
    thermalManager: UnlockedThermalManager,
    heartRateManager: HeartRateManager,
    spO2Manager: SpO2Manager,
    sdkInitializer: SamsungSdkInitializer
) {
    val navController = rememberSwipeDismissableNavController()
    val heartRateFlow = remember { heartRateManager.streamHeartRate() }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        composable(MainWearActivity.ROUTE_HOME) {
            HomeScreen(
                ringStateDataStore = ringStateDataStore,
                watchFaceConfigDataStore = watchFaceConfigDataStore,
                database = database,
                heartRateFlow = heartRateFlow,
                onNavigateToCoPilot = {
                    navController.navigate(MainWearActivity.ROUTE_COPILOT)
                },
                onNavigateToThermalDetail = {
                    navController.navigate(MainWearActivity.ROUTE_THERMAL_DETAIL)
                },
                onNavigateToHealthDetail = {
                    navController.navigate(MainWearActivity.ROUTE_HEALTH_DETAIL)
                },
                onNavigateToRingSelector = {
                    navController.navigate(MainWearActivity.ROUTE_RING_SELECTOR)
                }
            )
        }

        composable(MainWearActivity.ROUTE_THERMAL_DETAIL) {
            ThermalDetailScreen(
                thermalManager = thermalManager,
                database = database,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(MainWearActivity.ROUTE_COPILOT) {
            CoPilotScreen(
                database = database,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(MainWearActivity.ROUTE_RING_SELECTOR) {
            RingSelectorScreen(
                ringStateDataStore = ringStateDataStore,
                onModeSelected = {
                    navController.popBackStack()
                }
            )
        }

        composable(MainWearActivity.ROUTE_HEALTH_DETAIL) {
            HealthDetailScreen(
                heartRateFlow = heartRateFlow,
                spO2Manager = spO2Manager,
                sdkInitializer = sdkInitializer,
                database = database,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
