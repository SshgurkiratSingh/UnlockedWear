package com.unlockedwear.mobile.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.ui.components.UberBlack
import com.unlockedwear.mobile.ui.components.UberCard
import com.unlockedwear.mobile.ui.components.UberCardBorder
import com.unlockedwear.mobile.ui.components.UberChip
import com.unlockedwear.mobile.ui.components.UberMuted
import com.unlockedwear.mobile.ui.components.UberPrimaryButton
import com.unlockedwear.mobile.ui.components.UberSectionHeader
import com.unlockedwear.mobile.ui.components.UberWhite
import com.unlockedwear.mobile.ui.viewmodels.WatchFaceCustomizationViewModel
import com.unlockedwear.protocol.RingMode

val AccentColorOptions = listOf(
    "#FFFFFF" to "White",
    "#00B4D8" to "Cyan",
    "#10B981" to "Emerald",
    "#F59E0B" to "Amber",
    "#8B5CF6" to "Purple"
)

val ComplicationOptions = listOf(
    "Heart Rate (BPM)",
    "UV Index",
    "Autonomic Tone (HRV)",
    "Skin Temp (Z-Score)",
    "Focus Timer",
    "Battery Level"
)

val DialStyles = listOf(
    "Digital Modern",
    "Minimal Hybrid",
    "Telemetry Gauge"
)

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

    val activeAccentColor = try {
        Color(android.graphics.Color.parseColor(uiState.accentColorHex))
    } catch (_: Exception) {
        UberWhite
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UberBlack)
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Column {
            Text(
                text = "Watch Face Studio",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = UberWhite
            )
            Text(
                text = "Galaxy Watch 8 Customization",
                fontSize = 13.sp,
                color = UberMuted
            )
        }

        // Status Toast
        uiState.statusMessage?.let { status ->
            UberCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = status,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = UberWhite,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // 1. Dial Preview Card
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UberSectionHeader(title = "Live Dial Preview")

                Spacer(modifier = Modifier.height(18.dp))

                // Watch Dial Preview
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF090A0E))
                        .border(3.dp, activeAccentColor.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 10.dp.toPx()

                        drawCircle(
                            color = activeAccentColor.copy(alpha = 0.2f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.topComplication,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeAccentColor
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "10:08",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = UberWhite
                        )

                        Text(
                            text = uiState.ringMode.label.uppercase(),
                            fontSize = 10.sp,
                            color = UberMuted
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = uiState.bottomComplication,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeAccentColor
                        )
                    }
                }
            }
        }

        // 2. Ring Mode Selector
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                UberSectionHeader(title = "Ring Mode")
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RingMode.entries.forEach { mode ->
                        UberChip(
                            selected = uiState.ringMode == mode,
                            label = mode.label,
                            onClick = { viewModel.setRingMode(mode) }
                        )
                    }
                }
            }
        }

        // 3. Accent Color Theme
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                UberSectionHeader(title = "Accent Color")
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentColorOptions.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = uiState.accentColorHex.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) UberWhite else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setAccentColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(UberBlack)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Complications Configurations
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UberSectionHeader(title = "Complications")

                // Top Slot Dropdown
                Text("Top Slot", fontSize = 13.sp, color = UberMuted)
                ExposedDropdownMenuBox(
                    expanded = isTopExpanded,
                    onExpandedChange = { isTopExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.topComplication,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTopExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF13151D),
                            unfocusedContainerColor = Color(0xFF13151D),
                            focusedTextColor = UberWhite,
                            unfocusedTextColor = UberWhite,
                            focusedBorderColor = UberCardBorder,
                            unfocusedBorderColor = UberCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isTopExpanded,
                        onDismissRequest = { isTopExpanded = false }
                    ) {
                        ComplicationOptions.forEach { comp ->
                            DropdownMenuItem(
                                text = { Text(comp) },
                                onClick = {
                                    viewModel.setTopComplication(comp)
                                    isTopExpanded = false
                                }
                            )
                        }
                    }
                }

                // Bottom Slot Dropdown
                Text("Bottom Slot", fontSize = 13.sp, color = UberMuted)
                ExposedDropdownMenuBox(
                    expanded = isBottomExpanded,
                    onExpandedChange = { isBottomExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.bottomComplication,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBottomExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF13151D),
                            unfocusedContainerColor = Color(0xFF13151D),
                            focusedTextColor = UberWhite,
                            unfocusedTextColor = UberWhite,
                            focusedBorderColor = UberCardBorder,
                            unfocusedBorderColor = UberCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isBottomExpanded,
                        onDismissRequest = { isBottomExpanded = false }
                    ) {
                        ComplicationOptions.forEach { comp ->
                            DropdownMenuItem(
                                text = { Text(comp) },
                                onClick = {
                                    viewModel.setBottomComplication(comp)
                                    isBottomExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. Apply Button
        UberPrimaryButton(
            text = if (uiState.isSyncing) "Applying..." else "Apply to Galaxy Watch 8",
            onClick = { viewModel.applyCustomizationToWatch() },
            enabled = !uiState.isSyncing,
            loading = uiState.isSyncing
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
