package com.unlockedwear.protocol

import kotlinx.serialization.Serializable

/**
 * Defines the operational display modes for the UnlockedWear bezel gauge ring.
 *
 * Each mode specifies gauge scale boundaries, human-readable labels, and primary color aesthetics.
 *
 * @property label User-facing display title for this mode.
 * @property minValue The minimum scale value for the bezel gauge arc.
 * @property maxValue The maximum scale value for the bezel gauge arc.
 * @property colorHex The primary HEX color string (`#RRGGBB`) representing this mode.
 */
@Serializable
enum class RingMode(
    val label: String,
    val minValue: Float,
    val maxValue: Float,
    val colorHex: String
) {
    /**
     * Real-time Erythemal UV Index monitoring.
     * Uses a dynamic green-to-purple WHO scale (anchored at purple `#9C27B0` for extreme vigilance).
     */
    UV(
        label = "UV Index",
        minValue = 0.0f,
        maxValue = 12.0f,
        colorHex = "#9C27B0"
    ),

    /**
     * Biometric health and autonomic recovery readiness tracking (0 - 100%).
     * Visualized with an energetic cyan accent (`#00BCD4`).
     */
    HEALTH(
        label = "Health",
        minValue = 0.0f,
        maxValue = 100.0f,
        colorHex = "#00BCD4"
    ),

    /**
     * Deep work and cognitive focus block tracking (0 - 100% or session minutes).
     * Visualized with a vibrant amber accent (`#FFC107`).
     */
    FOCUS(
        label = "Focus",
        minValue = 0.0f,
        maxValue = 100.0f,
        colorHex = "#FFC107"
    ),

    /**
     * Power reserve, device battery percentage, or physical energy stamina (0 - 100%).
     * Visualized with a vibrant green accent (`#4CAF50`).
     */
    POWER(
        label = "Power",
        minValue = 0.0f,
        maxValue = 100.0f,
        colorHex = "#4CAF50"
    );

    /**
     * Resolves the display HEX color for this mode.
     * For [UV], dynamically maps across the WHO color spectrum (green -> yellow -> orange -> red -> purple).
     * For other modes, returns the fixed [colorHex].
     *
     * @param currentValue The active metric value to resolve.
     * @return HEX color string formatted as `#RRGGBB`.
     */
    fun resolveColorHex(currentValue: Float): String {
        return when (this) {
            UV -> {
                when {
                    currentValue < 3.0f -> "#4CAF50" // Low (Green)
                    currentValue < 6.0f -> "#FFEB3B" // Moderate (Yellow)
                    currentValue < 8.0f -> "#FF9800" // High (Orange)
                    currentValue < 11.0f -> "#F44336" // Very High (Red)
                    else -> "#9C27B0"                 // Extreme (Purple)
                }
            }
            HEALTH, FOCUS, POWER -> colorHex
        }
    }
}

/**
 * Encapsulates the real-time visual and metric state of the UnlockedWear watch face ring gauge.
 *
 * @property mode The active [RingMode] currently rendered.
 * @property currentValue The raw numeric value being visualized.
 * @property minValue The lower boundary of the gauge arc (defaults to [RingMode.minValue]).
 * @property maxValue The upper boundary of the gauge arc (defaults to [RingMode.maxValue]).
 * @property label The display label rendered on the watch face (defaults to [RingMode.label]).
 * @property formattedMetric Formatted metric string ready for UI rendering (e.g., "5.4 UV", "88%", "45 min").
 */
@Serializable
data class RingState(
    val mode: RingMode,
    val currentValue: Float,
    val minValue: Float = mode.minValue,
    val maxValue: Float = mode.maxValue,
    val label: String = mode.label,
    val formattedMetric: String
) {
    /**
     * Normalized progress fraction clamped to the interval `[0.0f, 1.0f]`.
     */
    val progressFraction: Float
        get() {
            val span = maxValue - minValue
            return if (span > 0.0f) {
                ((currentValue - minValue) / span).coerceIn(0.0f, 1.0f)
            } else {
                0.0f
            }
        }

    /**
     * The dynamic or fixed HEX color string appropriate for [currentValue] under [mode].
     */
    val effectiveColorHex: String
        get() = mode.resolveColorHex(currentValue)
}
