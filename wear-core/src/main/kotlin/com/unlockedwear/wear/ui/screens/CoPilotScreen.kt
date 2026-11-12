package com.unlockedwear.wear.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.unlockedwear.protocol.DataLayerPaths
import com.unlockedwear.protocol.ScheduleBlock
import com.unlockedwear.wear.data.DataSyncService
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.ui.theme.BioGreen
import com.unlockedwear.wear.ui.theme.CyberCyan
import com.unlockedwear.wear.ui.theme.NeonOrange
import com.unlockedwear.wear.ui.theme.SpaceBlack
import com.unlockedwear.wear.ui.theme.SpaceCardSurface
import com.unlockedwear.wear.ui.theme.TextMuted
import com.unlockedwear.wear.ui.theme.TextPrimary
import com.unlockedwear.wear.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying full DeepSeek AI Co-Pilot briefing, prioritized tasks, schedule, and voice dispatch.
 */
@Composable
fun CoPilotScreen(
    database: WearDatabase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val json = remember { Json { ignoreUnknownKeys = true } }

    val latestBriefing by database.briefingDao().observeLatestBriefing().collectAsState(initial = null)

    var isAnalyzing by remember { mutableStateOf(false) }

    val scheduleBlocks: List<ScheduleBlock> = remember(latestBriefing?.scheduleBlocksJson) {
        val raw = latestBriefing?.scheduleBlocksJson
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<List<ScheduleBlock>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            listOf(
                ScheduleBlock("1", "Morning UV Exposure Window", System.currentTimeMillis(), System.currentTimeMillis() + 1800000, "Outdoors", false),
                ScheduleBlock("2", "Deep Work: Architecture", System.currentTimeMillis() + 3600000, System.currentTimeMillis() + 9000000, "Studio", true)
            )
        }
    }

    val alerts: List<String> = remember(latestBriefing?.alertsJson) {
        val raw = latestBriefing?.alertsJson
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<List<String>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            listOf("Peak Solar UV expected at 13:00 (UVI 8.2)")
        }
    }

    fun requestDeepSeekAnalysis() {
        if (isAnalyzing) return
        isAnalyzing = true
        coroutineScope.launch {
            val sent = DataSyncService.sendMessageToPhone(
                context,
                DataLayerPaths.AI_DEEPSEEK_TRIGGER,
                "{}".toByteArray(Charsets.UTF_8)
            )
            isAnalyzing = false
            Toast.makeText(
                context,
                if (sent) "Analysis requested from phone" else "Phone not reachable",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchGeminiVoice() {
        coroutineScope.launch {
            val sent = DataSyncService.sendMessageToPhone(
                context,
                DataLayerPaths.AI_GEMINI_TRIGGER,
                "{}".toByteArray(Charsets.UTF_8)
            )
            Toast.makeText(
                context,
                if (sent) "Gemini Voice initiated..." else "Phone disconnected",
                Toast.LENGTH_SHORT
            ).show()
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
                        text = "DEEPSEEK CO-PILOT",
                        style = MaterialTheme.typography.caption2,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = latestBriefing?.date ?: "DAILY BRIEFING",
                        style = MaterialTheme.typography.caption2,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // Headline Card
            item {
                val headline = latestBriefing?.coPilotHeadline
                    ?: "All physiological indicators stable. Prepare for scheduled high-focus block."

                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = SpaceCardSurface,
                        endBackgroundColor = SpaceCardSurface
                    ),
                    contentColor = TextPrimary

                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "MISSION HEADLINE",
                            fontSize = 8.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Priority Task
            item {
                val priority = latestBriefing?.priorityTask
                    ?: "Execute 90-minute deep work block with phone muted"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF131D2A))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCyan)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRIORITY",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpaceBlack
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = priority,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Schedule Blocks Section Header
            item {
                Text(
                    text = "SCHEDULE TIMELINE",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Schedule Block Items
            items(scheduleBlocks.size) { index ->
                val block = scheduleBlocks[index]
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = "${timeFormat.format(Date(block.startTime))} - ${timeFormat.format(Date(block.endTime))}"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpaceCardSurface)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = block.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            if (block.location != null) {
                                Text(
                                    text = block.location,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        Text(
                            text = timeStr,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (block.isDeepWork) CyberCyan else BioGreen
                        )
                    }
                }
            }

            // Alerts Section
            if (alerts.isNotEmpty()) {
                item {
                    Text(
                        text = "ACTIVE ALERTS",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(alerts.size) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF261515))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = alerts[index],
                            fontSize = 11.sp,
                            color = NeonOrange
                        )
                    }
                }
            }

            // Action Buttons: DeepSeek Analyze & Gemini Voice
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { requestDeepSeekAnalysis() },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.primaryButtonColors(
                        backgroundColor = CyberCyan,
                        contentColor = SpaceBlack
                    )
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(14.dp).width(14.dp),
                            strokeWidth = 2.dp,
                            indicatorColor = SpaceBlack
                        )
                    } else {
                        Text(
                            text = "ANALYZE NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpaceBlack
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { launchGeminiVoice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.secondaryButtonColors(
                        backgroundColor = SpaceCardSurface,
                        contentColor = CyberCyan
                    )
                ) {
                    Text(
                        text = "GEMINI VOICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
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
