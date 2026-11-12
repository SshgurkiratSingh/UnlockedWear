package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.unlockedwear.mobile.data.AppPreferences
import com.unlockedwear.protocol.RingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing app configurations, API keys, and synchronization preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val deepSeekApiKey: StateFlow<String> = appPreferences.deepSeekApiKey
    val geminiApiKey: StateFlow<String> = appPreferences.geminiApiKey
    val weatherApiKey: StateFlow<String> = appPreferences.weatherApiKey
    val preferredRingMode: StateFlow<RingMode> = appPreferences.preferredRingMode
    val briefingHour: StateFlow<Int> = appPreferences.briefingHour
    val briefingMinute: StateFlow<Int> = appPreferences.briefingMinute

    fun saveDeepSeekKey(key: String) {
        appPreferences.setDeepSeekApiKey(key)
    }

    fun saveGeminiKey(key: String) {
        appPreferences.setGeminiApiKey(key)
    }

    fun saveWeatherKey(key: String) {
        appPreferences.setWeatherApiKey(key)
    }

    fun setPreferredRingMode(mode: RingMode) {
        appPreferences.setPreferredRingMode(mode)
    }

    fun setBriefingTime(hour: Int, minute: Int) {
        appPreferences.setBriefingTime(hour, minute)
    }
}
