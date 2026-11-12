package com.unlockedwear.mobile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unlockedwear.mobile.ai.AiRequest
import com.unlockedwear.mobile.ai.AiResponse
import com.unlockedwear.mobile.ai.IntelligenceRouter
import com.unlockedwear.mobile.data.BriefingRepository
import com.unlockedwear.mobile.data.SensorRepository
import com.unlockedwear.mobile.data.WeatherDao
import com.unlockedwear.mobile.sync.PhoneToWatchSyncer
import com.unlockedwear.protocol.CognitiveLoad
import com.unlockedwear.protocol.DailyBriefing
import com.unlockedwear.protocol.IntelligenceRequest
import com.unlockedwear.protocol.IntelligenceRequestType
import com.unlockedwear.protocol.TaskItem
import com.unlockedwear.protocol.WeatherState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Message entry within the AI assistant conversation stream.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val engine: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Active operational state of the dual AI engine architecture.
 */
data class AiEngineStatus(
    val deepSeekStatus: String = "Ready (ORBIT X Analytical)",
    val geminiStatus: String = "Ready (1.5 Flash Interactive)",
    val lastActiveEngine: String = "Idle"
)

/**
 * ViewModel managing AI interactions, conversational assistant state, and manual briefing triggers.
 */
@HiltViewModel
class AiViewModel @Inject constructor(
    private val intelligenceRouter: IntelligenceRouter,
    private val sensorRepository: SensorRepository,
    private val briefingRepository: BriefingRepository,
    private val weatherDao: WeatherDao,
    private val phoneToWatchSyncer: PhoneToWatchSyncer
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "UnlockedWear AI Co-Pilot online. DeepSeek-Chat handles analytical telemetry synthesis & briefings; Gemini 1.5 Flash powers real-time conversation.",
                isUser = false,
                engine = "System"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _engineStatus = MutableStateFlow(AiEngineStatus())
    val engineStatus: StateFlow<AiEngineStatus> = _engineStatus.asStateFlow()

    private val _latestManualBriefing = MutableStateFlow<DailyBriefing?>(null)
    val latestManualBriefing: StateFlow<DailyBriefing?> = _latestManualBriefing.asStateFlow()

    /**
     * Dispatches an interactive query to Gemini.
     */
    fun sendInteractiveQuery(query: String) {
        if (query.isBlank()) return

        val userMessage = ChatMessage(text = query, isUser = true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _engineStatus.value = _engineStatus.value.copy(lastActiveEngine = "Gemini 1.5 Flash")

        viewModelScope.launch {
            val response = intelligenceRouter.routeRequest(AiRequest.GeminiInteractive(query))
            _isLoading.value = false

            when (response) {
                is AiResponse.Text -> {
                    val assistantMsg = ChatMessage(
                        text = response.text,
                        isUser = false,
                        engine = response.engine
                    )
                    _messages.value = _messages.value + assistantMsg
                }
                is AiResponse.Error -> {
                    val errorMsg = ChatMessage(
                        text = "Error: ${response.message}",
                        isUser = false,
                        engine = "System"
                    )
                    _messages.value = _messages.value + errorMsg
                }
                is AiResponse.Briefing -> {
                    val briefMsg = ChatMessage(
                        text = "Briefing: ${response.briefing.coPilotHeadline}",
                        isUser = false,
                        engine = "DeepSeek"
                    )
                    _messages.value = _messages.value + briefMsg
                }
            }
        }
    }

    /**
     * Triggers an on-demand DeepSeek analytical reasoning query with real-time biometric grounding.
     */
    fun requestDeepSeekAnalysis(query: String) {
        if (query.isBlank()) return

        val userMessage = ChatMessage(text = "[Analytical Query] $query", isUser = true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _engineStatus.value = _engineStatus.value.copy(lastActiveEngine = "DeepSeek-Chat")

        viewModelScope.launch {
            val context = sensorRepository.getLatestBiometrics().first()
            val response = intelligenceRouter.routeRequest(AiRequest.DeepSeekAnalysis(query, context))
            _isLoading.value = false

            when (response) {
                is AiResponse.Text -> {
                    val assistantMsg = ChatMessage(
                        text = response.text,
                        isUser = false,
                        engine = response.engine
                    )
                    _messages.value = _messages.value + assistantMsg
                }
                is AiResponse.Error -> {
                    val errorMsg = ChatMessage(
                        text = "Analysis failed: ${response.message}",
                        isUser = false,
                        engine = "System"
                    )
                    _messages.value = _messages.value + errorMsg
                }
                is AiResponse.Briefing -> {}
            }
        }
    }

    /**
     * Manually triggers full morning briefing synthesis via DeepSeek and pushes to watch.
     */
    fun triggerManualBriefing() {
        _isLoading.value = true
        _engineStatus.value = _engineStatus.value.copy(lastActiveEngine = "DeepSeek-Chat (Briefing)")

        viewModelScope.launch {
            val biometrics = sensorRepository.getOvernightBiometricSummary()
            val cachedWeather = weatherDao.getLatestDirect()
            val weather = if (cachedWeather != null) {
                WeatherState(
                    tempCelsius = cachedWeather.temp,
                    humidity = cachedWeather.humidity,
                    uvIndex = cachedWeather.uvIndex,
                    condition = cachedWeather.condition,
                    forecastMaxUvi = cachedWeather.forecastMaxUvi,
                    peakUvWindow = cachedWeather.peakUvWindow,
                    barometricTrendHpa = cachedWeather.barometricTrend
                )
            } else {
                WeatherState(
                    tempCelsius = 21.0f,
                    humidity = 55.0f,
                    uvIndex = 4.0f,
                    condition = "Clear",
                    forecastMaxUvi = 6.5f,
                    peakUvWindow = "12:00 PM - 2:00 PM",
                    barometricTrendHpa = 0.0f
                )
            }

            val request = IntelligenceRequest(
                biometricTelemetry = biometrics,
                environmentalContext = weather,
                workloadQueue = listOf(
                    TaskItem(id = "task-opt", title = "Focus Optimization", deadlineHrs = 4.0, cognitiveLoad = CognitiveLoad.HIGH)
                ),
                requestType = IntelligenceRequestType.MORNING_BRIEFING
            )

            val response = intelligenceRouter.routeRequest(AiRequest.DeepSeekBriefing(request))
            _isLoading.value = false

            when (response) {
                is AiResponse.Briefing -> {
                    _latestManualBriefing.value = response.briefing
                    briefingRepository.saveBriefing(response.briefing)
                    phoneToWatchSyncer.pushDailyBriefing(response.briefing)
                    phoneToWatchSyncer.pushRingModeRecommendation(response.briefing.ringRecommendedMode)

                    val briefMsg = ChatMessage(
                        text = "Daily Briefing Generated:\nHeadline: ${response.briefing.coPilotHeadline}\nRecommended Mode: ${response.briefing.ringRecommendedMode.label}\nFocus Blocks: ${response.briefing.recommendedFocusBlocksMin} min\nRecovery: ${response.briefing.thermalRecoveryAssessment}",
                        isUser = false,
                        engine = "DeepSeek ORBIT X"
                    )
                    _messages.value = _messages.value + briefMsg
                }
                is AiResponse.Error -> {
                    val errorMsg = ChatMessage(
                        text = "Briefing synthesis failed: ${response.message}",
                        isUser = false,
                        engine = "System"
                    )
                    _messages.value = _messages.value + errorMsg
                }
                is AiResponse.Text -> {}
            }
        }
    }
}
