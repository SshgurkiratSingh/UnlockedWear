package com.unlockedwear.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uber Design System Palette
val UberBlack = Color(0xFF0A0B0E)
val UberCardBg = Color(0xFF161822)
val UberCardBorder = Color(0xFF2A2D3D)
val UberWhite = Color(0xFFFFFFFF)
val UberMuted = Color(0xFF94A3B8)
val UberGreen = Color(0xFF10B981)
val UberBlue = Color(0xFF2563EB)
val UberAmber = Color(0xFFF59E0B)
val UberRed = Color(0xFFEF4444)

// Legacy Palette Aliases for Compatibility
val SpaceBlack = UberBlack
val DeepIndigoSurface = UberCardBg
val CardSurface = UberCardBg
val CyanAccent = UberWhite
val OrangeWarning = UberAmber
val PurpleCosmic = UberBlue
val GreenStamina = UberGreen
val AmberFocus = UberAmber
val TextPrimary = UberWhite
val TextSecondary = UberMuted

private val UberDarkColorScheme = darkColorScheme(
    primary = UberWhite,
    onPrimary = UberBlack,
    primaryContainer = Color(0xFF222533),
    onPrimaryContainer = UberWhite,
    secondary = UberBlue,
    onSecondary = UberWhite,
    background = UberBlack,
    onBackground = UberWhite,
    surface = UberCardBg,
    onSurface = UberWhite,
    surfaceVariant = UberCardBg,
    onSurfaceVariant = UberMuted,
    error = UberRed,
    onError = UberWhite
)

val UberTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = UberWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        color = UberWhite
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = UberWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = UberWhite
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = UberMuted
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = UberMuted
    )
)

/**
 * Uber-inspired Material 3 Dark theme for the UnlockedWear companion app on Galaxy S25.
 */
@Composable
fun UnlockedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UberDarkColorScheme,
        typography = UberTypography,
        content = content
    )
}
