package com.unlockedwear.protocol

import kotlinx.serialization.Serializable

/**
 * Extended watch-face feature flags pushed from the Galaxy S25 companion app
 * to the Galaxy Watch via [DataLayerPaths.CONFIG_SYNC_FEATURES].
 *
 * Each flag enables a unique biometric or contextual overlay that standard
 * Wear OS / Samsung watch faces do not offer:
 *
 * | Flag                  | What it shows on the watch face                                          |
 * |-----------------------|--------------------------------------------------------------------------|
 * | bioReactiveArc        | Mission Ring arc colour shifts red→amber→green based on live HR zone     |
 * | hrvBand               | Thin HRV autonomic-zone colour strip below the clock                     |
 * | circadianSector       | 12-segment circadian wheel around the outer ring                         |
 * | thermalStrip          | Skin-temperature delta (ΔT) pill between clock and pods                  |
 * | coPilotTicker         | Scrolling AI headline band above the clock                               |
 * | uvZoneArc             | UV index secondary arc at 60% diameter, WHO colour-coded                 |
 * | recoveryScore         | HRV-derived overnight recovery score badge (0-100)                       |
 * | focusCountdown        | Pomodoro / deep-work countdown strip (replaces seconds sub-dial)         |
 */
@Serializable
data class WatchFaceFeatureFlags(
    /** Bio-reactive ring: arc colour driven by real-time HR zone (red/amber/green). */
    val bioReactiveArc: Boolean = true,
    /** HRV autonomic zone strip: thin coloured bar below clock showing parasympathetic state. */
    val hrvBand: Boolean = true,
    /** Circadian sector wheel: outer segmented ring showing current body-clock phase. */
    val circadianSector: Boolean = false,
    /** Thermal delta strip: live ΔT skin-temp pill on the dial. */
    val thermalStrip: Boolean = true,
    /** Co-Pilot headline ticker: scrolling AI briefing band above the clock. */
    val coPilotTicker: Boolean = true,
    /** UV zone arc: secondary arc ring colour-coded by WHO UV risk level. */
    val uvZoneArc: Boolean = false,
    /** Recovery score badge: overnight HRV-derived readiness score (0-100). */
    val recoveryScore: Boolean = false,
    /** Focus countdown strip: replaces seconds sub-dial with active Pomodoro timer. */
    val focusCountdown: Boolean = false
)
