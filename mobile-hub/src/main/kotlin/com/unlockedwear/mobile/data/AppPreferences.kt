package com.unlockedwear.mobile.data

import android.content.Context
import android.content.SharedPreferences
import com.unlockedwear.protocol.RingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persisted user preferences and secure credentials for the UnlockedWear companion app.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _deepSeekApiKey = MutableStateFlow(getDeepSeekApiKey())
    val deepSeekApiKey: StateFlow<String> = _deepSeekApiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _weatherApiKey = MutableStateFlow(getWeatherApiKey())
    val weatherApiKey: StateFlow<String> = _weatherApiKey.asStateFlow()

    private val _preferredRingMode = MutableStateFlow(getPreferredRingMode())
    val preferredRingMode: StateFlow<RingMode> = _preferredRingMode.asStateFlow()

    private val _briefingHour = MutableStateFlow(getBriefingHour())
    val briefingHour: StateFlow<Int> = _briefingHour.asStateFlow()

    private val _briefingMinute = MutableStateFlow(getBriefingMinute())
    val briefingMinute: StateFlow<Int> = _briefingMinute.asStateFlow()

    fun getDeepSeekApiKey(): String = prefs.getString(KEY_DEEPSEEK_API_KEY, "") ?: ""
    fun setDeepSeekApiKey(key: String) {
        prefs.edit().putString(KEY_DEEPSEEK_API_KEY, key.trim()).apply()
        _deepSeekApiKey.value = key.trim()
    }

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
        _geminiApiKey.value = key.trim()
    }

    fun getWeatherApiKey(): String = prefs.getString(KEY_WEATHER_API_KEY, "") ?: ""
    fun setWeatherApiKey(key: String) {
        prefs.edit().putString(KEY_WEATHER_API_KEY, key.trim()).apply()
        _weatherApiKey.value = key.trim()
    }

    fun getPreferredRingMode(): RingMode {
        val name = prefs.getString(KEY_PREFERRED_RING_MODE, RingMode.HEALTH.name)
        return try {
            RingMode.valueOf(name ?: RingMode.HEALTH.name)
        } catch (_: Exception) {
            RingMode.HEALTH
        }
    }

    fun setPreferredRingMode(mode: RingMode) {
        prefs.edit().putString(KEY_PREFERRED_RING_MODE, mode.name).apply()
        _preferredRingMode.value = mode
    }

    fun getBriefingHour(): Int = prefs.getInt(KEY_BRIEFING_HOUR, 7)
    fun getBriefingMinute(): Int = prefs.getInt(KEY_BRIEFING_MINUTE, 0)

    fun setBriefingTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_BRIEFING_HOUR, hour)
            .putInt(KEY_BRIEFING_MINUTE, minute)
            .apply()
        _briefingHour.value = hour
        _briefingMinute.value = minute
    }

    // -------------------------------------------------------------------------
    // Watch Face Customization Preferences
    // -------------------------------------------------------------------------

    fun getWatchFaceRingMode(): RingMode {
        val name = prefs.getString(KEY_WF_RING_MODE, RingMode.HEALTH.name)
        return try { RingMode.valueOf(name ?: RingMode.HEALTH.name) } catch (_: Exception) { RingMode.HEALTH }
    }

    fun setWatchFaceRingMode(mode: RingMode) {
        prefs.edit().putString(KEY_WF_RING_MODE, mode.name).apply()
    }

    fun getWatchFaceAccentColor(): String =
        prefs.getString(KEY_WF_ACCENT_COLOR, "#00B4D8") ?: "#00B4D8"

    fun setWatchFaceAccentColor(hex: String) {
        prefs.edit().putString(KEY_WF_ACCENT_COLOR, hex).apply()
    }

    fun getWatchFaceTopComplication(): String =
        prefs.getString(KEY_WF_TOP_COMPLICATION, "Heart Rate (BPM)") ?: "Heart Rate (BPM)"

    fun setWatchFaceTopComplication(complication: String) {
        prefs.edit().putString(KEY_WF_TOP_COMPLICATION, complication).apply()
    }

    fun getWatchFaceBottomComplication(): String =
        prefs.getString(KEY_WF_BOTTOM_COMPLICATION, "UV Index") ?: "UV Index"

    fun setWatchFaceBottomComplication(complication: String) {
        prefs.edit().putString(KEY_WF_BOTTOM_COMPLICATION, complication).apply()
    }

    fun getWatchFaceDialStyle(): String =
        prefs.getString(KEY_WF_DIAL_STYLE, "Digital Modern") ?: "Digital Modern"

    fun setWatchFaceDialStyle(style: String) {
        prefs.edit().putString(KEY_WF_DIAL_STYLE, style).apply()
    }

    companion object {
        private const val PREFS_NAME = "unlockedwear_prefs"
        private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_WEATHER_API_KEY = "weather_api_key"
        private const val KEY_PREFERRED_RING_MODE = "preferred_ring_mode"
        private const val KEY_BRIEFING_HOUR = "briefing_hour"
        private const val KEY_BRIEFING_MINUTE = "briefing_minute"

        // Watch Face Customization
        private const val KEY_WF_RING_MODE           = "wf_ring_mode"
        private const val KEY_WF_ACCENT_COLOR        = "wf_accent_color"
        private const val KEY_WF_TOP_COMPLICATION    = "wf_top_complication"
        private const val KEY_WF_BOTTOM_COMPLICATION = "wf_bottom_complication"
        private const val KEY_WF_DIAL_STYLE          = "wf_dial_style"
    }
}
