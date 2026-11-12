package com.unlockedwear.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.data.WeatherCacheEntity
import com.unlockedwear.mobile.ui.components.UberBlack
import com.unlockedwear.mobile.ui.components.UberCard
import com.unlockedwear.mobile.ui.components.UberCardBorder
import com.unlockedwear.mobile.ui.components.UberChip
import com.unlockedwear.mobile.ui.components.UberGreen
import com.unlockedwear.mobile.ui.components.UberMuted
import com.unlockedwear.mobile.ui.components.UberPrimaryButton
import com.unlockedwear.mobile.ui.components.UberSecondaryButton
import com.unlockedwear.mobile.ui.components.UberSectionHeader
import com.unlockedwear.mobile.ui.components.UberStatusPill
import com.unlockedwear.mobile.ui.components.UberWhite
import com.unlockedwear.mobile.ui.viewmodels.DashboardViewModel
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.uvRiskLevel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val biometrics by viewModel.latestBiometrics.collectAsState()
    val weatherCache by viewModel.weatherCache.collectAsState()
    val connectionState by viewModel.watchConnectionState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isMeasuringTemp by viewModel.isMeasuringTemp.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UberBlack)
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UnlockedWear",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = UberWhite
                )
                Text(
                    text = "Galaxy Watch 8 Hub",
                    fontSize = 13.sp,
                    color = UberMuted
                )
            }

            UberStatusPill(
                text = if (connectionState.isConnected) (connectionState.connectedNodes.firstOrNull() ?: "Watch Connected") else "Disconnected",
                dotColor = if (connectionState.isConnected) UberGreen else Color(0xFFF59E0B)
            )
        }

        // Status Toast
        statusMessage?.let { msg ->
            UberCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = UberWhite,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // 1. On-Demand Thermal Reader Card (Uber Hero Style)
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                UberSectionHeader(title = "On-Demand Temperature")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Instant thermal sensor sweep on Galaxy Watch 8",
                    fontSize = 13.sp,
                    color = UberMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f°C", biometrics.skinTempCelsius),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = UberWhite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WRIST TEMP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = UberMuted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = String.format(Locale.US, "Ambient: %.1f°C • Z: %+.2f", biometrics.ambientTempCelsius, biometrics.thermalZScore),
                            fontSize = 12.sp,
                            color = UberMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                UberPrimaryButton(
                    text = if (isMeasuringTemp) "Measuring Temp..." else "Read Temp Now",
                    onClick = { viewModel.readTemperatureOnDemand() },
                    enabled = !isMeasuringTemp,
                    loading = isMeasuringTemp
                )
            }
        }

        // 2. Biometric Telemetry (2x2 Grid)
        UberSectionHeader(title = "Biometric Telemetry")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "HEART RATE",
                value = String.format(Locale.US, "%.0f", biometrics.heartRateBpm),
                unit = "BPM",
                statusText = "Nominal",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "HRV (RMSSD)",
                value = String.format(Locale.US, "%.1f", biometrics.hrvRmssdMs),
                unit = "MS",
                statusText = "High Tone",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "SKIN TEMP",
                value = String.format(Locale.US, "%.1f°", biometrics.skinTempCelsius),
                unit = String.format(Locale.US, "Z: %+.2f", biometrics.thermalZScore),
                statusText = "Stable",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "SPO2",
                value = "${biometrics.spo2Percent}%",
                unit = "OPTIMAL",
                statusText = "Optimal Sat",
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Environmental Weather Card
        weatherCache?.let { weather ->
            WeatherCard(weather = weather)
        }

        // 4. Watch Bezel Ring Mode Selector
        UberSectionHeader(title = "Watch Ring Mode")

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RingMode.entries.forEach { mode ->
                UberChip(
                    selected = mode == RingMode.HEALTH,
                    label = mode.label,
                    onClick = { viewModel.switchRingMode(mode) }
                )
            }
        }

        // 5. Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UberSecondaryButton(
                text = "Sync Watch Face",
                onClick = { viewModel.triggerWatchSync() },
                modifier = Modifier.weight(1f)
            )

            UberSecondaryButton(
                text = "Refresh Weather",
                onClick = { viewModel.refreshWeather() },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String,
    statusText: String,
    modifier: Modifier = Modifier
) {
    UberCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = UberMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = UberWhite
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = UberMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = UberGreen
            )
        }
    }
}

@Composable
fun WeatherCard(
    weather: WeatherCacheEntity
) {
    val risk = uvRiskLevel(weather.uvIndex)

    UberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            UberSectionHeader(
                title = "Solar UV & Weather",
                actionText = "${weather.temp.toInt()}°C • ${weather.condition}"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current UV: ${weather.uvIndex}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = UberWhite
                    )
                    Text(
                        text = "Risk Level: ${risk.label}",
                        fontSize = 13.sp,
                        color = UberMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Max Today: ${weather.forecastMaxUvi}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UberWhite
                    )
                    Text(
                        text = "Peak: ${weather.peakUvWindow}",
                        fontSize = 12.sp,
                        color = UberMuted
                    )
                }
            }
        }
    }
}
