package com.unlockedwear.protocol

import kotlinx.serialization.Serializable

/**
 * Represents the estimated mental workload or cognitive demand of a task or work session.
 *
 * Used by the UnlockedWear scheduling system and AI Co-Pilot to dynamically structure
 * focus blocks, adjust recovery targets, and prevent cognitive fatigue.
 *
 * @property label Human-readable description of the load level.
 * @property weight Relative numeric multiplier used in workload balancing algorithms.
 */
@Serializable
enum class CognitiveLoad(
    val label: String,
    val weight: Int
) {
    /**
     * Low cognitive intensity: administrative work, light email triage, or routine updates.
     */
    LOW(label = "Low", weight = 1),

    /**
     * Moderate cognitive intensity: collaborative reviews, structured design, or documentation.
     */
    MEDIUM(label = "Medium", weight = 2),

    /**
     * High cognitive intensity: complex algorithm development, deep debugging, or architectural synthesis.
     */
    HIGH(label = "High", weight = 3);

    companion object {
        /**
         * Safely parses a [CognitiveLoad] from a string name (case-insensitive), defaulting to [MEDIUM].
         *
         * @param value The raw string representation.
         * @return The corresponding [CognitiveLoad], or [MEDIUM] if unrecognized.
         */
        fun fromString(value: String?): CognitiveLoad {
            return when (value?.trim()?.uppercase()) {
                "LOW" -> LOW
                "HIGH" -> HIGH
                else -> MEDIUM
            }
        }
    }
}
