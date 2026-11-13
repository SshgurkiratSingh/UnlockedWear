package com.unlockedwear.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.WatchFaceFeatureFlags
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed persistence for all watch face visual customization and
 * feature-flag settings pushed from the Galaxy S25 companion app via
 * [DataLayerPaths.CONFIG_SYNC] and [DataLayerPaths.CONFIG_SYNC_FEATURES].
 *
 * Base customization fields:
 * - Accent colour hex string
 * - Top / bottom complication slot labels
 * - Dial layout style
 * - Ring gauge mode override
 *
 * Extended feature flags (unique differentiators):
 * - [KEY_BIO_REACTIVE_ARC]  — ring arc shifts colour by HR zone
 * - [KEY_HRV_BAND]          — HRV autonomic state colour strip
 * - [KEY_CIRCADIAN_SECTOR]  — circadian phase wheel on outer ring
 * - [KEY_THERMAL_STRIP]     — live ΔT skin-temp pill
 * - [KEY_COPILOT_TICKER]    — scrolling AI briefing band
 * - [KEY_UV_ZONE_ARC]       — WHO UV index secondary arc
 * - [KEY_RECOVERY_SCORE]    — HRV overnight recovery badge
 * - [KEY_FOCUS_COUNTDOWN]   — Pomodoro focus-block countdown strip
 */
private val Context.watchFaceConfigDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "unlockedwear_watchface_config")

data class WatchFaceConfig(
    // ── Base ──────────────────────────────────────────────────────────────
    val accentColorHex: String = "#3B82F6",
    val topComplication: String = "Heart Rate (BPM)",
    val bottomComplication: String = "UV Index",
    val complicationSlots: List<String> = listOf("Heart Rate (BPM)", "UV Index"),
    val dialStyle: String = "Digital Modern",
    val ringMode: RingMode = RingMode.HEALTH,
    // ── Feature flags ─────────────────────────────────────────────────────
    val bioReactiveArc: Boolean = true,
    val hrvBand: Boolean = true,
    val circadianSector: Boolean = false,
    val thermalStrip: Boolean = true,
    val coPilotTicker: Boolean = true,
    val uvZoneArc: Boolean = false,
    val recoveryScore: Boolean = false,
    val focusCountdown: Boolean = false
) {
    /** Convenience accessor: convert feature booleans to [WatchFaceFeatureFlags]. */
    fun toFeatureFlags() = WatchFaceFeatureFlags(
        bioReactiveArc  = bioReactiveArc,
        hrvBand         = hrvBand,
        circadianSector = circadianSector,
        thermalStrip    = thermalStrip,
        coPilotTicker   = coPilotTicker,
        uvZoneArc       = uvZoneArc,
        recoveryScore   = recoveryScore,
        focusCountdown  = focusCountdown
    )
}

