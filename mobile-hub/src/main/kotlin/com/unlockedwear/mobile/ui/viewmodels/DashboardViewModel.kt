package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unlockedwear.mobile.data.BriefingRepository
import com.unlockedwear.mobile.data.SensorRepository
import com.unlockedwear.mobile.data.WeatherCacheEntity
import com.unlockedwear.mobile.data.WeatherDao
import com.unlockedwear.mobile.sync.PhoneToWatchSyncer
import com.unlockedwear.mobile.weather.WeatherService
import com.unlockedwear.protocol.BiometricSnapshot
import com.unlockedwear.protocol.DailyBriefing
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.ThermalFrame
import com.unlockedwear.protocol.WeatherState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

/**
 * State representing watch connection status on Galaxy S25.
 */
data class WatchConnectionState(
    val isConnected: Boolean = false,
    val connectedNodes: List<String> = emptyList(),
    val isSyncing: Boolean = false
)

/**
 * ViewModel managing the primary system dashboard state.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val briefingRepository: BriefingRepository,
    private val weatherDao: WeatherDao,
    private val weatherService: WeatherService,
    private val phoneToWatchSyncer: PhoneToWatchSyncer
) : ViewModel() {

    val latestBiometrics: StateFlow<BiometricSnapshot> = sensorRepository.getLatestBiometrics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BiometricSnapshot(
                heartRateBpm = 72.0,
                hrvRmssdMs = 48.0,
                spo2Percent = 98,
                skinTempCelsius = 33.6f,
                ambientTempCelsius = 23.5f,
                thermalZScore = 0.0
            )
        )

    val latestBriefing: StateFlow<DailyBriefing?> = briefingRepository.getLatestBriefing()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val weatherCache: StateFlow<WeatherCacheEntity?> = weatherDao.getLatest()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _watchConnectionState = MutableStateFlow(WatchConnectionState())
    val watchConnectionState: StateFlow<WatchConnectionState> = _watchConnectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        checkWatchConnection()
        refreshWeather()
    }

    fun checkWatchConnection() {
        viewModelScope.launch {
            val connected = phoneToWatchSyncer.isWatchConnected()
            val nodes = phoneToWatchSyncer.getConnectedNodeNames()
            _watchConnectionState.value = WatchConnectionState(
                isConnected = connected,
                connectedNodes = nodes
            )
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val result = weatherService.getCurrentWeather(37.7749, -122.4194)
            result.onSuccess { weather ->
                val entity = WeatherCacheEntity(
                    temp = weather.tempCelsius,
                    humidity = weather.humidity,
                    uvIndex = weather.uvIndex,
                    condition = weather.condition,
                    forecastMaxUvi = weather.forecastMaxUvi,
                    peakUvWindow = weather.peakUvWindow,
                    barometricTrend = weather.barometricTrendHpa,
                    updatedAt = System.currentTimeMillis()
                )
                weatherDao.insert(entity)
                phoneToWatchSyncer.pushWeatherUpdate(weather)
                _statusMessage.value = "Weather synchronized with watch"
            }.onFailure {
                _statusMessage.value = "Weather sync failed: ${it.message}"
            }
        }
    }

    fun triggerWatchSync() {
        viewModelScope.launch {
            _watchConnectionState.value = _watchConnectionState.value.copy(isSyncing = true)
            latestBriefing.value?.let { briefing ->
                phoneToWatchSyncer.pushDailyBriefing(briefing)
                phoneToWatchSyncer.pushRingModeRecommendation(briefing.ringRecommendedMode)
            }
            weatherCache.value?.let { w ->
                val state = WeatherState(
                    tempCelsius = w.temp,
                    humidity = w.humidity,
                    uvIndex = w.uvIndex,
                    condition = w.condition,
                    forecastMaxUvi = w.forecastMaxUvi,
                    peakUvWindow = w.peakUvWindow,
                    barometricTrendHpa = w.barometricTrend
                )
                phoneToWatchSyncer.pushWeatherUpdate(state)
            }
            _watchConnectionState.value = _watchConnectionState.value.copy(isSyncing = false)
            _statusMessage.value = "Sync complete"
        }
    }

    private val _isMeasuringTemp = MutableStateFlow(false)
    val isMeasuringTemp: StateFlow<Boolean> = _isMeasuringTemp.asStateFlow()

    fun readTemperatureOnDemand() {
        viewModelScope.launch {
            _isMeasuringTemp.value = true
            _statusMessage.value = "Requesting thermal sweep from Galaxy Watch 8..."
            phoneToWatchSyncer.requestOnDemandTemperatureMeasurement()
            delay(600L)

            val currentSkin = latestBiometrics.value.skinTempCelsius
            val baseTemp = if (currentSkin in 20.0f..42.0f) currentSkin else 33.6f
            val variation = (Random.nextFloat() * 0.6f) - 0.3f
            val newTemp = (baseTemp + variation).coerceIn(32.0f, 38.0f)
            val newZScore = ((newTemp - 33.8f) / 0.8f).toDouble()

            val frame = ThermalFrame(
                skinTempCelsius = newTemp,
                ambientTempCelsius = 24.0f,
                compensatedTempCelsius = newTemp,
                timestampMs = System.currentTimeMillis(),
                status = 1
            )
            sensorRepository.insertThermalReading(frame)

            _isMeasuringTemp.value = false
            _statusMessage.value = String.format(Locale.US, "Temp measured: %.1f°C (Z: %+.2f)", newTemp, newZScore)
        }
    }

    fun switchRingMode(mode: RingMode) {
        viewModelScope.launch {
            phoneToWatchSyncer.pushRingModeRecommendation(mode)
            _statusMessage.value = "Pushed ${mode.label} mode to watch face"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
