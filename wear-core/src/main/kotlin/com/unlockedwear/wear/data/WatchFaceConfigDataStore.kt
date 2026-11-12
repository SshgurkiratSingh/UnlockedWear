package com.unlockedwear.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unlockedwear.protocol.RingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed persistence for watch face visual customization settings pushed from the
 * Galaxy S25 companion app via the Wearable Data Layer API ([DataLayerPaths.CONFIG_SYNC]).
 *
 * Stored fields:
 * - [KEY_ACCENT_COLOR]        — primary HEX accent colour (e.g. "#00B4D8")
 * - [KEY_TOP_COMPLICATION]    — label for top complication slot (e.g. "Heart Rate (BPM)")
 * - [KEY_BOTTOM_COMPLICATION] — label for bottom complication slot (e.g. "UV Index")
 * - [KEY_DIAL_STYLE]          — dial layout identifier ("Digital Modern" / "Minimal Hybrid" / "Telemetry Gauge")
 * - [KEY_RING_MODE]           — bezel ring mode override sent from phone (e.g. "HEALTH")
 *
 * These settings are consumed by [com.unlockedwear.wear.ui.screens.HomeScreen],
 * [com.unlockedwear.wear.complications.MissionRingComplicationService], and the
 * watch-face complication data-source services.
 */
private val Context.watchFaceConfigDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "unlockedwear_watchface_config")

data class WatchFaceConfig(
    val accentColorHex: String = "#00D4FF",
    val topComplication: String = "Heart Rate (BPM)",
    val bottomComplication: String = "UV Index",
    val dialStyle: String = "Digital Modern",
    val ringMode: RingMode = RingMode.HEALTH
)

@Singleton
class WatchFaceConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_ACCENT_COLOR        = stringPreferencesKey("wf_accent_color")
        val KEY_TOP_COMPLICATION    = stringPreferencesKey("wf_top_complication")
        val KEY_BOTTOM_COMPLICATION = stringPreferencesKey("wf_bottom_complication")
        val KEY_DIAL_STYLE          = stringPreferencesKey("wf_dial_style")
        val KEY_RING_MODE           = stringPreferencesKey("wf_ring_mode")
    }

    /** Reactive stream of the current [WatchFaceConfig]. */
    val configFlow: Flow<WatchFaceConfig> =
        context.watchFaceConfigDataStore.data.map { prefs ->
            val ringModeStr = prefs[KEY_RING_MODE] ?: RingMode.HEALTH.name
            val ringMode = try { RingMode.valueOf(ringModeStr) } catch (_: Exception) { RingMode.HEALTH }
            WatchFaceConfig(
                accentColorHex  = prefs[KEY_ACCENT_COLOR]        ?: "#00D4FF",
                topComplication = prefs[KEY_TOP_COMPLICATION]     ?: "Heart Rate (BPM)",
                bottomComplication = prefs[KEY_BOTTOM_COMPLICATION] ?: "UV Index",
                dialStyle       = prefs[KEY_DIAL_STYLE]           ?: "Digital Modern",
                ringMode        = ringMode
            )
        }

    /** Returns a one-shot snapshot of the current config. */
    suspend fun getConfig(): WatchFaceConfig = configFlow.first()

    /**
     * Persists a full watch face configuration pushed from the companion phone.
     *
     * @param accentColorHex  New primary accent colour in `#RRGGBB` format.
     * @param topComplication  Top slot complication label.
     * @param bottomComplication Bottom slot complication label.
     * @param dialStyle  Dial layout identifier.
     * @param ringMode  Ring gauge mode override.
     */
    suspend fun applyConfig(
        accentColorHex: String,
        topComplication: String,
        bottomComplication: String,
        dialStyle: String,
        ringMode: RingMode
    ) {
        context.watchFaceConfigDataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR]        = accentColorHex
            prefs[KEY_TOP_COMPLICATION]    = topComplication
            prefs[KEY_BOTTOM_COMPLICATION] = bottomComplication
            prefs[KEY_DIAL_STYLE]          = dialStyle
            prefs[KEY_RING_MODE]           = ringMode.name
        }
    }

    /** Updates only the accent colour (e.g. during local quick-swap). */
    suspend fun setAccentColor(hex: String) {
        context.watchFaceConfigDataStore.edit { it[KEY_ACCENT_COLOR] = hex }
    }

    /** Updates only the dial style. */
    suspend fun setDialStyle(style: String) {
        context.watchFaceConfigDataStore.edit { it[KEY_DIAL_STYLE] = style }
    }

    /** Updates only the top complication slot label. */
    suspend fun setTopComplication(complication: String) {
        context.watchFaceConfigDataStore.edit { it[KEY_TOP_COMPLICATION] = complication }
    }

    /** Updates only the bottom complication slot label. */
    suspend fun setBottomComplication(complication: String) {
        context.watchFaceConfigDataStore.edit { it[KEY_BOTTOM_COMPLICATION] = complication }
    }
}
