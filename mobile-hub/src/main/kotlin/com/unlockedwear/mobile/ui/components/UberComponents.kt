package com.unlockedwear.mobile.ui.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Uber Dark Aesthetic Palette
val UberBlack = Color(0xFF000000)
val UberDarkBg = Color(0xFF0D0E12)
val UberCardBg = Color(0xFF181A22)
val UberCardBorder = Color(0xFF282C3A)
val UberWhite = Color(0xFFFFFFFF)
val UberMuted = Color(0xFF94A3B8)
val UberAccentBlue = Color(0xFF2563EB)
val UberGreen = Color(0xFF10B981)
val UberAmber = Color(0xFFF59E0B)
val UberRed = Color(0xFFEF4444)

/**
 * Uber-style surface card container with sharp contrast and clean border.
 */
@Composable
fun UberCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = UberCardBg,
    borderColor: Color = UberCardBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = shape)
    ) {
        content()
    }
}

/**
 * Uber primary full-width action button (White background with bold black text).
 */
@Composable
fun UberPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = UberWhite,
    contentColor: Color = UberBlack
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Uber secondary action button (Dark grey container with clean white text).
 */
@Composable
fun UberSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF262936))
            .border(1.dp, UberCardBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = UberWhite
        )
    }
}

/**
 * Uber clean section header.
 */
@Composable
fun UberSectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = UberWhite
        )

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = UberAccentBlue,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

/**
 * Uber pill chip for status or selections.
 */
@Composable
fun UberChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = UberWhite,
    activeTextColor: Color = UberBlack
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) activeColor else Color(0xFF222531))
            .border(
                width = 1.dp,
                color = if (selected) activeColor else UberCardBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeTextColor else UberMuted
        )
    }
}

/**
 * Uber clean status pill with colored dot indicator.
 */
@Composable
fun UberStatusPill(
    text: String,
    dotColor: Color = UberGreen,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E212B))
            .border(1.dp, UberCardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = UberWhite
        )
    }
}
