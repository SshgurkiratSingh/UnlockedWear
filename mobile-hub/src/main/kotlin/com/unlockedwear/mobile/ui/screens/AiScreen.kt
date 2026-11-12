package com.unlockedwear.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.ui.components.CyberButton
import com.unlockedwear.mobile.ui.components.GlassCard
import com.unlockedwear.mobile.ui.components.GlassFilterChip
import com.unlockedwear.mobile.ui.components.PulsingStatusDot
import com.unlockedwear.mobile.ui.theme.CardSurface
import com.unlockedwear.mobile.ui.theme.CyanAccent
import com.unlockedwear.mobile.ui.theme.DeepIndigoSurface
import com.unlockedwear.mobile.ui.theme.PurpleCosmic
import com.unlockedwear.mobile.ui.theme.SpaceBlack
import com.unlockedwear.mobile.ui.theme.TextPrimary
import com.unlockedwear.mobile.ui.theme.TextSecondary
import com.unlockedwear.mobile.ui.viewmodels.AiEngineStatus
import com.unlockedwear.mobile.ui.viewmodels.AiViewModel
import com.unlockedwear.mobile.ui.viewmodels.ChatMessage

@Composable
fun AiScreen(
    viewModel: AiViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val engineStatus by viewModel.engineStatus.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isAnalyticalMode by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .padding(16.dp)
    ) {
        // App Header
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
                    text = "HYBRID INTELLIGENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "AI Co-Pilot Hub",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Engine Status Card
        EngineStatusBar(status = engineStatus)

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Command Triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CyberButton(
                onClick = { viewModel.triggerManualBriefing() },
                accentColor = PurpleCosmic,
                secondaryColor = CardSurface,
                modifier = Modifier.weight(1f)
            ) {
                Text("Synthesize Briefing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            CyberButton(
                onClick = { viewModel.requestDeepSeekAnalysis("Analyze nocturnal thermal stability and recommend optimal deep work duration.") },
                accentColor = CyanAccent,
                secondaryColor = DeepIndigoSurface,
                modifier = Modifier.weight(1f)
            ) {
                Text("Thermal Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpaceBlack)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            if (isLoading) {
                item {
                    GlassCard(
                        borderColor = CyanAccent.copy(alpha = 0.3f),
                        gradientColors = listOf(CardSurface, DeepIndigoSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = if (isAnalyticalMode) PurpleCosmic else CyanAccent,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Synthesizing inference with ${if (isAnalyticalMode) "DeepSeek" else "Gemini"}...",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Engine Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassFilterChip(
                selected = !isAnalyticalMode,
                onClick = { isAnalyticalMode = false },
                label = "Gemini Flash (Interactive)",
                accentColor = CyanAccent,
                modifier = Modifier.weight(1f)
            )

            GlassFilterChip(
                selected = isAnalyticalMode,
                onClick = { isAnalyticalMode = true },
                label = "DeepSeek (Analytical)",
                accentColor = PurpleCosmic,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Field and Send Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isAnalyticalMode) "Ask DeepSeek analytical query..." else "Ask Gemini assistant...",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isAnalyticalMode) PurpleCosmic else CyanAccent,
                    unfocusedBorderColor = CardSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardSurface.copy(alpha = 0.8f),
                    unfocusedContainerColor = CardSurface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotBlank()) {
                        inputText = ""
                        if (isAnalyticalMode) {
                            viewModel.requestDeepSeekAnalysis(text)
                        } else {
                            viewModel.sendInteractiveQuery(text)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAnalyticalMode) PurpleCosmic else CyanAccent)
            ) {
                Text(
                    text = "➔",
                    color = if (isAnalyticalMode) Color.White else SpaceBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EngineStatusBar(status: AiEngineStatus) {
    GlassCard(
        borderColor = PurpleCosmic.copy(alpha = 0.3f),
        gradientColors = listOf(CardSurface, DeepIndigoSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingStatusDot(color = PurpleCosmic, pulse = true)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DeepSeek: ${status.deepSeekStatus}", fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingStatusDot(color = CyanAccent, pulse = true)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini: ${status.geminiStatus}", fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        message.engine?.let { engine ->
            Text(
                text = engine.uppercase(),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (engine.contains("DeepSeek")) PurpleCosmic else CyanAccent,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        GlassCard(
            borderColor = if (isUser) CyanAccent.copy(alpha = 0.4f) else PurpleCosmic.copy(alpha = 0.25f),
            gradientColors = if (isUser) {
                listOf(DeepIndigoSurface.copy(alpha = 0.95f), CardSurface.copy(alpha = 0.85f))
            } else {
                listOf(CardSurface.copy(alpha = 0.90f), DeepIndigoSurface.copy(alpha = 0.75f))
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
