package com.unlockedwear.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.RingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ringPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "unlockedwear_ring_state")

/**
 * Manages the active Mission Ring mode and its telemetry gauge properties using Jetpack DataStore.
 *
 * Supported Ring Modes:
 * - [RingMode.UV]: Ambient ultraviolet index and solar exposure monitoring
 * - [RingMode.HEALTH]: Cardio-vascular load, HRV RMSSD, and SpO2 status
 * - [RingMode.FOCUS]: Deep work blocks, Pomodoro progress, and distraction prevention
 * - [RingMode.POWER]: Watch energy optimization, thermal throttling, and battery status
 */
@Singleton
class RingStateDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val KEY_RING_MODE = stringPreferencesKey("ring_mode")
        val KEY_CURRENT_VAL = floatPreferencesKey("current_val")
        val KEY_MIN_VAL = floatPreferencesKey("min_val")
        val KEY_MAX_VAL = floatPreferencesKey("max_val")
        val KEY_LABEL = stringPreferencesKey("label")
        val KEY_FORMATTED_METRIC = stringPreferencesKey("formatted_metric")
    }

    /**
     * Observable flow of the active [RingState].
     */
    val ringStateFlow: Flow<RingState> = context.ringPreferencesDataStore.data.map { preferences ->
        val modeString = preferences[KEY_RING_MODE] ?: RingMode.UV.name
        val mode = try {
            RingMode.valueOf(modeString)
        } catch (e: Exception) {
            RingMode.UV
        }

        val currentValue = preferences[KEY_CURRENT_VAL] ?: getDefaultValForMode(mode)
        val minValue = preferences[KEY_MIN_VAL] ?: mode.minValue
        val maxValue = preferences[KEY_MAX_VAL] ?: mode.maxValue
        val label = preferences[KEY_LABEL] ?: mode.label
        val formattedMetric = preferences[KEY_FORMATTED_METRIC] ?: getDefaultFormattedMetric(mode, currentValue)

        RingState(
            mode = mode,
            currentValue = currentValue,
            minValue = minValue,
            maxValue = maxValue,
            label = label,
            formattedMetric = formattedMetric
        )
    }

    /**
     * Reads the current [RingState] snapshot.
     */
    suspend fun getCurrentRingState(): RingState = ringStateFlow.first()

    /**
     * Updates the active [RingMode] and resets gauge bounds to mode defaults.
     */
    suspend fun setRingMode(mode: RingMode) {
        val defaultVal = getDefaultValForMode(mode)
        context.ringPreferencesDataStore.edit { preferences ->
            preferences[KEY_RING_MODE] = mode.name
            preferences[KEY_CURRENT_VAL] = defaultVal
            preferences[KEY_MIN_VAL] = mode.minValue
            preferences[KEY_MAX_VAL] = mode.maxValue
            preferences[KEY_LABEL] = mode.label
            preferences[KEY_FORMATTED_METRIC] = getDefaultFormattedMetric(mode, defaultVal)
        }
    }

    /**
     * Cycles to the next available [RingMode].
     */
    suspend fun cycleNextMode(): RingMode {
        val current = getCurrentRingState().mode
        val values = RingMode.values()
        val nextIndex = (current.ordinal + 1) % values.size
        val nextMode = values[nextIndex]
        setRingMode(nextMode)
        return nextMode
    }

    /**
     * Updates gauge readings for the current active mode.
     */
    suspend fun updateRingState(state: RingState) {
        context.ringPreferencesDataStore.edit { preferences ->
            preferences[KEY_RING_MODE] = state.mode.name
            preferences[KEY_CURRENT_VAL] = state.currentValue
            preferences[KEY_MIN_VAL] = state.minValue
            preferences[KEY_MAX_VAL] = state.maxValue
            preferences[KEY_LABEL] = state.label
            preferences[KEY_FORMATTED_METRIC] = state.formattedMetric
        }
    }

    private fun getDefaultValForMode(mode: RingMode): Float = when (mode) {
        RingMode.UV -> 3.5f
        RingMode.HEALTH -> 72f
        RingMode.FOCUS -> 25f
        RingMode.POWER -> 88f
    }

    private fun getDefaultFormattedMetric(mode: RingMode, value: Float): String = when (mode) {
        RingMode.UV -> "${String.format("%.1f", value)} UVI"
        RingMode.HEALTH -> "${value.toInt()} BPM"
        RingMode.FOCUS -> "${value.toInt()} MIN"
        RingMode.POWER -> "${value.toInt()}%"
    }
}
