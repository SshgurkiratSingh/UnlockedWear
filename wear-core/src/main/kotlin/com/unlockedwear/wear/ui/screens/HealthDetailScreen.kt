package com.unlockedwear.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.unlockedwear.wear.data.BiometricReadingEntity
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.sensors.HeartRateManager
import com.unlockedwear.wear.sensors.HeartRateReading
import com.unlockedwear.wear.sensors.SamsungSdkInitializer
import com.unlockedwear.wear.sensors.SpO2Manager
import com.unlockedwear.wear.sensors.SpO2Reading
import com.unlockedwear.wear.ui.theme.BioGreen
import com.unlockedwear.wear.ui.theme.CyberCyan
import com.unlockedwear.wear.ui.theme.DangerRed
import com.unlockedwear.wear.ui.theme.NeonOrange
import com.unlockedwear.wear.ui.theme.SpaceBlack
import com.unlockedwear.wear.ui.theme.SpaceCardSurface
import com.unlockedwear.wear.ui.theme.TextMuted
import com.unlockedwear.wear.ui.theme.TextPrimary
import com.unlockedwear.wear.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Screen displaying continuous cardio load (HR, HRV RMSSD) and on-demand blood oxygen (SpO2).
 */
@Composable
fun HealthDetailScreen(
    heartRateFlow: Flow<HeartRateReading>,
    spO2Manager: SpO2Manager,
    sdkInitializer: SamsungSdkInitializer,
    database: WearDatabase,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val currentHr by heartRateFlow.collectAsState(
        initial = HeartRateReading(bpm = 74, ibiListMs = emptyList(), rmssdMs = 45.0)
    )

    val sdkState by sdkInitializer.connectionState.collectAsState()

    var isMeasuringSpO2 by remember { mutableStateOf(false) }
    var currentSpO2Reading by remember { mutableStateOf<SpO2Reading?>(null) }
    var spO2StatusMessage by remember { mutableStateOf<String?>(null) }

    fun triggerSpO2Measurement() {
        if (isMeasuringSpO2) return
        isMeasuringSpO2 = true
        spO2StatusMessage = "Position watch firmly..."

        coroutineScope.launch {
            try {
                spO2Manager.measureSpO2()
                    .take(20)
                    .collect { reading ->
                        currentSpO2Reading = reading
                        spO2StatusMessage = when (reading.status) {
                            SpO2Reading.STATUS_MEASURING -> "Measuring (${reading.spo2Percent}%)..."
                            SpO2Reading.STATUS_LOW_SIGNAL -> "Low signal • Tighten strap"
                            SpO2Reading.STATUS_DEVICE_MOVING -> "Keep wrist still"
                            SpO2Reading.STATUS_SUCCESS -> "Complete: ${reading.spo2Percent}%"
                            else -> "Measuring..."
                        }

                        if (reading.isSuccessful && reading.spo2Percent > 0) {
                            val entity = BiometricReadingEntity(
                                timestamp = reading.timestamp,
                                heartRateBpm = reading.heartRate.takeIf { it > 0 } ?: currentHr.bpm,
                                rmssdMs = currentHr.rmssdMs,
                                spo2Percent = reading.spo2Percent
                            )
                            database.biometricDao().insert(entity)
                        }
                    }
            } catch (e: Exception) {
                spO2StatusMessage = "SpO2 check interrupted"
            } finally {
                isMeasuringSpO2 = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CARDIO BIOMETRICS",
                        style = MaterialTheme.typography.caption2,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Samsung Health Sensor SDK",
                        style = MaterialTheme.typography.caption2,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Primary Heart Rate Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpaceCardSurface)
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${currentHr.bpm}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 42.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BPM",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = DangerRed
                            )
                        }
                        Text(
                            text = "CONTINUOUS HEART RATE",
                            style = MaterialTheme.typography.caption1,
                            color = TextSecondary
                        )
                    }
                }
            }

            // HRV & SpO2 Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // HRV Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceCardSurface)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "HRV (RMSSD)",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format("%.1f", currentHr.rmssdMs)} ms",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = if (currentHr.rmssdMs > 40.0) "Balanced" else "Elevated Load",
                                fontSize = 8.sp,
                                color = if (currentHr.rmssdMs > 40.0) BioGreen else NeonOrange
                            )
                        }
                    }

                    // SpO2 Card
                    val spo2Val = currentSpO2Reading?.spo2Percent ?: 98
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceCardSurface)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "BLOOD OXYGEN",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$spo2Val %",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BioGreen
                            )
                            Text(
                                text = if (spo2Val >= 95) "Optimal" else "Low Saturation",
                                fontSize = 8.sp,
                                color = if (spo2Val >= 95) BioGreen else DangerRed
                            )
                        }
                    }
                }
            }

            // SpO2 Spot Measure Action
            item {
                Button(
                    onClick = { triggerSpO2Measurement() },
                    enabled = !isMeasuringSpO2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .padding(horizontal = 6.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.primaryButtonColors(
                        backgroundColor = BioGreen,
                        contentColor = SpaceBlack
                    )
                ) {
                    if (isMeasuringSpO2) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = SpaceBlack
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = spO2StatusMessage ?: "Measuring...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpaceBlack
                            )
                        }
                    } else {
                        Text(
                            text = "SPOT MEASURE SpO2",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpaceBlack
                        )
                    }
                }
            }

            // Samsung SDK Diagnostics Badge
            item {
                val isConnected = (sdkState is SamsungSdkInitializer.ConnectionState.Connected)
                val statusText = if (isConnected) "Samsung SDK Active" else "Samsung SDK Standby"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) BioGreen else NeonOrange)
                    )
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
