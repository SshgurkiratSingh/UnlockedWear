package com.unlockedwear.mobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.ui.theme.CardSurface
import com.unlockedwear.mobile.ui.theme.CyanAccent
import com.unlockedwear.mobile.ui.theme.DeepIndigoSurface
import com.unlockedwear.mobile.ui.theme.SpaceBlack
import com.unlockedwear.mobile.ui.theme.TextPrimary
import com.unlockedwear.mobile.ui.theme.TextSecondary
import com.unlockedwear.mobile.ui.viewmodels.WatchFaceCustomizationViewModel
import com.unlockedwear.protocol.RingMode
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
//  Static option lists
// ─────────────────────────────────────────────────────────────────────────────

val AccentColorOptions = listOf(
    "#3B82F6" to "Blue",
    "#00D4FF" to "Cyan",
    "#10B981" to "Emerald",
    "#F59E0B" to "Amber",
    "#8B5CF6" to "Purple",
    "#EF4444" to "Crimson",
    "#FF6B35" to "Orange",
    "#F5F5F5" to "Mono"
)

val ComplicationOptions = listOf(
    "Heart Rate (BPM)",
    "UV Index",
    "Autonomic Tone (HRV)",
    "Skin Temp (ΔT)",
    "Focus Timer",
    "Battery Level",
    "AI Headline",
    "SpO2 (%)",
    "Recovery Score",
    "Steps / Goal"
)

val DialStyles = listOf(
    "Digital Modern",
    "Minimal Hybrid",
    "Telemetry Gauge"
)

// Feature flag metadata for the toggle list
data class FeatureToggle(
    val label: String,
    val description: String,
    val tag: String
)

val FeatureToggles = listOf(
    FeatureToggle("Bio-Reactive Arc",   "Ring arc shifts red/amber/green based on live HR zone",        "bioReactiveArc"),
    FeatureToggle("HRV Autonomic Band", "Colour strip below clock shows parasympathetic nervous state", "hrvBand"),
    FeatureToggle("Thermal ΔT Strip",   "Live skin-temperature deviation pill on the dial",             "thermalStrip"),
    FeatureToggle("AI Co-Pilot Ticker", "Scrolling DeepSeek headline above the clock",                  "coPilotTicker"),
    FeatureToggle("Circadian Wheel",    "Outer segmented ring shows current body-clock phase",           "circadianSector"),
    FeatureToggle("UV Zone Arc",        "Secondary arc shows WHO UV risk level in real time",            "uvZoneArc"),
    FeatureToggle("Recovery Score",     "HRV overnight readiness badge (0–100) on the dial",            "recoveryScore"),
    FeatureToggle("Focus Countdown",    "Pomodoro timer strip replaces the seconds sub-dial",           "focusCountdown")
)

