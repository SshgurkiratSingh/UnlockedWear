package com.unlockedwear.mobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.unlockedwear.mobile.workers.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Android Application class for the UnlockedWear mobile companion app on Galaxy S25.
 *
 * Configures Dagger-Hilt dependency injection and registers periodic WorkManager background
 * tasks on startup.
 */
@HiltAndroidApp
class UnlockedMobileApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Register periodic background synchronization and morning briefing schedules
        workScheduler.schedulePeriodicWork()
    }
}
