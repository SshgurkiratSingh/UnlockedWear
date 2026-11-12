package com.unlockedwear.wear.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.unlockedwear.wear.complications.MissionRingComplicationService
import com.unlockedwear.wear.data.RingStateDataStore
import com.unlockedwear.wear.ui.theme.BioGreen
import com.unlockedwear.wear.ui.theme.CyberCyan
import com.unlockedwear.wear.ui.theme.DangerRed
import com.unlockedwear.wear.ui.theme.SolarYellow
import com.unlockedwear.wear.ui.theme.SpaceBlack
import com.unlockedwear.wear.ui.theme.SpaceCardSurface
import com.unlockedwear.wear.ui.theme.TextMuted
import com.unlockedwear.wear.ui.theme.TextPrimary
import com.unlockedwear.wear.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Crown-scrollable selector allowing the user to select the active Mission Ring mode.
 */
@Composable
fun RingSelectorScreen(
    ringStateDataStore: RingStateDataStore,
    onModeSelected: (RingMode) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberScalingLazyListState()

    val currentRingState by ringStateDataStore.ringStateFlow.collectAsState(
        initial = RingState(
            mode = RingMode.UV,
            currentValue = 3.5f,
            minValue = 0f,
            maxValue = 12f,
            label = "UV Solar",
            formattedMetric = "3.5 UVI"
        )
    )


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val modes = listOf(
        RingModeOption(
            mode = RingMode.UV,
            title = "UV SOLAR SHIELD",
            subtitle = "Sun exposure & UV index",
            previewValue = "7.2 UVI",
            accentColor = SolarYellow
        ),
        RingModeOption(
            mode = RingMode.HEALTH,
            title = "CARDIO BIOMETRICS",
            subtitle = "HR, RMSSD & SpO2",
            previewValue = "74 BPM",
            accentColor = DangerRed
        ),
        RingModeOption(
            mode = RingMode.FOCUS,
            title = "DEEP WORK FOCUS",
            subtitle = "Sprint & block timer",
            previewValue = "45 MIN",
            accentColor = CyberCyan
        ),
        RingModeOption(
            mode = RingMode.POWER,
            title = "SYSTEM POWER",
            subtitle = "Battery & thermal load",
            previewValue = "88 %",
            accentColor = BioGreen
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    listState.scrollBy(event.verticalScrollPixels)
                }
                true
            }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MISSION RINGS",
                        style = MaterialTheme.typography.caption2,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Select Primary Bezel Gauge",
                        style = MaterialTheme.typography.caption2,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Mode Selection Cards
            items(modes.size) { index ->
                val option = modes[index]
                val isSelected = (option.mode == currentRingState.mode)

                RingModeCard(
                    option = option,
                    isSelected = isSelected,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        coroutineScope.launch {
                            ringStateDataStore.setRingMode(option.mode)
                            MissionRingComplicationService.triggerImmediateRefresh(context)
                            onModeSelected(option.mode)
                        }
                    }
                )
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private data class RingModeOption(
    val mode: RingMode,
    val title: String,
    val subtitle: String,
    val previewValue: String,
    val accentColor: Color
)

@Composable
private fun RingModeCard(
    option: RingModeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, option.accentColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = if (isSelected) Color(0xFF191B28) else SpaceCardSurface,
            endBackgroundColor = if (isSelected) Color(0xFF191B28) else SpaceCardSurface
        ),
        contentColor = TextPrimary
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(option.accentColor)
                )
                Column {
                    Text(
                        text = option.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = option.subtitle,
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = option.previewValue,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = option.accentColor
            )
        }
    }
}
