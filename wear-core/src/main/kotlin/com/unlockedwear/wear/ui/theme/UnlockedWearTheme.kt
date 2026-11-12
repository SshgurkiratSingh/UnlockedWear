package com.unlockedwear.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

// ─────────────────────────────────────────────────────────────────────────────
//  UNLOCKED WEAR — OBSIDIAN GLASS DESIGN SYSTEM
//  Inspired by high-end aviation HUDs and premium watch movements.
//  Palette: near-black base, single electric-blue primary, warm amber accent.
// ─────────────────────────────────────────────────────────────────────────────

// Blacks & surfaces — true AMOLED-safe near-blacks
val Void          = Color(0xFF050508)   // screen background
val Obsidian      = Color(0xFF0C0C12)   // card bg
val ObsidianLift  = Color(0xFF13131C)   // elevated card / row bg
val ObsidianRim   = Color(0xFF1E1E2A)   // subtle border / divider

// Primary — electric arctic blue
val ElectricBlue  = Color(0xFF3B82F6)   // primary interactive / ring arc
val ElectricBlueDim = Color(0xFF1D4ED8) // darker shade for fills

// Accent — warm amber / gold
val Amber         = Color(0xFFF59E0B)   // HR, biometric highlight
val AmberDim      = Color(0xFF92400E)

// Status / data colours
val Mint          = Color(0xFF10B981)   // good/healthy
val MintDim       = Color(0xFF065F46)
val Coral         = Color(0xFFEF4444)   // danger / high load
val CoralDim      = Color(0xFF7F1D1D)
val Lavender      = Color(0xFF8B5CF6)   // ECG / neural
val Sage          = Color(0xFF6EE7B7)   // SpO2 / oxygen
val SageWhite     = Color(0xFFD1FAE5)   // faint tint

// Text — crisp whites, no blue tints
val White100      = Color(0xFFFFFFFF)
val White90       = Color(0xFFEBEBEB)
val White60       = Color(0xFF9A9AA8)
val White30       = Color(0xFF4A4A5A)
val White15       = Color(0xFF25252F)

// Legacy aliases kept for existing screens that still import by old name
val SpaceBlack         = Void
val SpaceCardSurface   = Obsidian
val SpaceSurfaceSubtle = ObsidianLift
val CyberCyan          = ElectricBlue
val CyberCyanDim       = ElectricBlueDim
val NeonOrange         = Amber
val BioGreen           = Mint
val BioGreenDim        = MintDim
val SolarYellow        = Amber
val DangerRed          = Coral
val TextPrimary        = White90
val TextSecondary      = White60
val TextMuted          = White30

// ─────────────────────────────────────────────────────────────────────────────
//  Colour palette wired into Wear Compose MaterialTheme
// ─────────────────────────────────────────────────────────────────────────────
private val ObsidianGlassColors = Colors(
    primary          = ElectricBlue,
    primaryVariant   = ElectricBlueDim,
    secondary        = Amber,
    secondaryVariant = AmberDim,
    background       = Void,
    surface          = Obsidian,
    onPrimary        = White100,
    onSecondary      = Void,
    onBackground     = White90,
    onSurface        = White90,
    onSurfaceVariant = White60,
    error            = Coral
)

// ─────────────────────────────────────────────────────────────────────────────
//  Typography — Inter-influenced, monospace numerics
// ─────────────────────────────────────────────────────────────────────────────
private val ObsidianTypography = Typography(
    // 44 sp — large numerics (HR, temp hero value)
    display1 = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Bold,
        fontSize      = 44.sp,
        letterSpacing = (-1).sp,
        color         = White90
    ),
    // 36 sp — clock time
    display2 = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Bold,
        fontSize      = 36.sp,
        letterSpacing = (-0.5).sp,
        color         = White90
    ),
    // 28 sp — secondary hero values
    display3 = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Medium,
        fontSize      = 28.sp,
        color         = White90
    ),
    // 15 sp bold — section / card titles
    title1 = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 15.sp,
        letterSpacing = 0.1.sp,
        color         = White90
    ),
    // 13 sp — sub-titles
    title2 = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        color         = White90
    ),
    // 13 sp normal — body
    body1 = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        color         = White60
    ),
    // 12 sp — secondary body
    body2 = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 17.sp,
        color         = White60
    ),
    // 12 sp semi-bold — buttons
    button = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        letterSpacing = 0.5.sp,
        color         = White90
    ),
    // 10 sp mono — labels / kicker text
    caption1 = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        letterSpacing = 1.sp,
        color         = White60
    ),
    // 9 sp mono — micro labels
    caption2 = TextStyle(
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.sp,
        letterSpacing = 1.2.sp,
        color         = White30
    )
)

/**
 * Top-level theme composable for UnlockedWear on Galaxy Watch.
 * Uses the Obsidian Glass design language — deep AMOLED blacks,
 * single electric-blue primary, warm amber biometric accent.
 */
@Composable
fun UnlockedWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors     = ObsidianGlassColors,
        typography = ObsidianTypography,
        content    = content
    )
}
