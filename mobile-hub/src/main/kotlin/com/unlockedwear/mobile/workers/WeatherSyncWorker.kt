package com.unlockedwear.mobile.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unlockedwear.mobile.data.WeatherCacheEntity
import com.unlockedwear.mobile.data.WeatherDao
import com.unlockedwear.mobile.sync.PhoneToWatchSyncer
import com.unlockedwear.mobile.weather.WeatherService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager CoroutineWorker periodically querying real-time weather and solar UV radiation
 * and synchronizing state with the watch.
 */
@HiltWorker
class WeatherSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val weatherService: WeatherService,
    private val weatherDao: WeatherDao,
    private val phoneToWatchSyncer: PhoneToWatchSyncer
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val weatherResult = weatherService.getCurrentWeather(DEFAULT_LAT, DEFAULT_LON)

            if (weatherResult.isSuccess) {
                val weather = weatherResult.getOrThrow()

                // Cache in local Room database
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

                // Transmit to watch
                phoneToWatchSyncer.pushWeatherUpdate(weather)

                Result.success()
            } else {
                if (runAttemptCount < 2) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "UnlockedWeatherSyncWork"
        private const val DEFAULT_LAT = 37.7749
        private const val DEFAULT_LON = -122.4194
    }
}
