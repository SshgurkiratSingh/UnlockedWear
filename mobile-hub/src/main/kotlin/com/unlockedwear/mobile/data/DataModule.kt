package com.unlockedwear.mobile.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module configuring Room database instances, Data Access Objects, and repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideUnlockedDatabase(
        @ApplicationContext context: Context
    ): UnlockedDatabase {
        return Room.databaseBuilder(
            context,
            UnlockedDatabase::class.java,
            UnlockedDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideThermalDao(database: UnlockedDatabase): ThermalDao {
        return database.thermalDao()
    }

    @Provides
    fun provideBiometricDao(database: UnlockedDatabase): BiometricDao {
        return database.biometricDao()
    }

    @Provides
    fun provideBriefingDao(database: UnlockedDatabase): BriefingDao {
        return database.briefingDao()
    }

    @Provides
    fun provideWeatherDao(database: UnlockedDatabase): WeatherDao {
        return database.weatherDao()
    }

    @Provides
    @Singleton
    fun provideSensorRepository(
        thermalDao: ThermalDao,
        biometricDao: BiometricDao
    ): SensorRepository {
        return SensorRepository(thermalDao, biometricDao)
    }

    @Provides
    @Singleton
    fun provideBriefingRepository(
        briefingDao: BriefingDao
    ): BriefingRepository {
        return BriefingRepository(briefingDao)
    }
}
