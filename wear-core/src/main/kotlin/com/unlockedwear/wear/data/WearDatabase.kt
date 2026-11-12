package com.unlockedwear.wear.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Persisted entity for skin and ambient thermal recordings.
 */
@Entity(tableName = "thermal_readings")
data class ThermalReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val skinTemp: Float,
    val ambientTemp: Float,
    val compensatedTemp: Float,
    val deltaFromBaseline: Float,
    val zScore: Float,
    val isOnDemand: Boolean
)

/**
 * Persisted entity for continuous heart rate, RMSSD, and SpO2 readings.
 */
@Entity(tableName = "biometric_readings")
data class BiometricReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val heartRateBpm: Int,
    val rmssdMs: Double,
    val spo2Percent: Int,
    val ibiListJson: String = "[]"
)

/**
 * Persisted entity for DeepSeek AI daily co-pilot briefing.
 */
@Entity(tableName = "copilot_briefings")
data class BriefingEntity(
    @PrimaryKey
    val date: String,
    val coPilotHeadline: String,
    val priorityTask: String,
    val scheduleBlocksJson: String = "[]",
    val alertsJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Type converters for Room handling lists and JSON objects.
 */
class WearDatabaseConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrBlank()) emptyList() else json.decodeFromString(value)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        return if (value.isNullOrBlank()) emptyList() else json.decodeFromString(value)
    }
}

@Dao
interface ThermalDao {
    @Query("SELECT * FROM thermal_readings ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<ThermalReadingEntity?>

    @Query("SELECT * FROM thermal_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ThermalReadingEntity?

    @Query("SELECT * FROM thermal_readings ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 120): Flow<List<ThermalReadingEntity>>

    @Query("SELECT * FROM thermal_readings WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getReadingsSince(sinceTimestamp: Long): List<ThermalReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: ThermalReadingEntity): Long

    @Query("DELETE FROM thermal_readings WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOlderThan(cutoffTimestamp: Long): Int
}

@Dao
interface BiometricDao {
    @Query("SELECT * FROM biometric_readings ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<BiometricReadingEntity?>

    @Query("SELECT * FROM biometric_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): BiometricReadingEntity?

    @Query("SELECT * FROM biometric_readings ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 120): Flow<List<BiometricReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BiometricReadingEntity): Long

    @Query("DELETE FROM biometric_readings WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOlderThan(cutoffTimestamp: Long): Int
}

@Dao
interface BriefingDao {
    @Query("SELECT * FROM copilot_briefings ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestBriefing(): Flow<BriefingEntity?>

    @Query("SELECT * FROM copilot_briefings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBriefing(): BriefingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(briefing: BriefingEntity)

    @Query("DELETE FROM copilot_briefings")
    suspend fun clearBriefings()
}

/**
 * Core Room Database for UnlockedWear on Galaxy Watch.
 */
@Database(
    entities = [
        ThermalReadingEntity::class,
        BiometricReadingEntity::class,
        BriefingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(WearDatabaseConverters::class)
abstract class WearDatabase : RoomDatabase() {
    abstract fun thermalDao(): ThermalDao
    abstract fun biometricDao(): BiometricDao
    abstract fun briefingDao(): BriefingDao

    companion object {
        const val DATABASE_NAME = "unlockedwear_watch.db"
    }
}

/**
 * Hilt DI module providing Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWearDatabase(
        @ApplicationContext context: Context
    ): WearDatabase {
        return Room.databaseBuilder(
            context,
            WearDatabase::class.java,
            WearDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideThermalDao(database: WearDatabase): ThermalDao = database.thermalDao()

    @Provides
    fun provideBiometricDao(database: WearDatabase): BiometricDao = database.biometricDao()

    @Provides
    fun provideBriefingDao(database: WearDatabase): BriefingDao = database.briefingDao()
}
