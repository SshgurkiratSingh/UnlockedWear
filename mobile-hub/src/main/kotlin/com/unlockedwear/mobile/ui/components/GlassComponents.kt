package com.unlockedwear.mobile.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unlockedwear.mobile.ui.theme.CardSurface
import com.unlockedwear.mobile.ui.theme.CyanAccent
import com.unlockedwear.mobile.ui.theme.DeepIndigoSurface
import com.unlockedwear.mobile.ui.theme.GreenStamina
import com.unlockedwear.mobile.ui.theme.OrangeWarning
import com.unlockedwear.mobile.ui.theme.PurpleCosmic
import com.unlockedwear.mobile.ui.theme.SpaceBlack
import com.unlockedwear.mobile.ui.theme.TextPrimary
import com.unlockedwear.mobile.ui.theme.TextSecondary

/**
 * High-precision glassmorphic container card with subtle gradient fill and glowing accent border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color = CyanAccent.copy(alpha = 0.25f),
    borderWidth: Dp = 1.dp,
    gradientColors: List<Color> = listOf(
        Color(0xFF131B2E).copy(alpha = 0.95f),
        Color(0xFF0F1525).copy(alpha = 0.90f)
    ),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(gradientColors)
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * Pulsing live status indicator dot for hardware and AI engine states.
 */
@Composable
fun PulsingStatusDot(
    color: Color = GreenStamina,
    size: Dp = 8.dp,
    pulse: Boolean = true
) {
    if (!pulse) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(size * 1.8f)) {
        drawCircle(
            color = color.copy(alpha = alpha * 0.35f),
            radius = (size.toPx() * 0.9f)
        )
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = (size.toPx() * 0.5f)
        )
    }
}

/**
 * Sleek telemetry section header with monospaced label and optional action widget.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = CyanAccent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(3.dp, 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = badgeColor,
                    maxLines = 1
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 11.dp)
                )
            }
        }

        if (badgeText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Premium cyber action button with linear gradient fill and subtle glow border.
 */
@Composable
fun CyberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = CyanAccent,
    secondaryColor: Color = PurpleCosmic,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (enabled) {
                        listOf(accentColor, secondaryColor)
                    } else {
                        listOf(CardSurface, CardSurface)
                    }
                )
            )
            .border(
                width = 1.dp,
                color = if (enabled) accentColor.copy(alpha = 0.6f) else Color.DarkGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Glassmorphic status chip for filter selections and mode toggles.
 */
@Composable
fun GlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    accentColor: Color = CyanAccent,
    modifier: Modifier = Modifier
) {
    val bgColors = if (selected) {
        listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.10f))
    } else {
        listOf(CardSurface.copy(alpha = 0.8f), CardSurface.copy(alpha = 0.5f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(bgColors))
            .border(
                width = 1.dp,
                color = if (selected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else TextSecondary
        )
    }
}
