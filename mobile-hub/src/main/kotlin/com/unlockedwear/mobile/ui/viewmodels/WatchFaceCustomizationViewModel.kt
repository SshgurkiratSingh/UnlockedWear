package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unlockedwear.mobile.data.AppPreferences
import com.unlockedwear.mobile.sync.PhoneToWatchSyncer
import com.unlockedwear.protocol.RingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchFaceUiState(
    val ringMode: RingMode = RingMode.HEALTH,
    val accentColorHex: String = "#00B4D8",
    val topComplication: String = "Heart Rate (BPM)",
    val bottomComplication: String = "UV Index",
    val dialStyle: String = "Digital Modern",
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean? = null,          // null = not attempted, true/false = last result
    val statusMessage: String? = null
)

/**
 * ViewModel backing [WatchFaceCustomizationScreen].
 *
 * Responsibilities:
 * - Loads saved watch face config from [AppPreferences] on initialisation.
 * - Persists every user change to [AppPreferences] so settings survive app restarts.
 * - Pushes the full config to the connected Galaxy Watch via [PhoneToWatchSyncer.pushWatchFaceConfig].
 */
@HiltViewModel
class WatchFaceCustomizationViewModel @Inject constructor(
    private val phoneToWatchSyncer: PhoneToWatchSyncer,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadSavedState())
    val uiState: StateFlow<WatchFaceUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private fun loadSavedState(): WatchFaceUiState {
        return WatchFaceUiState(
            ringMode         = appPreferences.getWatchFaceRingMode(),
            accentColorHex   = appPreferences.getWatchFaceAccentColor(),
            topComplication  = appPreferences.getWatchFaceTopComplication(),
            bottomComplication = appPreferences.getWatchFaceBottomComplication(),
            dialStyle        = appPreferences.getWatchFaceDialStyle()
        )
    }

    // -------------------------------------------------------------------------
    // User interactions – each setter immediately persists to AppPreferences
    // -------------------------------------------------------------------------

    fun setRingMode(mode: RingMode) {
        _uiState.value = _uiState.value.copy(ringMode = mode)
        appPreferences.setWatchFaceRingMode(mode)
    }

    fun setAccentColor(colorHex: String) {
        _uiState.value = _uiState.value.copy(accentColorHex = colorHex)
        appPreferences.setWatchFaceAccentColor(colorHex)
    }

    fun setTopComplication(complication: String) {
        _uiState.value = _uiState.value.copy(topComplication = complication)
        appPreferences.setWatchFaceTopComplication(complication)
    }

    fun setBottomComplication(complication: String) {
        _uiState.value = _uiState.value.copy(bottomComplication = complication)
        appPreferences.setWatchFaceBottomComplication(complication)
    }

    fun setDialStyle(style: String) {
        _uiState.value = _uiState.value.copy(dialStyle = style)
        appPreferences.setWatchFaceDialStyle(style)
    }

    // -------------------------------------------------------------------------
    // Sync to watch
    // -------------------------------------------------------------------------

    /**
     * Pushes the current configuration to the connected Galaxy Watch via the
     * Wearable Data Layer API, then updates [uiState] with the outcome.
     */
    fun applyCustomizationToWatch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                statusMessage = "Applying to Galaxy Watch 8..."
            )
            val state = _uiState.value
            val success = phoneToWatchSyncer.pushWatchFaceConfig(
                ringMode           = state.ringMode,
                accentColorHex     = state.accentColorHex,
                topComplication    = state.topComplication,
                bottomComplication = state.bottomComplication,
                dialStyle          = state.dialStyle
            )
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncSuccess = success,
                statusMessage = if (success) {
                    "✓ Watch Face updated on Galaxy Watch 8!"
                } else {
                    "✗ Sync failed — is the watch connected?"
                }
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null, syncSuccess = null)
    }
}
