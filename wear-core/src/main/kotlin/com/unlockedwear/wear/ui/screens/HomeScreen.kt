package com.unlockedwear.wear.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.RingState
import com.unlockedwear.wear.data.RingStateDataStore
import com.unlockedwear.wear.data.WatchFaceConfig
import com.unlockedwear.wear.data.WatchFaceConfigDataStore
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.sensors.HeartRateReading
import com.unlockedwear.wear.ui.theme.BioGreen
import com.unlockedwear.wear.ui.theme.CyberCyan
import com.unlockedwear.wear.ui.theme.DangerRed
import com.unlockedwear.wear.ui.theme.NeonOrange
import com.unlockedwear.wear.ui.theme.SolarYellow
import com.unlockedwear.wear.ui.theme.SpaceBlack
import com.unlockedwear.wear.ui.theme.SpaceCardSurface
import com.unlockedwear.wear.ui.theme.TextMuted
import com.unlockedwear.wear.ui.theme.TextPrimary
import com.unlockedwear.wear.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Primary dashboard Composable for UnlockedWear on Galaxy Watch.
 */
@Composable
fun HomeScreen(
    ringStateDataStore: RingStateDataStore,
    database: WearDatabase,
    heartRateFlow: Flow<HeartRateReading>,
    onNavigateToCoPilot: () -> Unit,
    onNavigateToThermalDetail: () -> Unit,
    onNavigateToHealthDetail: () -> Unit,
    onNavigateToRingSelector: () -> Unit,
    watchFaceConfigDataStore: WatchFaceConfigDataStore? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberScalingLazyListState()

    val ringState by ringStateDataStore.ringStateFlow.collectAsState(
        initial = RingState(RingMode.UV, 3.5f, 0f, 12f, "UV Index", "3.5 UVI")
    )

    // Watch face customization config (accent colour, complication slot labels, dial style)
    val watchFaceConfig by (watchFaceConfigDataStore?.configFlow
        ?: flowOf(WatchFaceConfig()))
        .collectAsState(initial = WatchFaceConfig())

    val accentColor = try {
        Color(android.graphics.Color.parseColor(watchFaceConfig.accentColorHex))
    } catch (_: Exception) {
        CyberCyan
    }

    val latestThermal by database.thermalDao().observeLatest().collectAsState(initial = null)
    val latestBriefing by database.briefingDao().observeLatestBriefing().collectAsState(initial = null)
    val currentHeartRate by heartRateFlow.collectAsState(
        initial = HeartRateReading(bpm = 74, ibiListMs = emptyList(), rmssdMs = 42.0)
    )

    // Clock update ticker
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (true) {
            val now = System.currentTimeMillis()
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
            currentDateString = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(now)).uppercase()
            kotlinx.coroutines.delay(1000)
        }
    }

    // Rotary scroll accumulator for crown mode cycling
    var rotaryAccumulator by remember { mutableStateOf(0f) }
    val rotaryThreshold = 35f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                rotaryAccumulator += event.verticalScrollPixels
                if (rotaryAccumulator > rotaryThreshold) {
                    rotaryAccumulator = 0f
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    coroutineScope.launch {
                        ringStateDataStore.cycleNextMode()
                    }
                    true
                } else if (rotaryAccumulator < -rotaryThreshold) {
                    rotaryAccumulator = 0f
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    coroutineScope.launch {
                        ringStateDataStore.cycleNextMode()
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        // Outer Mission Ring Gauge Arc
        MissionRingOuterArc(
            progress = ringState.progressFraction,
            mode = ringState.mode,
            accentColor = accentColor,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onNavigateToRingSelector() }
        )

        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time & Date Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // Top Complication Slot label (driven by watch face config)
                    Text(
                        text = watchFaceConfig.topComplication.uppercase(),
                        style = MaterialTheme.typography.caption2,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDateString.ifEmpty { "WED, NOV 12" },
                        style = MaterialTheme.typography.caption2,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = currentTimeString.ifEmpty { "10:42" },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = TextPrimary
                    )
                    // Bottom Complication Slot label
                    Text(
                        text = watchFaceConfig.bottomComplication.uppercase(),
                        style = MaterialTheme.typography.caption2,
                        color = accentColor.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    if (watchFaceConfig.complicationSlots.size > 2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            watchFaceConfig.complicationSlots.drop(2).take(4).forEach { slot ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SpaceCardSurface)
                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = slot.uppercase(),
                                        style = MaterialTheme.typography.caption2,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mission Ring Quick Chip
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpaceCardSurface)
                        .clickable { onNavigateToRingSelector() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getRingColor(ringState.mode))
                        )
                        Text(
                            text = "${ringState.mode.name}: ${ringState.formattedMetric}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Dual Telemetry Row: Heart Rate & Skin Temp Delta
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Heart Rate Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceCardSurface)
                            .clickable { onNavigateToHealthDetail() }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            PulsingHeartIcon()
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "${currentHeartRate.bpm}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "BPM",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Thermal Delta Pill
                    val delta = latestThermal?.deltaFromBaseline ?: 0.0f
                    val deltaSign = if (delta >= 0f) "+" else ""
                    val deltaFormatted = "$deltaSign${String.format("%.1f", delta)}°C"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceCardSurface)
                            .clickable { onNavigateToThermalDetail() }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = deltaFormatted,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (kotlin.math.abs(delta) > 1.0f) NeonOrange else CyberCyan
                            )
                            Text(
                                text = "Δ TEMP",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Co-Pilot Briefing Card
            item {
                val headline = latestBriefing?.coPilotHeadline
                    ?: "Solar elevation peak at 13:00 • Optimal hydration & UV shield advised"

                Card(
                    onClick = onNavigateToCoPilot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = SpaceCardSurface,
                        endBackgroundColor = SpaceCardSurface
                    ),
                    contentColor = TextPrimary

                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CO-PILOT",
                                style = MaterialTheme.typography.caption2,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "DEEPSEEK",
                                style = MaterialTheme.typography.caption2,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Next Event / Schedule Stub
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF11111B))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEXT EVENT",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Deep Work Block",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "14:00",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                    }
                }
            }

            // Wear OS System Weather Launcher Tile
            item {
                val context = view.context
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F1B2B))
                        .clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.wearable.weather")
                                ?: context.packageManager.getLaunchIntentForPackage("com.samsung.android.weather")
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                android.widget.Toast.makeText(context, "System Weather app launched", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYSTEM WEATHER",
                                fontSize = 9.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Wear OS Weather & UV",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "OPEN ↗",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }
            }

            // Bottom Spacing for Circular Watch Bezel
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PulsingHeartIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Canvas(modifier = Modifier.size((14 * scale).dp)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            val width = size.width
            val height = size.height
            moveTo(width / 2f, height / 5f)
            cubicTo(5f * width / 14f, 0f, 0f, height / 15f, 0f, 2f * height / 5f)
            cubicTo(0f, 2f * height / 3f, width / 2f, 9f * height / 10f, width / 2f, height)
            cubicTo(width / 2f, 9f * height / 10f, width, 2f * height / 3f, width, 2f * height / 5f)
            cubicTo(width, height / 15f, 9f * width / 14f, 0f, width / 2f, height / 5f)
        }
        drawPath(path = path, color = DangerRed)
    }
}

@Composable
private fun MissionRingOuterArc(
    progress: Float,
    mode: RingMode,
    accentColor: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    val ringColor = accentColor.takeIf { accentColor != CyberCyan } ?: getRingColor(mode)

    Canvas(modifier = modifier) {
        val strokeWidth = 5.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        // Background subtle track
        drawArc(
            color = Color(0x33333344),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Active progress arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(ringColor.copy(alpha = 0.5f), ringColor),
                center = center
            ),
            startAngle = 135f,
            sweepAngle = 270f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun getRingColor(mode: RingMode): Color = when (mode) {
    RingMode.UV -> SolarYellow
    RingMode.HEALTH -> DangerRed
    RingMode.FOCUS -> CyberCyan
    RingMode.POWER -> BioGreen
}
