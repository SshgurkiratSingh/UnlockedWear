package com.unlockedwear.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.ui.components.UberBlack
import com.unlockedwear.mobile.ui.components.UberCard
import com.unlockedwear.mobile.ui.components.UberCardBorder
import com.unlockedwear.mobile.ui.components.UberMuted
import com.unlockedwear.mobile.ui.components.UberPrimaryButton
import com.unlockedwear.mobile.ui.components.UberSectionHeader
import com.unlockedwear.mobile.ui.components.UberWhite
import com.unlockedwear.mobile.ui.viewmodels.SettingsViewModel
import com.unlockedwear.protocol.RingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val weatherKey by viewModel.weatherApiKey.collectAsState()
    val preferredMode by viewModel.preferredRingMode.collectAsState()

    var tempWeather by remember(weatherKey) { mutableStateOf(weatherKey) }
    var isModeMenuExpanded by remember { mutableStateOf(false) }

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
        Column {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = UberWhite
            )
            Text(
                text = "System Preferences & Credentials",
                fontSize = 13.sp,
                color = UberMuted
            )
        }

        // Section 1: Weather Credentials
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UberSectionHeader(title = "OpenWeatherMap API Key")

                ApiKeyInputField(
                    label = "API Key (Optional)",
                    value = tempWeather,
                    onValueChange = { tempWeather = it },
                    onSave = { viewModel.saveWeatherKey(tempWeather) }
                )
            }
        }

        // Section 2: Sync Schedule
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UberSectionHeader(title = "Background Synchronization")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Weather & UV Sync Interval", color = UberWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Telemetry push to Galaxy Watch 8", color = UberMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Every 2 Hours", color = UberWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 3: Ring Mode Defaults
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UberSectionHeader(title = "Default Watch Face Ring Mode")

                ExposedDropdownMenuBox(
                    expanded = isModeMenuExpanded,
                    onExpandedChange = { isModeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = preferredMode.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModeMenuExpanded) },
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
                        expanded = isModeMenuExpanded,
                        onDismissRequest = { isModeMenuExpanded = false }
                    ) {
                        RingMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    viewModel.setPreferredRingMode(mode)
                                    isModeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 4: About
        UberCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UberSectionHeader(title = "About UnlockedWear")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Version 1.0.0-PROD (Companion Hub)", fontSize = 13.sp, color = UberWhite, fontWeight = FontWeight.SemiBold)
                Text(
                    "Engineered for Samsung Galaxy S25 interfacing with Galaxy Watch 8 running Wear OS 5+.\nPowered by Samsung Health Sensor SDK 1.4.1.",
                    fontSize = 12.sp,
                    color = UberMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ApiKeyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 12.sp, color = UberMuted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF13151D),
                    unfocusedContainerColor = Color(0xFF13151D),
                    focusedTextColor = UberWhite,
                    unfocusedTextColor = UberWhite,
                    focusedBorderColor = UberCardBorder,
                    unfocusedBorderColor = UberCardBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            UberPrimaryButton(
                text = "Save",
                onClick = onSave,
                modifier = Modifier.width(90.dp)
            )
        }
    }
}
