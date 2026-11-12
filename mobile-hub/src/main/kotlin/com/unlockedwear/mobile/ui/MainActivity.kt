package com.unlockedwear.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unlockedwear.mobile.ui.components.UberBlack
import com.unlockedwear.mobile.ui.components.UberCardBorder
import com.unlockedwear.mobile.ui.components.UberMuted
import com.unlockedwear.mobile.ui.components.UberWhite
import com.unlockedwear.mobile.ui.screens.DashboardScreen
import com.unlockedwear.mobile.ui.screens.SettingsScreen
import com.unlockedwear.mobile.ui.screens.WatchFaceCustomizationScreen
import com.unlockedwear.mobile.ui.theme.UnlockedTheme
import com.unlockedwear.mobile.ui.viewmodels.DashboardViewModel
import com.unlockedwear.mobile.ui.viewmodels.SettingsViewModel
import com.unlockedwear.mobile.ui.viewmodels.WatchFaceCustomizationViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Destinations for bottom navigation tabs.
 */
sealed class Screen(val route: String, val title: String, val badgeText: String) {
    data object Dashboard : Screen("dashboard", "Dashboard", "DASH")
    data object WatchFace : Screen("watchface", "Watch Face", "FACE")
    data object Settings : Screen("settings", "Settings", "CFG")
}

/**
 * Main Activity for the UnlockedWear companion hub on Galaxy S25.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnlockedTheme {
                MainAppScaffold()
            }
        }
    }
}

@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Dashboard,
        Screen.WatchFace,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = UberBlack,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF14161F),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(
                        width = 1.dp,
                        color = UberCardBorder,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) UberWhite else Color.Transparent
                                    )
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = screen.badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) UberBlack else UberMuted
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) UberWhite else UberMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = UberWhite,
                            selectedTextColor = UberWhite,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(viewModel = dashboardViewModel)
            }

            composable(Screen.WatchFace.route) {
                val watchFaceViewModel: WatchFaceCustomizationViewModel = hiltViewModel()
                WatchFaceCustomizationScreen(viewModel = watchFaceViewModel)
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
