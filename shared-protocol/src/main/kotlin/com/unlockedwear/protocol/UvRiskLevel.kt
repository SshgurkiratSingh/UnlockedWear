package com.unlockedwear.protocol

import kotlinx.serialization.Serializable

/**
 * World Health Organization (WHO) and EPA standardized Erythemal UV Index risk levels.
 *
 * Each level defines the index threshold range, canonical visualization color,
 * and user-facing risk classification label.
 *
 * @property range The discrete integer UV Index range for this risk band.
 * @property colorHex Standardized HEX color string (`#RRGGBB`) representing the severity level.
 * @property label Human-readable risk label.
 */
@Serializable
enum class UvRiskLevel(
    val range: IntRange,
    val colorHex: String,
    val label: String
) {
    /**
     * Low risk (UVI 0..2). Minimal sun protection required for normal exposure.
     */
    LOW(0..2, "#4CAF50", "Low"),

    /**
     * Moderate risk (UVI 3..5). Sun protection recommended during midday hours.
     */
    MODERATE(3..5, "#FFEB3B", "Moderate"),

    /**
     * High risk (UVI 6..7). Sun protection essential; reduce direct sun exposure between 10am and 4pm.
     */
    HIGH(6..7, "#FF9800", "High"),

    /**
     * Very high risk (UVI 8..10). Extra protection required; minimize outdoor exposure during midday.
     */
    VERY_HIGH(8..10, "#F44336", "Very High"),

    /**
     * Extreme risk (UVI 11+). Full protection required; avoid sun exposure during peak sunlight hours.
     */
    EXTREME(11..Int.MAX_VALUE, "#9C27B0", "Extreme");

    /**
     * Returns true if the given floating-point UV index falls within this risk level's category.
     *
     * @param uvIndex Measured or forecasted Erythemal UV index.
     */
    fun matches(uvIndex: Float): Boolean {
        val clamped = uvIndex.coerceAtLeast(0.0f)
        return when (this) {
            LOW -> clamped < 3.0f
            MODERATE -> clamped in 3.0f..<6.0f
            HIGH -> clamped in 6.0f..<8.0f
            VERY_HIGH -> clamped in 8.0f..<11.0f
            EXTREME -> clamped >= 11.0f
        }
    }

    companion object {
        /**
         * Resolves the appropriate [UvRiskLevel] for a given [uvIndex] reading.
         *
         * @param uvIndex Real-time or forecasted UV Index.
         * @return The corresponding [UvRiskLevel].
         */
        fun fromUvIndex(uvIndex: Float): UvRiskLevel = uvRiskLevel(uvIndex)
    }
}
