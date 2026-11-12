package com.unlockedwear.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.unlockedwear.mobile.ui.components.GlassCard
import com.unlockedwear.mobile.ui.components.GlassFilterChip
import com.unlockedwear.mobile.ui.components.SectionHeader
import com.unlockedwear.mobile.ui.theme.CardSurface
import com.unlockedwear.mobile.ui.theme.CyanAccent
import com.unlockedwear.mobile.ui.theme.DeepIndigoSurface
import com.unlockedwear.mobile.ui.theme.GreenStamina
import com.unlockedwear.mobile.ui.theme.OrangeWarning
import com.unlockedwear.mobile.ui.theme.PurpleCosmic
import com.unlockedwear.mobile.ui.theme.SpaceBlack
import com.unlockedwear.mobile.ui.theme.TextPrimary
import com.unlockedwear.mobile.ui.theme.TextSecondary
import com.unlockedwear.mobile.ui.viewmodels.HealthUiState
import com.unlockedwear.mobile.ui.viewmodels.HealthViewModel
import com.unlockedwear.mobile.ui.viewmodels.MetricPoint
import com.unlockedwear.mobile.ui.viewmodels.TimeRange
import java.util.Locale

@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp, 16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(CyanAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PHYSIOLOGICAL ANALYTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Biometric Telemetry History",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Time Range Selector Pills (6h, 24h, 7d)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.entries.forEach { range ->
                GlassFilterChip(
                    selected = uiState.timeRange == range,
                    onClick = { viewModel.setTimeRange(range) },
                    label = range.label,
                    accentColor = CyanAccent
                )
            }
        }

        // Summary Stat Pills
        SummaryStatsRow(uiState = uiState)

        // 1. Heart Rate Timeline
        TelemetryChartCard(
            title = "Heart Rate Timeline",
            subtitle = String.format(Locale.US, "Min: %.0f | Max: %.0f | Avg: %.0f BPM", uiState.minHeartRate, uiState.maxHeartRate, uiState.avgHeartRate),
            points = uiState.heartRatePoints,
            lineColor = CyanAccent,
            defaultMin = 50f,
            defaultMax = 120f
        )

        // 2. Autonomic Tone (HRV RMSSD) Timeline
        TelemetryChartCard(
            title = "Autonomic Tone (HRV RMSSD)",
            subtitle = String.format(Locale.US, "Average Vagal Tone: %.1f ms", uiState.avgHrv),
            points = uiState.hrvPoints,
            lineColor = GreenStamina,
            defaultMin = 20f,
            defaultMax = 90f
        )

        // 3. Wrist Skin Temperature & Z-Score Anomalies
        ThermalChartCard(uiState = uiState)

        // 4. Blood Oxygen Saturation (SpO2)
        TelemetryChartCard(
            title = "Blood Oxygen (SpO2)",
            subtitle = "Saturated Baseline Target: 95% - 100%",
            points = uiState.spo2Points,
            lineColor = PurpleCosmic,
            defaultMin = 90f,
            defaultMax = 100f,
            referenceLine = 95f
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SummaryStatsRow(uiState: HealthUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPill(
            label = "AVG HR",
            value = String.format(Locale.US, "%.0f", uiState.avgHeartRate),
            unit = "BPM",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "HRV",
            value = String.format(Locale.US, "%.0f", uiState.avgHrv),
            unit = "MS",
            color = GreenStamina,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "SKIN TEMP",
            value = String.format(Locale.US, "%.1f°", uiState.avgSkinTemp),
            unit = "C",
            color = OrangeWarning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        borderColor = color.copy(alpha = 0.3f),
        gradientColors = listOf(CardSurface.copy(alpha = 0.9f), DeepIndigoSurface.copy(alpha = 0.8f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
fun TelemetryChartCard(
    title: String,
    subtitle: String,
    points: List<MetricPoint>,
    lineColor: Color,
    defaultMin: Float,
    defaultMax: Float,
    referenceLine: Float? = null
) {
    GlassCard(
        borderColor = lineColor.copy(alpha = 0.3f),
        gradientColors = listOf(DeepIndigoSurface, CardSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = title,
                subtitle = subtitle,
                badgeColor = lineColor
            )
            Spacer(modifier = Modifier.height(14.dp))

            MetricLineChart(
                points = points,
                lineColor = lineColor,
                defaultMin = defaultMin,
                defaultMax = defaultMax,
                referenceLine = referenceLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        }
    }
}

@Composable
fun ThermalChartCard(
    uiState: HealthUiState
) {
    GlassCard(
        borderColor = OrangeWarning.copy(alpha = 0.4f),
        gradientColors = listOf(DeepIndigoSurface, CardSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Thermal Baseline & Z-Score Anomalies",
                subtitle = String.format(Locale.US, "Baseline: %.1f°C (Deviations |Z| >= 1.5σ marked)", uiState.thermalBaselineMean),
                badgeText = "${uiState.thermalZScoreAnomalies.size} ANOMALIES",
                badgeColor = OrangeWarning
            )
            Spacer(modifier = Modifier.height(14.dp))

            MetricLineChart(
                points = uiState.skinTempPoints,
                lineColor = OrangeWarning,
                defaultMin = 30f,
                defaultMax = 37f,
                referenceLine = uiState.thermalBaselineMean,
                anomalyPoints = uiState.thermalZScoreAnomalies,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
        }
    }
}

@Composable
fun MetricLineChart(
    points: List<MetricPoint>,
    lineColor: Color,
    defaultMin: Float,
    defaultMax: Float,
    referenceLine: Float? = null,
    anomalyPoints: List<MetricPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (points.isEmpty()) {
            drawLine(
                color = Color.DarkGray.copy(alpha = 0.3f),
                start = Offset(0f, height / 2f),
                end = Offset(width, height / 2f),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val values = points.map { it.value }
        val minVal = (values.minOrNull() ?: defaultMin).coerceAtMost(defaultMin)
        val maxVal = (values.maxOrNull() ?: defaultMax).coerceAtLeast(defaultMax)
        val rangeVal = (maxVal - minVal).coerceAtLeast(1.0f)

        // Reference line
        referenceLine?.let { ref ->
            val refY = height - (((ref - minVal) / rangeVal) * height)
            drawLine(
                color = lineColor.copy(alpha = 0.35f),
                start = Offset(0f, refY),
                end = Offset(width, refY),
                strokeWidth = 1.dp.toPx()
            )
        }

        val strokePath = Path()
        val fillPath = Path()
        val stepX = if (points.size > 1) width / (points.size - 1) else width

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = height - (((point.value - minVal) / rangeVal) * height)
            if (index == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw gradient fill area
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw line series stroke
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Anomaly dots
        anomalyPoints.forEach { anomaly ->
            val pointIndex = points.indexOfFirst { it.timestampMs == anomaly.timestampMs }
            if (pointIndex >= 0) {
                val x = pointIndex * stepX
                val y = height - (((anomaly.value - minVal) / rangeVal) * height)
                drawCircle(
                    color = Color.Red.copy(alpha = 0.4f),
                    radius = 8.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.Red,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
