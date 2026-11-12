package com.unlockedwear.wear.sensors

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for Samsung Health Sensor SDK managers.
 */
@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideSamsungSdkInitializer(
        @ApplicationContext context: Context
    ): SamsungSdkInitializer {
        return SamsungSdkInitializer(context)
    }

    @Provides
    @Singleton
    fun provideUnlockedThermalManager(
        @ApplicationContext context: Context,
        sdkInitializer: SamsungSdkInitializer
    ): UnlockedThermalManager {
        return UnlockedThermalManager(context, sdkInitializer)
    }

    @Provides
    @Singleton
    fun provideHeartRateManager(
        @ApplicationContext context: Context,
        sdkInitializer: SamsungSdkInitializer
    ): HeartRateManager {
        return HeartRateManager(context, sdkInitializer)
    }

    @Provides
    @Singleton
    fun provideSpO2Manager(
        @ApplicationContext context: Context,
        sdkInitializer: SamsungSdkInitializer
    ): SpO2Manager {
        return SpO2Manager(context, sdkInitializer)
    }
}
