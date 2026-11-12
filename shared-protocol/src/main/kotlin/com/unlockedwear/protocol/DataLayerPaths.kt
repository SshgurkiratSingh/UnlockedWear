package com.unlockedwear.protocol

/**
 * Constants defining the Wearable Data Layer API paths used for communication
 * between the Wear OS watch (`wear-core`) and the mobile companion app (`mobile-hub`).
 *
 * Paths follow the canonical URI-like structure `/unlockedwear/<domain>/<action_or_resource>`.
 */
object DataLayerPaths {

    /**
     * Common prefix identifying all UnlockedWear communication channels.
     */
    const val PREFIX = "/unlockedwear"

    /**
     * Path for transmitting raw and compensated thermal sensor frames from watch to mobile.
     */
    const val SENSOR_THERMAL = "/unlockedwear/sensor/thermal"

    /**
     * Path for transmitting aggregated biometric telemetry snapshots (HR, HRV, SpO2, skin temp) from watch to mobile.
     */
    const val SENSOR_BIOMETRIC = "/unlockedwear/sensor/biometric"

    /**
     * Path for synchronizing synthesized daily briefing state, AI co-pilot headline, and alerts from mobile to watch.
     */
    const val STATE_DAILY_BRIEFING = "/unlockedwear/state/daily_briefing"

    /**
     * Path for synchronizing active ring mode and gauge metrics between watch and mobile.
     */
    const val STATE_RING_MODE = "/unlockedwear/state/ring_mode"

    /**
     * Path for synchronizing real-time environmental context, UV forecasts, and atmospheric trends from mobile to watch.
     */
    const val STATE_WEATHER = "/unlockedwear/state/weather"

    /**
     * Path for triggering local or device-hosted DeepSeek AI reasoning engine inferences.
     */
    const val AI_DEEPSEEK_TRIGGER = "/unlockedwear/ai/deepseek_trigger"

    /**
     * Path for triggering cloud-based multimodal Gemini Co-Pilot analysis inferences.
     */
    const val AI_GEMINI_TRIGGER = "/unlockedwear/ai/gemini_trigger"

    /**
     * Path for bidirectional configuration and user preferences synchronization.
     */
    const val CONFIG_SYNC = "/unlockedwear/config/sync"

    /**
     * Path for commanding an immediate on-demand temperature measurement sweep from mobile to watch.
     */
    const val COMMAND_READ_TEMP = "/unlockedwear/command/read_temp"

    /**
     * Validates if a given URI path belongs to the UnlockedWear protocol.
     *
     * @param path The path string to inspect.
     * @return `true` if the path starts with [PREFIX], `false` otherwise.
     */
    fun isUnlockedWearPath(path: String?): Boolean = path != null && path.startsWith(PREFIX)
}