@Singleton
class WatchFaceConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Base keys
        val KEY_ACCENT_COLOR        = stringPreferencesKey("wf_accent_color")
        val KEY_TOP_COMPLICATION    = stringPreferencesKey("wf_top_complication")
        val KEY_BOTTOM_COMPLICATION = stringPreferencesKey("wf_bottom_complication")
        val KEY_COMPLICATION_SLOTS = stringPreferencesKey("wf_complication_slots")
        val KEY_DIAL_STYLE          = stringPreferencesKey("wf_dial_style")
        val KEY_RING_MODE           = stringPreferencesKey("wf_ring_mode")
        // Feature flag keys
        val KEY_BIO_REACTIVE_ARC   = booleanPreferencesKey("wf_bio_reactive_arc")
        val KEY_HRV_BAND           = booleanPreferencesKey("wf_hrv_band")
        val KEY_CIRCADIAN_SECTOR   = booleanPreferencesKey("wf_circadian_sector")
        val KEY_THERMAL_STRIP      = booleanPreferencesKey("wf_thermal_strip")
        val KEY_COPILOT_TICKER     = booleanPreferencesKey("wf_copilot_ticker")
        val KEY_UV_ZONE_ARC        = booleanPreferencesKey("wf_uv_zone_arc")
        val KEY_RECOVERY_SCORE     = booleanPreferencesKey("wf_recovery_score")
        val KEY_FOCUS_COUNTDOWN    = booleanPreferencesKey("wf_focus_countdown")
    }

    /** Reactive stream of the full [WatchFaceConfig]. */
    val configFlow: Flow<WatchFaceConfig> =
        context.watchFaceConfigDataStore.data.map { p ->
            val ringMode = try {
                RingMode.valueOf(p[KEY_RING_MODE] ?: RingMode.HEALTH.name)
            } catch (_: Exception) { RingMode.HEALTH }
            WatchFaceConfig(
                accentColorHex     = p[KEY_ACCENT_COLOR]        ?: "#3B82F6",
                topComplication    = p[KEY_TOP_COMPLICATION]     ?: "Heart Rate (BPM)",
                bottomComplication = p[KEY_BOTTOM_COMPLICATION]  ?: "UV Index",
                complicationSlots  = (p[KEY_COMPLICATION_SLOTS] ?: "")
                    .split("||").filter { it.isNotBlank() }
                    .ifEmpty { listOf(p[KEY_TOP_COMPLICATION] ?: "Heart Rate (BPM)", p[KEY_BOTTOM_COMPLICATION] ?: "UV Index") },
                dialStyle          = p[KEY_DIAL_STYLE]           ?: "Digital Modern",
                ringMode           = ringMode,
                bioReactiveArc     = p[KEY_BIO_REACTIVE_ARC]    ?: true,
                hrvBand            = p[KEY_HRV_BAND]             ?: true,
                circadianSector    = p[KEY_CIRCADIAN_SECTOR]     ?: false,
                thermalStrip       = p[KEY_THERMAL_STRIP]        ?: true,
                coPilotTicker      = p[KEY_COPILOT_TICKER]       ?: true,
                uvZoneArc          = p[KEY_UV_ZONE_ARC]          ?: false,
                recoveryScore      = p[KEY_RECOVERY_SCORE]       ?: false,
                focusCountdown     = p[KEY_FOCUS_COUNTDOWN]      ?: false
            )
        }

    /** One-shot snapshot. */
    suspend fun getConfig(): WatchFaceConfig = configFlow.first()

    /** Persist base config (sent via CONFIG_SYNC pipe). */
    suspend fun applyConfig(
        accentColorHex: String,
        topComplication: String,
        bottomComplication: String,
        dialStyle: String,
        ringMode: RingMode,
        complicationSlots: List<String> = listOf(topComplication, bottomComplication)
    ) {
        context.watchFaceConfigDataStore.edit { p ->
            p[KEY_ACCENT_COLOR]        = accentColorHex
            p[KEY_TOP_COMPLICATION]    = topComplication
            p[KEY_BOTTOM_COMPLICATION] = bottomComplication
            p[KEY_DIAL_STYLE]          = dialStyle
            p[KEY_RING_MODE]           = ringMode.name
            p[KEY_COMPLICATION_SLOTS]  = complicationSlots.joinToString("||")
        }
    }

    /** Persist all feature flags (sent via CONFIG_SYNC_FEATURES JSON). */
    suspend fun applyFeatureFlags(flags: WatchFaceFeatureFlags) {
        context.watchFaceConfigDataStore.edit { p ->
            p[KEY_BIO_REACTIVE_ARC]  = flags.bioReactiveArc
            p[KEY_HRV_BAND]          = flags.hrvBand
            p[KEY_CIRCADIAN_SECTOR]  = flags.circadianSector
            p[KEY_THERMAL_STRIP]     = flags.thermalStrip
            p[KEY_COPILOT_TICKER]    = flags.coPilotTicker
            p[KEY_UV_ZONE_ARC]       = flags.uvZoneArc
            p[KEY_RECOVERY_SCORE]    = flags.recoveryScore
            p[KEY_FOCUS_COUNTDOWN]   = flags.focusCountdown
        }
    }

    // ── Convenience single-key setters ────────────────────────────────────
    suspend fun setAccentColor(hex: String)             { context.watchFaceConfigDataStore.edit { it[KEY_ACCENT_COLOR]     = hex   } }
    suspend fun setDialStyle(style: String)             { context.watchFaceConfigDataStore.edit { it[KEY_DIAL_STYLE]       = style } }
    suspend fun setTopComplication(c: String)           { context.watchFaceConfigDataStore.edit { it[KEY_TOP_COMPLICATION] = c     } }
    suspend fun setBottomComplication(c: String)        { context.watchFaceConfigDataStore.edit { it[KEY_BOTTOM_COMPLICATION] = c  } }
    suspend fun setBioReactiveArc(enabled: Boolean)     { context.watchFaceConfigDataStore.edit { it[KEY_BIO_REACTIVE_ARC] = enabled } }
    suspend fun setHrvBand(enabled: Boolean)            { context.watchFaceConfigDataStore.edit { it[KEY_HRV_BAND]         = enabled } }
    suspend fun setCircadianSector(enabled: Boolean)    { context.watchFaceConfigDataStore.edit { it[KEY_CIRCADIAN_SECTOR] = enabled } }
    suspend fun setThermalStrip(enabled: Boolean)       { context.watchFaceConfigDataStore.edit { it[KEY_THERMAL_STRIP]    = enabled } }
    suspend fun setCoPilotTicker(enabled: Boolean)      { context.watchFaceConfigDataStore.edit { it[KEY_COPILOT_TICKER]   = enabled } }
    suspend fun setUvZoneArc(enabled: Boolean)          { context.watchFaceConfigDataStore.edit { it[KEY_UV_ZONE_ARC]      = enabled } }
    suspend fun setRecoveryScore(enabled: Boolean)      { context.watchFaceConfigDataStore.edit { it[KEY_RECOVERY_SCORE]   = enabled } }
    suspend fun setFocusCountdown(enabled: Boolean)     { context.watchFaceConfigDataStore.edit { it[KEY_FOCUS_COUNTDOWN]  = enabled } }
}
