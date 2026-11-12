package com.unlockedwear.mobile.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unlockedwear.mobile.ai.DeepSeekClient
import com.unlockedwear.mobile.data.BriefingRepository
import com.unlockedwear.mobile.data.SensorRepository
import com.unlockedwear.mobile.sync.PhoneToWatchSyncer
import com.unlockedwear.mobile.weather.WeatherService
import com.unlockedwear.protocol.CognitiveLoad
import com.unlockedwear.protocol.IntelligenceRequest
import com.unlockedwear.protocol.IntelligenceRequestType
import com.unlockedwear.protocol.TaskItem
import com.unlockedwear.protocol.WeatherState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager CoroutineWorker scheduled to trigger daily (at 7:00 AM) to synthesize
 * the morning intelligence briefing and dispatch it to the Galaxy Watch 8.
 */
@HiltWorker
class MorningBriefingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sensorRepository: SensorRepository,
    private val weatherService: WeatherService,
    private val deepSeekClient: DeepSeekClient,
    private val briefingRepository: BriefingRepository,
    private val phoneToWatchSyncer: PhoneToWatchSyncer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Gather overnight physiological recovery telemetry
            val overnightBiometrics = sensorRepository.getOvernightBiometricSummary()

            // 2. Fetch environmental conditions
            val weather = weatherService.getCurrentWeather(DEFAULT_LAT, DEFAULT_LON).getOrElse {
                WeatherState(
                    tempCelsius = 20.0f,
                    humidity = 50.0f,
                    uvIndex = 3.0f,
                    condition = "Clear",
                    forecastMaxUvi = 6.0f,
                    peakUvWindow = "12:00 PM - 2:00 PM",
                    barometricTrendHpa = 0.0f
                )
            }

            // 3. Assemble active workload agenda
            val taskQueue = listOf(
                TaskItem(id = "task-101", title = "Kernel & Firmware Review", deadlineHrs = 3.0, cognitiveLoad = CognitiveLoad.HIGH),
                TaskItem(id = "task-102", title = "Architectural Sprint Sync", deadlineHrs = 6.0, cognitiveLoad = CognitiveLoad.MEDIUM),
                TaskItem(id = "task-103", title = "Telemetry Ingestion Audit", deadlineHrs = 9.0, cognitiveLoad = CognitiveLoad.LOW)
            )

            val request = IntelligenceRequest(
                biometricTelemetry = overnightBiometrics,
                environmentalContext = weather,
                workloadQueue = taskQueue,
                requestType = IntelligenceRequestType.MORNING_BRIEFING
            )

            // 4. Request analytical synthesis from DeepSeek (ORBIT X)
            val briefingResult = deepSeekClient.requestMorningBriefing(request)

            if (briefingResult.isSuccess) {
                val briefing = briefingResult.getOrThrow()

                // 5. Persist locally to Room
                briefingRepository.saveBriefing(briefing)

                // 6. Push to Galaxy Watch 8 via Wearable Data Layer
                phoneToWatchSyncer.pushDailyBriefing(briefing)
                phoneToWatchSyncer.pushRingModeRecommendation(briefing.ringRecommendedMode)

                Result.success()
            } else {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "UnlockedMorningBriefingWork"
        private const val DEFAULT_LAT = 37.7749
        private const val DEFAULT_LON = -122.4194
    }
}
