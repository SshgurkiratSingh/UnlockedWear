package com.unlockedwear.mobile.weather

import com.unlockedwear.mobile.data.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing meteorological and solar UV radiation data services.
 */
@Module
@InstallIn(SingletonComponent::class)
object WeatherModule {

    @Provides
    @Singleton
    fun provideWeatherService(appPreferences: AppPreferences): WeatherService {
        return WeatherService(appPreferences)
    }
}
