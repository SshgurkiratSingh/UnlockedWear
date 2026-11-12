package com.unlockedwear.wear.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.unlockedwear.wear.data.ThermalReadingEntity
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.sensors.UnlockedThermalManager
import com.unlockedwear.wear.ui.theme.CyberCyan
import com.unlockedwear.wear.ui.theme.NeonOrange
import com.unlockedwear.wear.ui.theme.SpaceBlack
import com.unlockedwear.wear.ui.theme.SpaceCardSurface
import com.unlockedwear.wear.ui.theme.TextMuted
import com.unlockedwear.wear.ui.theme.TextPrimary
import com.unlockedwear.wear.ui.theme.TextSecondary
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Screen displaying detailed skin and ambient thermal telemetry, Z-scores, and 24h trendline.
 */
@Composable
fun ThermalDetailScreen(
    thermalManager: UnlockedThermalManager,
    database: WearDatabase,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    val latestReading by database.thermalDao().observeLatest().collectAsState(initial = null)
    val recentReadings by database.thermalDao().observeRecent(limit = 60).collectAsState(initial = emptyList())

    var isMeasuringOnDemand by remember { mutableStateOf(false) }
    var onDemandProgressText by remember { mutableStateOf<String?>(null) }

    fun triggerOnDemandMeasurement() {
        if (isMeasuringOnDemand) return
        isMeasuringOnDemand = true
        onDemandProgressText = "Calibrating..."

        coroutineScope.launch {
            try {
                thermalManager.streamSkinTemperature(onDemand = true)
                    .take(5)
                    .collect { frame ->
                        onDemandProgressText = "Reading ${String.format("%.1f", frame.compensatedTempCelsius)}°C"
                        val baseline = thermalManager.getBaselineTemperature()
                        val delta = frame.compensatedTempCelsius - baseline
                        val zScore = thermalManager.getCurrentZScore()

                        val entity = ThermalReadingEntity(
                            timestamp = frame.timestampMs,
                            skinTemp = frame.skinTempCelsius,
                            ambientTemp = frame.ambientTempCelsius,
                            compensatedTemp = frame.compensatedTempCelsius,
                            deltaFromBaseline = delta,
                            zScore = zScore,
                            isOnDemand = true
                        )
                        database.thermalDao().insert(entity)
                    }
            } catch (e: Exception) {
                onDemandProgressText = "Sensor Error"
            } finally {
                isMeasuringOnDemand = false
                onDemandProgressText = null
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
            // Screen Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "THERMAL BIOLOGY",
                        style = MaterialTheme.typography.caption2,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Galaxy Watch 8 IR Core",
                        style = MaterialTheme.typography.caption2,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Hero Metric: Compensated Core-Skin Temp
            item {
                val temp = latestReading?.compensatedTemp ?: 34.2f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format("%.1f", temp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "°C",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = CyberCyan,
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = "Compensated Skin Temp",
                        style = MaterialTheme.typography.caption1,
                        color = TextSecondary
                    )
                }
            }

            // 24-Hour Mini Trend Canvas Chart
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpaceCardSurface)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TREND (LAST 24H)",
                                fontSize = 8.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            val zScore = latestReading?.zScore ?: 0.0f
                            Text(
                                text = "Z-Score: ${if (zScore >= 0f) "+" else ""}${String.format("%.2f", zScore)}σ",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (kotlin.math.abs(zScore) > 2.0f) NeonOrange else CyberCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ThermalTrendChart(
                            readings = recentReadings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            // Metrics 2x2 Grid
            item {
                val rawSkin = latestReading?.skinTemp ?: 33.4f
                val ambient = latestReading?.ambientTemp ?: 22.0f
                val delta = latestReading?.deltaFromBaseline ?: 0.1f

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThermalStatCard(
                            label = "RAW SKIN",
                            value = "${String.format("%.1f", rawSkin)}°C",
                            modifier = Modifier.weight(1f)
                        )
                        ThermalStatCard(
                            label = "AMBIENT",
                            value = "${String.format("%.1f", ambient)}°C",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThermalStatCard(
                            label = "BASELINE Δ",
                            value = "${if (delta >= 0f) "+" else ""}${String.format("%.1f", delta)}°C",
                            isWarning = kotlin.math.abs(delta) > 0.8f,
                            modifier = Modifier.weight(1f)
                        )
                        ThermalStatCard(
                            label = "CIRCADIAN",
                            value = "Optimal",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // On-Demand Spot Measurement Action Button
            item {
                Button(
                    onClick = { triggerOnDemandMeasurement() },
                    enabled = !isMeasuringOnDemand,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.primaryButtonColors(
                        backgroundColor = CyberCyan,
                        contentColor = SpaceBlack
                    )
                ) {
                    if (isMeasuringOnDemand) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = SpaceBlack
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = onDemandProgressText ?: "Measuring...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpaceBlack
                            )
                        }
                    } else {
                        Text(
                            text = "SPOT MEASURE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpaceBlack
                        )
                    }
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ThermalStatCard(
    label: String,
    value: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceCardSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 8.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) NeonOrange else TextPrimary
            )
        }
    }
}

@Composable
private fun ThermalTrendChart(
    readings: List<ThermalReadingEntity>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (readings.size < 2) {
            drawLine(
                color = Color(0x3300D4FF),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        val minTemp = (readings.minOfOrNull { it.compensatedTemp } ?: 32f) - 0.5f
        val maxTemp = (readings.maxOfOrNull { it.compensatedTemp } ?: 38f) + 0.5f
        val range = (maxTemp - minTemp).coerceAtLeast(1.0f)

        val points = readings.mapIndexed { index, item ->
            val x = (index.toFloat() / (readings.size - 1)) * size.width
            val y = size.height - (((item.compensatedTemp - minTemp) / range) * size.height)
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = strokePath,
            color = CyberCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyberCyan.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )
    }
}
