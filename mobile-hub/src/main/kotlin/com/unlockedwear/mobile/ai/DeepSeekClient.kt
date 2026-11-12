package com.unlockedwear.mobile.ai

import com.unlockedwear.mobile.data.AppPreferences
import com.unlockedwear.protocol.BiometricSnapshot
import com.unlockedwear.protocol.DailyBriefing
import com.unlockedwear.protocol.IntelligenceRequest
import com.unlockedwear.protocol.RingMode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ktor-based HTTP client for the DeepSeek API (https://api.deepseek.com).
 *
 * Configured as the ORBIT X analytical engine for synthesis of nocturnal physiological metrics,
 * thermal baseline deviations, and schedule optimization.
 */
@Singleton
class DeepSeekClient @Inject constructor(
    private val appPreferences: AppPreferences
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000L
            connectTimeoutMillis = 15_000L
            socketTimeoutMillis = 30_000L
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    /**
     * Synthesizes a comprehensive [DailyBriefing] from nocturnal biometrics, weather, and tasks.
     *
     * Utilizes DeepSeek's JSON-object response format mode to enforce strict data contracts.
     *
     * @param request The aggregated biometric, environmental, and workload telemetry payload.
     * @return [Result] enclosing the generated [DailyBriefing] or an error on failure.
     */
    suspend fun requestMorningBriefing(request: IntelligenceRequest): Result<DailyBriefing> {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("DeepSeek API key is not configured."))
        }

        val systemPrompt = """
            You are ORBIT X, the high-precision analytical intelligence engine for the UnlockedWear biometric ecosystem.
            Analyze the user's nocturnal telemetry, thermal recovery baseline (z-scores), ambient weather & UV status, and workload queue.
            Generate a concise, mission-grade daily briefing adhering STRICTLY to this JSON structure:
            {
              "ringRecommendedMode": "UV" | "HEALTH" | "FOCUS" | "POWER",
              "coPilotHeadline": "string (strictly under 38 characters)",
              "priorityTaskId": "string or null",
              "recommendedFocusBlocksMin": integer,
              "thermalRecoveryAssessment": "string (concise clinical-grade recovery assessment)",
              "alerts": ["actionable alert 1", "actionable alert 2"]
            }
            Important:
            - coPilotHeadline MUST be 38 characters or fewer for curved watch display.
            - ringRecommendedMode MUST be one of: UV, HEALTH, FOCUS, POWER.
        """.trimIndent()

        val userMessage = """
            Biometric Telemetry:
            - Heart Rate: ${request.biometricTelemetry.heartRateBpm} bpm
            - HRV RMSSD: ${request.biometricTelemetry.hrvRmssdMs} ms
            - SpO2: ${request.biometricTelemetry.spo2Percent}%
            - Skin Temp: ${request.biometricTelemetry.skinTempCelsius} °C
            - Ambient Temp: ${request.biometricTelemetry.ambientTempCelsius} °C
            - Thermal Z-Score: ${request.biometricTelemetry.thermalZScore}

            Environmental Status:
            - Temperature: ${request.environmentalContext.tempCelsius} °C
            - Humidity: ${request.environmentalContext.humidity}%
            - UV Index: ${request.environmentalContext.uvIndex} (Max Forecast: ${request.environmentalContext.forecastMaxUvi})
            - Condition: ${request.environmentalContext.condition}
            - Barometric Trend: ${request.environmentalContext.barometricTrendHpa} hPa

            Pending Tasks:
            ${request.workloadQueue.joinToString("\n") { "- [${it.id}] ${it.title} (Deadline: ${it.deadlineHrs}h, Load: ${it.cognitiveLoad.name})" }}
        """.trimIndent()

        val requestPayload = DeepSeekChatRequest(
            model = MODEL_NAME,
            messages = listOf(
                DeepSeekMessage(role = "system", content = systemPrompt),
                DeepSeekMessage(role = "user", content = userMessage)
            ),
            responseFormat = DeepSeekResponseFormat(type = "json_object"),
            temperature = 0.3
        )

        return executeWithRetry {
            val response = httpClient.post(ENDPOINT_CHAT_COMPLETIONS) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(requestPayload)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw IllegalStateException("DeepSeek API error ${response.status}: $errorBody")
            }

            val chatResponse = response.body<DeepSeekChatResponse>()
            val contentJson = chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response content from DeepSeek")

            parseBriefingJson(contentJson)
        }
    }

    /**
     * Executes an on-demand analytical reasoning query with optional physiological context.
     *
     * @param query The user's query or diagnostic command.
     * @param context Optional real-time [BiometricSnapshot] for grounding the analysis.
     * @return [Result] enclosing the generated textual analysis.
     */
    suspend fun requestOnDemandAnalysis(
        query: String,
        context: BiometricSnapshot? = null
    ): Result<String> {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("DeepSeek API key is not configured."))
        }

        val systemPrompt = """
            You are ORBIT X, an analytical co-pilot operating on the Galaxy S25 companion hub for Galaxy Watch 8.
            Provide analytical, precise, and scientifically grounded insights on biometrics, circadian pacing, and cognitive optimization.
        """.trimIndent()

        val contextPrompt = if (context != null) {
            """
            [Current Physiological State]
            Heart Rate: ${context.heartRateBpm} bpm | HRV: ${context.hrvRmssdMs} ms | SpO2: ${context.spo2Percent}%
            Skin Temp: ${context.skinTempCelsius} °C | Ambient: ${context.ambientTempCelsius} °C | Thermal Z-Score: ${context.thermalZScore}
            
            Query: $query
            """.trimIndent()
        } else {
            query
        }

        val requestPayload = DeepSeekChatRequest(
            model = MODEL_NAME,
            messages = listOf(
                DeepSeekMessage(role = "system", content = systemPrompt),
                DeepSeekMessage(role = "user", content = contextPrompt)
            ),
            temperature = 0.5
        )

        return executeWithRetry {
            val response = httpClient.post(ENDPOINT_CHAT_COMPLETIONS) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(requestPayload)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw IllegalStateException("DeepSeek API error ${response.status}: $errorBody")
            }

            val chatResponse = response.body<DeepSeekChatResponse>()
            chatResponse.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response from DeepSeek")
        }
    }

    private suspend fun resolveApiKey(): String {
        return appPreferences.deepSeekApiKey.first()
    }

    private suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay *= 2
                }
            }
        }
        return Result.failure(lastException ?: IllegalStateException("Unknown failure executing DeepSeek request"))
    }

    private fun parseBriefingJson(jsonString: String): DailyBriefing {
        @Serializable
        data class BriefingDto(
            val ringRecommendedMode: String,
            val coPilotHeadline: String,
            val priorityTaskId: String? = null,
            val recommendedFocusBlocksMin: Int = 45,
            val thermalRecoveryAssessment: String = "Normal autonomic stability",
            val alerts: List<String> = emptyList()
        )

        val dto = json.decodeFromString<BriefingDto>(jsonString)
        val mode = try {
            RingMode.valueOf(dto.ringRecommendedMode.trim().uppercase())
        } catch (_: Exception) {
            RingMode.HEALTH
        }

        val trimmedHeadline = if (dto.coPilotHeadline.length <= 38) {
            dto.coPilotHeadline
        } else {
            dto.coPilotHeadline.take(35) + "..."
        }

        return DailyBriefing(
            ringRecommendedMode = mode,
            coPilotHeadline = trimmedHeadline,
            priorityTaskId = dto.priorityTaskId,
            recommendedFocusBlocksMin = dto.recommendedFocusBlocksMin,
            thermalRecoveryAssessment = dto.thermalRecoveryAssessment,
            alerts = dto.alerts
        )
    }

    companion object {
        private const val ENDPOINT_CHAT_COMPLETIONS = "https://api.deepseek.com/chat/completions"
        private const val MODEL_NAME = "deepseek-chat"
    }
}

@Serializable
internal data class DeepSeekChatRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    @SerialName("response_format")
    val responseFormat: DeepSeekResponseFormat? = null,
    val temperature: Double = 0.5
)

@Serializable
internal data class DeepSeekMessage(
    val role: String,
    val content: String
)

@Serializable
internal data class DeepSeekResponseFormat(
    val type: String
)

@Serializable
internal data class DeepSeekChatResponse(
    val id: String? = null,
    val choices: List<DeepSeekChoice> = emptyList()
)

@Serializable
internal data class DeepSeekChoice(
    val index: Int = 0,
    val message: DeepSeekMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)
