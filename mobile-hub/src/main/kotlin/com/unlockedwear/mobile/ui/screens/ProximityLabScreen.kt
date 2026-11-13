package com.unlockedwear.mobile.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.data.ProximityDevice
import com.unlockedwear.mobile.data.WallCondition
import com.unlockedwear.mobile.ui.components.UberAccentBlue
import com.unlockedwear.mobile.ui.components.UberAmber
import com.unlockedwear.mobile.ui.components.UberBlack
import com.unlockedwear.mobile.ui.components.UberCard
import com.unlockedwear.mobile.ui.components.UberCardBorder
import com.unlockedwear.mobile.ui.components.UberChip
import com.unlockedwear.mobile.ui.components.UberGreen
import com.unlockedwear.mobile.ui.components.UberMuted
import com.unlockedwear.mobile.ui.components.UberPrimaryButton
import com.unlockedwear.mobile.ui.components.UberSectionHeader
import com.unlockedwear.mobile.ui.components.UberWhite
import com.unlockedwear.mobile.ui.viewmodels.ProximityViewModel
import java.util.Locale

@Composable
fun ProximityLabScreen(
    viewModel: ProximityViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var distanceText by remember { mutableStateOf("1.0") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (viewModel.hasRequiredPermissions()) viewModel.scan()
    }

    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }.toTypedArray()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(UberBlack)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("PROXIMITY LAB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = UberAccentBlue, letterSpacing = 2.sp)
                Text("RSSI distance mapping", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = UberWhite)
                Text(
                    "Calibrate your room once, then track authorized BLE and Wi‑Fi devices with a local estimate.",
                    fontSize = 13.sp,
                    color = UberMuted
                )
            }
        }

        item {
            UberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UberSectionHeader(
                        title = "SCAN RADIOS",
                        actionText = if (state.devices.isEmpty()) "NO DEVICES" else "${state.devices.size} FOUND"
                    )
                    Text(
                        "RSSI is an estimate, not a measurement. Keep scanning conditions consistent for useful trends.",
                        fontSize = 12.sp,
                        color = UberMuted
                    )
                    if (!state.hasRequiredPermissions) {
                        UberPrimaryButton(
                            text = "GRANT RADIO ACCESS",
                            onClick = { permissionLauncher.launch(permissions) }
                        )
                    } else {
                        UberPrimaryButton(
                            text = if (state.isScanning) "SCANNING…" else "SCAN BLE + WI-FI",
                            onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.scan() },
                            containerColor = if (state.isScanning) UberAmber else UberWhite
                        )
                    }
                    state.errorMessage?.let {
                        Text(it, fontSize = 12.sp, color = Color(0xFFF87171))
                    }
                }
            }
        }

        item {
            UberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UberSectionHeader(title = "ROOM PROFILE")
                    Text("Choose the wall condition used for the current scan.", fontSize = 12.sp, color = UberMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WallCondition.entries.take(3).forEach { condition ->
                            UberChip(
                                selected = state.selectedCondition == condition,
                                label = condition.label,
                                onClick = { viewModel.setCondition(condition) },
                                activeColor = UberAccentBlue,
                                activeTextColor = UberWhite
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WallCondition.entries.drop(3).forEach { condition ->
                            UberChip(
                                selected = state.selectedCondition == condition,
                                label = condition.label,
                                onClick = { viewModel.setCondition(condition) },
                                activeColor = UberAccentBlue,
                                activeTextColor = UberWhite
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = { distanceText = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                            singleLine = true,
                            label = { Text("Known distance (m)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = UberWhite,
                                unfocusedTextColor = UberWhite,
                                focusedBorderColor = UberAccentBlue,
                                unfocusedBorderColor = UberCardBorder,
                                focusedLabelColor = UberAccentBlue,
                                unfocusedLabelColor = UberMuted
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "ADD SAMPLE",
                            color = UberAccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF202638), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 17.dp)
                                .clickable {
                                    distanceText.toFloatOrNull()?.let(viewModel::addCalibrationSample)
                                }
                        )
                    }
                    val profile = state.calibrationProfiles[state.selectedCondition]
                    Text(
                        text = "Samples: ${profile?.points?.size ?: 0} • Path-loss exponent: ${String.format(Locale.US, "%.2f", profile?.pathLossExponent ?: 2f)}",
                        fontSize = 11.sp,
                        color = UberMuted
                    )
                }
            }
        }

        item {
            UberSectionHeader(title = "DISCOVERED DEVICES")
        }

        if (state.devices.isEmpty()) {
            item {
                UberCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Scan to find nearby Bluetooth beacons, ESP32 advertisements, Wi‑Fi access points, and ESP8266 lab nodes.",
                        fontSize = 13.sp,
                        color = UberMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.devices, key = { it.id }) { device ->
                DeviceCard(device)
            }
        }

        item {
            UberCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UberSectionHeader(title = "AUTHORIZED LAB DEVICES")
                    Text(
                        "ESP32 and ESP8266 nodes are shown as telemetry targets only. This app does not send deauth, injection, credential-capture, or disruption commands.",
                        fontSize = 12.sp,
                        color = UberMuted
                    )
                    Text("Use the device firmware’s own safe simulator endpoint for test telemetry.", fontSize = 12.sp, color = UberGreen)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DeviceCard(device: ProximityDevice) {
    val family = when {
        device.isEsp32 -> "ESP32"
        device.isEsp8266 -> "ESP8266"
        else -> device.radio.name
    }
    UberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, color = UberWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("$family • ${device.radio.name}", color = UberMuted, fontSize = 11.sp)
                }
                Text("${device.rssi} dBm", color = UberAccentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Estimated distance", color = UberMuted, fontSize = 12.sp)
                Text(
                    formatDistance(device.estimatedDistanceMeters),
                    color = if (device.estimatedDistanceMeters < 3f) UberGreen else UberAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatDistance(meters: Float): String =
    if (meters < 10f) String.format(Locale.US, "%.1f m", meters)
    else String.format(Locale.US, "%.0f m", meters)