// ─────────────────────────────────────────────────────────────────────────────
//  Main screen composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WatchFaceCustomizationScreen(
    viewModel: WatchFaceCustomizationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var isTopExpanded by remember { mutableStateOf(false) }
    var isBottomExpanded by remember { mutableStateOf(false) }

    val activeAccentColor by animateColorAsState(
        targetValue = try { Color(android.graphics.Color.parseColor(uiState.accentColorHex)) }
                      catch (_: Exception) { CyanAccent },
        animationSpec = tween(300),
        label = "accentAnim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header ───────────────────────────────────────────────────────────
        Column {
            Text(
                text = "WATCH FACE STUDIO",
                fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, color = activeAccentColor, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(2.dp))
            Text("Galaxy Watch 8 — Full Customization", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // ── Status banner ─────────────────────────────────────────────────────
        uiState.statusMessage?.let { msg ->
            val bg = when (uiState.syncSuccess) {
                true  -> Color(0xFF0D2B1A)
                false -> Color(0xFF2B0D0D)
                null  -> DeepIndigoSurface
            }
            val fg = when (uiState.syncSuccess) {
                true  -> Color(0xFF4ADE80)
                false -> Color(0xFFF87171)
                null  -> activeAccentColor
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable { viewModel.clearStatusMessage() }
            ) {
                Text(msg, color = fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp))
            }
        }

        // ── 1. LIVE DIAL PREVIEW ─────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("LIVE DIAL PREVIEW", fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    color = TextSecondary, letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(220.dp).clip(CircleShape)
                        .background(Color(0xFF050508))
                        .border(3.dp, activeAccentColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2, size.height / 2)
                        val outerR = size.width / 2 - 10.dp.toPx()

                        // Track
                        drawArc(color = activeAccentColor.copy(alpha = 0.12f),
                            startAngle = 135f, sweepAngle = 270f, useCenter = false,
                            topLeft = Offset(c.x - outerR, c.y - outerR), size = Size(outerR * 2, outerR * 2),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))

                        // Bio-reactive arc — progress by ring mode default
                        val progress = when (uiState.ringMode) {
                            RingMode.HEALTH -> 0.72f; RingMode.UV -> 0.42f
                            RingMode.FOCUS  -> 0.50f; RingMode.POWER -> 0.88f
                        }
                        val arcColor = if (uiState.bioReactiveArc) {
                            // Shift red→amber→green for demonstration
                            when {
                                progress < 0.33f -> Color(0xFFEF4444)
                                progress < 0.66f -> Color(0xFFF59E0B)
                                else             -> Color(0xFF10B981)
                            }
                        } else activeAccentColor

                        drawArc(
                            brush = Brush.sweepGradient(listOf(arcColor.copy(0.4f), arcColor), center = c),
                            startAngle = 135f, sweepAngle = 270f * progress, useCenter = false,
                            topLeft = Offset(c.x - outerR, c.y - outerR), size = Size(outerR * 2, outerR * 2),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // UV zone arc (secondary inner ring)
                        if (uiState.uvZoneArc) {
                            val innerR = outerR - 14.dp.toPx()
                            drawArc(color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                startAngle = 135f, sweepAngle = 270f * 0.6f, useCenter = false,
                                topLeft = Offset(c.x - innerR, c.y - innerR), size = Size(innerR * 2, innerR * 2),
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                        }

                        // Circadian sector segments
                        if (uiState.circadianSector) {
                            val segR = outerR + 6.dp.toPx()
                            val segColors = listOf(
                                Color(0xFF1E3A5F), Color(0xFF1E3A5F), Color(0xFF2D4A6F),
                                Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFFBBF24),
                                Color(0xFFEF4444), Color(0xFFEF4444), Color(0xFFF97316),
                                Color(0xFF10B981), Color(0xFF059669), Color(0xFF1E3A5F)
                            )
                            segColors.forEachIndexed { i, col ->
                                val start = 135f + i * 22.5f
                                drawArc(color = col.copy(alpha = 0.5f),
                                    startAngle = start, sweepAngle = 21f, useCenter = false,
                                    topLeft = Offset(c.x - segR, c.y - segR), size = Size(segR * 2, segR * 2),
                                    style = Stroke(width = 3.5.dp.toPx()))
                            }
                        }

                        // HRV band strip (thin bar below clock text position)
                        if (uiState.hrvBand) {
                            val bandY = c.y + 22.dp.toPx()
                            val bandW = 68.dp.toPx()
                            val hrvColor = Color(0xFF3B82F6)
                            drawLine(color = Color.White.copy(0.08f),
                                start = Offset(c.x - bandW / 2, bandY), end = Offset(c.x + bandW / 2, bandY),
                                strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                            drawLine(color = hrvColor,
                                start = Offset(c.x - bandW / 2, bandY), end = Offset(c.x + bandW / 2 * 0.7f, bandY),
                                strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                        }

                        // Thermal strip
                        if (uiState.thermalStrip) {
                            val stripY = c.y + 32.dp.toPx()
                            drawRoundRect(
                                color = activeAccentColor.copy(alpha = 0.15f),
                                topLeft = Offset(c.x - 32.dp.toPx(), stripY - 6.dp.toPx()),
                                size = Size(64.dp.toPx(), 12.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                        }

                        // Recovery score dot (top right of clock)
                        if (uiState.recoveryScore) {
                            drawCircle(color = Color(0xFF10B981), radius = 5.dp.toPx(),
                                center = Offset(c.x + 42.dp.toPx(), c.y - 30.dp.toPx()))
                        }

                        // Focus countdown bar (at seconds position)
                        if (uiState.focusCountdown) {
                            drawRoundRect(
                                color = activeAccentColor.copy(0.2f),
                                topLeft = Offset(c.x - 50.dp.toPx(), c.y + 48.dp.toPx()),
                                size = Size(100.dp.toPx(), 10.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                            )
                            drawRoundRect(
                                color = activeAccentColor.copy(0.8f),
                                topLeft = Offset(c.x - 50.dp.toPx(), c.y + 48.dp.toPx()),
                                size = Size(60.dp.toPx(), 10.dp.toPx()),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                            )
                        }
                    }

                    // Overlay text elements
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Co-Pilot ticker
                        if (uiState.coPilotTicker) {
                            Text("▸ UV shield advised",
                                fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                                color = activeAccentColor.copy(0.8f))
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(uiState.topComplication,
                            fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                            color = activeAccentColor, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        when (uiState.dialStyle) {
                            "Minimal Hybrid" -> {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("10", fontSize = 38.sp, fontWeight = FontWeight.Light, color = TextPrimary)
                                    Text("08", fontSize = 24.sp, fontWeight = FontWeight.Light, color = activeAccentColor,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                                }
                            }
                            "Telemetry Gauge" -> {
                                Text("10:08", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                                Text(uiState.ringMode.label.uppercase(), fontSize = 7.sp,
                                    color = activeAccentColor, fontFamily = FontFamily.Monospace)
                            }
                            else -> {
                                Text("10:08", fontSize = 34.sp, fontWeight = FontWeight.Bold,
                                    color = TextPrimary, fontFamily = FontFamily.Monospace)
                            }
                        }
                        // HRV band label
                        if (uiState.hrvBand) {
                            Text("HRV: BALANCED", fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                                color = Color(0xFF3B82F6).copy(0.9f))
                        }
                        // Thermal strip label
                        if (uiState.thermalStrip) {
                            Text("ΔT +0.3°C", fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                                color = activeAccentColor.copy(0.7f))
                        }
                        // Recovery badge
                        if (uiState.recoveryScore) {
                            Spacer(Modifier.height(2.dp))
                            Text("⬤ 82", fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                                color = Color(0xFF4ADE80))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(uiState.bottomComplication,
                            fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                            color = activeAccentColor, fontWeight = FontWeight.SemiBold)
                        // Focus countdown strip label
                        if (uiState.focusCountdown) {
                            Spacer(Modifier.height(2.dp))
                            Text("⏱ 23:40 DEEP WORK", fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                                color = activeAccentColor.copy(0.7f))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                // Active feature chips below preview
                val activeFeatures = buildList {
                    if (uiState.bioReactiveArc)   add("BIO ARC")
                    if (uiState.hrvBand)           add("HRV BAND")
                    if (uiState.thermalStrip)      add("ΔT STRIP")
                    if (uiState.coPilotTicker)     add("AI TICKER")
                    if (uiState.circadianSector)   add("CIRCADIAN")
                    if (uiState.uvZoneArc)         add("UV ARC")
                    if (uiState.recoveryScore)     add("RECOVERY")
                    if (uiState.focusCountdown)    add("FOCUS")
                }
                if (activeFeatures.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        activeFeatures.forEach { tag ->
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeAccentColor.copy(0.15f))
                                .border(1.dp, activeAccentColor.copy(0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(tag, fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                                    color = activeAccentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ── 2. BEZEL RING MODE ───────────────────────────────────────────────
        SectionCard(title = "BEZEL RING MODE", accent = activeAccentColor) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                RingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.ringMode == mode,
                        onClick = { viewModel.setRingMode(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeAccentColor,
                            selectedLabelColor = SpaceBlack,
                            containerColor = DeepIndigoSurface, labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        // ── 3. ACCENT COLOUR ─────────────────────────────────────────────────
        SectionCard(title = "ACCENT COLOUR", accent = activeAccentColor) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                AccentColorOptions.forEach { (hex, name) ->
                    val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { CyanAccent }
                    val selected = uiState.accentColorHex.equals(hex, ignoreCase = true)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(col)
                                .border(if (selected) 3.dp else 0.dp,
                                    if (selected) TextPrimary else Color.Transparent, CircleShape)
                                .clickable { viewModel.setAccentColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(TextPrimary))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(name, fontSize = 7.sp, color = TextSecondary)
                    }
                }
            }
        }

        // ── 4. COMPLICATION SLOTS ────────────────────────────────────────────
        SectionCard(title = "COMPLICATION SLOTS", accent = activeAccentColor) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Top Slot", fontSize = 11.sp, color = TextSecondary)
                ExposedDropdownMenuBox(expanded = isTopExpanded, onExpandedChange = { isTopExpanded = it }) {
                    OutlinedTextField(
                        value = uiState.topComplication, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isTopExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DeepIndigoSurface, unfocusedContainerColor = DeepIndigoSurface,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = activeAccentColor, unfocusedBorderColor = DeepIndigoSurface
                        ),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = isTopExpanded, onDismissRequest = { isTopExpanded = false }) {
                        ComplicationOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                viewModel.setTopComplication(opt); isTopExpanded = false
                            })
                        }
                    }
                }

                Text("Bottom Slot", fontSize = 11.sp, color = TextSecondary)
                ExposedDropdownMenuBox(expanded = isBottomExpanded, onExpandedChange = { isBottomExpanded = it }) {
                    OutlinedTextField(
                        value = uiState.bottomComplication, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(isBottomExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DeepIndigoSurface, unfocusedContainerColor = DeepIndigoSurface,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = activeAccentColor, unfocusedBorderColor = DeepIndigoSurface
                        ),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = isBottomExpanded, onDismissRequest = { isBottomExpanded = false }) {
                        ComplicationOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                viewModel.setBottomComplication(opt); isBottomExpanded = false
                            })
                        }
                    }
                }
            }
        }

        // ── 5. DIAL LAYOUT ───────────────────────────────────────────────────
        SectionCard(title = "DIAL LAYOUT", accent = activeAccentColor) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialStyles.forEach { style ->
                    FilterChip(
                        selected = uiState.dialStyle == style,
                        onClick = { viewModel.setDialStyle(style) },
                        label = { Text(style, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeAccentColor, selectedLabelColor = SpaceBlack,
                            containerColor = DeepIndigoSurface, labelColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── 6. EXCLUSIVE BIOMETRIC FEATURES ─────────────────────────────────
        SectionCard(title = "EXCLUSIVE BIOMETRIC OVERLAYS", accent = activeAccentColor) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    "These features are not available on any stock Wear OS or Samsung theme.",
                    fontSize = 10.sp, color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                FeatureToggles.forEachIndexed { i, feat ->
                    val enabled = when (feat.tag) {
                        "bioReactiveArc"  -> uiState.bioReactiveArc
                        "hrvBand"         -> uiState.hrvBand
                        "circadianSector" -> uiState.circadianSector
                        "thermalStrip"    -> uiState.thermalStrip
                        "coPilotTicker"   -> uiState.coPilotTicker
                        "uvZoneArc"       -> uiState.uvZoneArc
                        "recoveryScore"   -> uiState.recoveryScore
                        "focusCountdown"  -> uiState.focusCountdown
                        else              -> false
                    }
                    val onToggle: (Boolean) -> Unit = { v ->
                        when (feat.tag) {
                            "bioReactiveArc"  -> viewModel.setBioReactiveArc(v)
                            "hrvBand"         -> viewModel.setHrvBand(v)
                            "circadianSector" -> viewModel.setCircadianSector(v)
                            "thermalStrip"    -> viewModel.setThermalStrip(v)
                            "coPilotTicker"   -> viewModel.setCoPilotTicker(v)
                            "uvZoneArc"       -> viewModel.setUvZoneArc(v)
                            "recoveryScore"   -> viewModel.setRecoveryScore(v)
                            "focusCountdown"  -> viewModel.setFocusCountdown(v)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (enabled) activeAccentColor.copy(0.08f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(feat.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (enabled) TextPrimary else TextSecondary)
                            Text(feat.description, fontSize = 10.sp, color = TextSecondary,
                                modifier = Modifier.padding(top = 1.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = enabled, onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpaceBlack,
                                checkedTrackColor = activeAccentColor,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DeepIndigoSurface
                            )
                        )
                    }
                    if (i < FeatureToggles.lastIndex) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(0.04f)))
                    }
                }
            }
        }

        // ── 7. APPLY BUTTON ──────────────────────────────────────────────────
        Button(
            onClick = { viewModel.applyCustomizationToWatch() },
            enabled = !uiState.isSyncing,
            colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor,
                disabledContainerColor = activeAccentColor.copy(0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (uiState.isSyncing) {
                CircularProgressIndicator(color = SpaceBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Syncing…", color = SpaceBlack, fontWeight = FontWeight.Bold)
            } else {
                Text("Apply to Galaxy Watch 8", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable section card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(3.dp, 14.dp).background(accent, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
