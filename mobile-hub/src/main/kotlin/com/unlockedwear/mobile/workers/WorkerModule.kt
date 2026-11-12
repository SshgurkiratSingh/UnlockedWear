package com.unlockedwear.mobile.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for scheduling background tasks with WorkManager.
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Enqueues periodic workers for weather syncing (every 2h) and morning briefing (daily at 7:00 AM).
     */
    fun schedulePeriodicWork() {
        val workManager = WorkManager.getInstance(context)

        val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. Weather Sync Worker: Every 2 Hours
        val weatherRequest = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
            repeatInterval = 2,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        ).setConstraints(networkConstraint).build()

        workManager.enqueueUniquePeriodicWork(
            WeatherSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            weatherRequest
        )

        // 2. Morning Briefing Worker: Every 24 Hours, scheduled for 7:00 AM
        val initialDelayMinutes = computeInitialDelayMinutes(targetHour = 7, targetMinute = 0)
        val briefingRequest = PeriodicWorkRequestBuilder<MorningBriefingWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 30,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()

        workManager.enqueueUniquePeriodicWork(
            MorningBriefingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            briefingRequest
        )
    }

    private fun computeInitialDelayMinutes(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val diffMs = target.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(diffMs).coerceAtLeast(0L)
    }
}

/**
 * Hilt module providing the WorkScheduler.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWorkScheduler(@ApplicationContext context: Context): WorkScheduler {
        return WorkScheduler(context)
    }
}
