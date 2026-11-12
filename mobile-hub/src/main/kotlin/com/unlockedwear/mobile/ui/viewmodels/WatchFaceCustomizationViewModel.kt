package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unlockedwear.mobile.data.AppPreferences
import com.unlockedwear.mobile.sync.PhoneToWatchSyncer
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.WatchFaceFeatureFlags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchFaceUiState(
    // ── Base config ───────────────────────────────────────────────────────
    val ringMode: RingMode = RingMode.HEALTH,
    val accentColorHex: String = "#3B82F6",
    val topComplication: String = "Heart Rate (BPM)",
    val bottomComplication: String = "UV Index",
    val dialStyle: String = "Digital Modern",
    // ── Feature flags ─────────────────────────────────────────────────────
    val bioReactiveArc: Boolean = true,
    val hrvBand: Boolean = true,
    val circadianSector: Boolean = false,
    val thermalStrip: Boolean = true,
    val coPilotTicker: Boolean = true,
    val uvZoneArc: Boolean = false,
    val recoveryScore: Boolean = false,
    val focusCountdown: Boolean = false,
    // ── Sync state ────────────────────────────────────────────────────────
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean? = null,
    val statusMessage: String? = null
) {
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

/**
 * ViewModel for the Watch Face Customization screen.
 *
 * Manages both base config (accent colour, dial style, complication slots, ring mode)
 * and extended feature flags (bio-reactive arc, HRV band, circadian sector, thermal strip,
 * Co-Pilot ticker, UV zone arc, recovery score, focus countdown).
 *
 * All changes are immediately persisted to [AppPreferences] and pushed to the watch
 * via [PhoneToWatchSyncer] when the user taps "Apply".
 */
@HiltViewModel
class WatchFaceCustomizationViewModel @Inject constructor(
    private val phoneToWatchSyncer: PhoneToWatchSyncer,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadSavedState())
    val uiState: StateFlow<WatchFaceUiState> = _uiState.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────

    private fun loadSavedState() = WatchFaceUiState(
        ringMode           = appPreferences.getWatchFaceRingMode(),
        accentColorHex     = appPreferences.getWatchFaceAccentColor(),
        topComplication    = appPreferences.getWatchFaceTopComplication(),
        bottomComplication = appPreferences.getWatchFaceBottomComplication(),
        dialStyle          = appPreferences.getWatchFaceDialStyle(),
        bioReactiveArc     = appPreferences.getWatchFaceBioReactiveArc(),
        hrvBand            = appPreferences.getWatchFaceHrvBand(),
        circadianSector    = appPreferences.getWatchFaceCircadianSector(),
        thermalStrip       = appPreferences.getWatchFaceThermalStrip(),
        coPilotTicker      = appPreferences.getWatchFaceCoPilotTicker(),
        uvZoneArc          = appPreferences.getWatchFaceUvZoneArc(),
        recoveryScore      = appPreferences.getWatchFaceRecoveryScore(),
        focusCountdown     = appPreferences.getWatchFaceFocusCountdown()
    )

    // ── Base config setters ───────────────────────────────────────────────

    fun setRingMode(mode: RingMode) {
        _uiState.value = _uiState.value.copy(ringMode = mode)
        appPreferences.setWatchFaceRingMode(mode)
    }

    fun setAccentColor(hex: String) {
        _uiState.value = _uiState.value.copy(accentColorHex = hex)
        appPreferences.setWatchFaceAccentColor(hex)
    }

    fun setTopComplication(c: String) {
        _uiState.value = _uiState.value.copy(topComplication = c)
        appPreferences.setWatchFaceTopComplication(c)
    }

    fun setBottomComplication(c: String) {
        _uiState.value = _uiState.value.copy(bottomComplication = c)
        appPreferences.setWatchFaceBottomComplication(c)
    }

    fun setDialStyle(style: String) {
        _uiState.value = _uiState.value.copy(dialStyle = style)
        appPreferences.setWatchFaceDialStyle(style)
    }

    // ── Feature flag setters ──────────────────────────────────────────────

    fun setBioReactiveArc(v: Boolean)  { _uiState.value = _uiState.value.copy(bioReactiveArc  = v); appPreferences.setWatchFaceBioReactiveArc(v)  }
    fun setHrvBand(v: Boolean)         { _uiState.value = _uiState.value.copy(hrvBand         = v); appPreferences.setWatchFaceHrvBand(v)         }
    fun setCircadianSector(v: Boolean) { _uiState.value = _uiState.value.copy(circadianSector = v); appPreferences.setWatchFaceCircadianSector(v) }
    fun setThermalStrip(v: Boolean)    { _uiState.value = _uiState.value.copy(thermalStrip    = v); appPreferences.setWatchFaceThermalStrip(v)    }
    fun setCoPilotTicker(v: Boolean)   { _uiState.value = _uiState.value.copy(coPilotTicker   = v); appPreferences.setWatchFaceCoPilotTicker(v)   }
    fun setUvZoneArc(v: Boolean)       { _uiState.value = _uiState.value.copy(uvZoneArc       = v); appPreferences.setWatchFaceUvZoneArc(v)       }
    fun setRecoveryScore(v: Boolean)   { _uiState.value = _uiState.value.copy(recoveryScore   = v); appPreferences.setWatchFaceRecoveryScore(v)   }
    fun setFocusCountdown(v: Boolean)  { _uiState.value = _uiState.value.copy(focusCountdown  = v); appPreferences.setWatchFaceFocusCountdown(v)  }

    // ── Sync ──────────────────────────────────────────────────────────────

    /**
     * Pushes both base config AND feature flags to the watch in parallel.
     * Shows a coloured status banner on completion.
     */
    fun applyCustomizationToWatch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, statusMessage = "Applying to Galaxy Watch 8…")
            val s = _uiState.value
            val baseOk = phoneToWatchSyncer.pushWatchFaceConfig(
                ringMode           = s.ringMode,
                accentColorHex     = s.accentColorHex,
                topComplication    = s.topComplication,
                bottomComplication = s.bottomComplication,
                dialStyle          = s.dialStyle
            )
            val flagsOk = phoneToWatchSyncer.pushWatchFaceFeatureFlags(s.toFeatureFlags())
            val success = baseOk && flagsOk
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncSuccess = success,
                statusMessage = if (success) "✓ Watch Face updated on Galaxy Watch 8!"
                                else         "✗ Sync failed — is the watch connected?"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null, syncSuccess = null)
    }
}
