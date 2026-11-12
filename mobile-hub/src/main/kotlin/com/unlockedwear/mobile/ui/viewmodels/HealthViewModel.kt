package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unlockedwear.mobile.data.BiometricReadingEntity
import com.unlockedwear.mobile.data.SensorRepository
import com.unlockedwear.mobile.data.ThermalReadingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Filter window for historical telemetry queries.
 */
enum class TimeRange(val hours: Int, val label: String) {
    SIX_HOURS(6, "6h"),
    TWENTY_FOUR_HOURS(24, "24h"),
    SEVEN_DAYS(168, "7d")
}

/**
 * Chart data model holding normalized plotting points and metrics.
 */
data class MetricPoint(
    val timestampMs: Long,
    val value: Float
)

/**
 * Aggregated health telemetry state prepared for chart rendering and anomaly analysis.
 */
data class HealthUiState(
    val timeRange: TimeRange = TimeRange.TWENTY_FOUR_HOURS,
    val heartRatePoints: List<MetricPoint> = emptyList(),
    val hrvPoints: List<MetricPoint> = emptyList(),
    val skinTempPoints: List<MetricPoint> = emptyList(),
    val spo2Points: List<MetricPoint> = emptyList(),
    val thermalBaselineMean: Float = 33.8f,
    val thermalZScoreAnomalies: List<MetricPoint> = emptyList(),
    val minHeartRate: Float = 0f,
    val maxHeartRate: Float = 0f,
    val avgHeartRate: Float = 0f,
    val avgHrv: Float = 0f,
    val avgSkinTemp: Float = 0f
)

/**
 * ViewModel processing time-series physiological data into graphical timelines.
 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(TimeRange.TWENTY_FOUR_HOURS)
    val selectedRange: StateFlow<TimeRange> = _selectedRange.asStateFlow()

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        observeHistoricalData()
    }

    fun setTimeRange(range: TimeRange) {
        _selectedRange.value = range
        observeHistoricalData()
    }

    private fun observeHistoricalData() {
        val hours = _selectedRange.value.hours
        viewModelScope.launch {
            sensorRepository.getBiometricHistory(hours).collect { bioList ->
                sensorRepository.getThermalHistory(hours).collect { thermList ->
                    processData(bioList, thermList)
                }
            }
        }
    }

    private fun processData(
        bioList: List<BiometricReadingEntity>,
        thermList: List<ThermalReadingEntity>
    ) {
        val hrPoints = bioList.map { MetricPoint(it.timestampMs, it.heartRate.toFloat()) }
        val hrvPoints = bioList.map { MetricPoint(it.timestampMs, it.hrvRmssd.toFloat()) }
        val spo2Points = bioList.map { MetricPoint(it.timestampMs, it.spo2.toFloat()) }
        val skinTempPoints = thermList.map { MetricPoint(it.timestampMs, it.skinTemp) }

        val anomalies = thermList
            .filter { kotlin.math.abs(it.thermalZScore) >= 1.5 }
            .map { MetricPoint(it.timestampMs, it.skinTemp) }

        val hrValues = hrPoints.map { it.value }
        val minHr = hrValues.minOrNull() ?: 0f
        val maxHr = hrValues.maxOrNull() ?: 0f
        val avgHr = if (hrValues.isNotEmpty()) hrValues.average().toFloat() else 0f

        val hrvValues = hrvPoints.map { it.value }
        val avgHrv = if (hrvValues.isNotEmpty()) hrvValues.average().toFloat() else 0f

        val skinTempValues = skinTempPoints.map { it.value }
        val avgSkinTemp = if (skinTempValues.isNotEmpty()) skinTempValues.average().toFloat() else 0f

        _uiState.value = HealthUiState(
            timeRange = _selectedRange.value,
            heartRatePoints = hrPoints,
            hrvPoints = hrvPoints,
            skinTempPoints = skinTempPoints,
            spo2Points = spo2Points,
            thermalBaselineMean = if (avgSkinTemp > 0f) avgSkinTemp else 33.8f,
            thermalZScoreAnomalies = anomalies,
            minHeartRate = minHr,
            maxHeartRate = maxHr,
            avgHeartRate = avgHr,
            avgHrv = avgHrv,
            avgSkinTemp = avgSkinTemp
        )
    }
}
