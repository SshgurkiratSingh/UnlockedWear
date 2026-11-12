package com.unlockedwear.mobile.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.unlockedwear.protocol.RingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persisted entity representing a single thermal telemetry reading from the watch sensor.
 */
@Entity(tableName = "thermal_readings")
data class ThermalReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val skinTemp: Float,
    val ambientTemp: Float,
    val compensatedTemp: Float,
    val thermalZScore: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Persisted entity capturing real-time physiological indicators (HR, HRV, SpO2).
 */
@Entity(tableName = "biometric_readings")
data class BiometricReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val heartRate: Double,
    val hrvRmssd: Double,
    val spo2: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Persisted entity representing a synthesized daily intelligence briefing.
 */
@Entity(tableName = "briefings")
data class BriefingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val headline: String,
    val priorityTaskId: String?,
    val focusBlocksMin: Int,
    val thermalAssessment: String,
    val alertsJson: String,
    val ringMode: RingMode,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Persisted entity caching recent environmental and meteorological telemetry.
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val temp: Float,
    val humidity: Float,
    val uvIndex: Float,
    val condition: String,
    val forecastMaxUvi: Float,
    val peakUvWindow: String,
    val barometricTrend: Float,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Data Access Object for querying and inserting thermal readings.
 */
@Dao
interface ThermalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: ThermalReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<ThermalReadingEntity>)

    @Query("SELECT * FROM thermal_readings WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    fun getHistorySince(sinceMs: Long): Flow<List<ThermalReadingEntity>>

    @Query("SELECT * FROM thermal_readings ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ThermalReadingEntity>>

    @Query("SELECT * FROM thermal_readings ORDER BY timestampMs DESC LIMIT 1")
    fun getLatest(): Flow<ThermalReadingEntity?>

    @Query("SELECT * FROM thermal_readings ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLatestDirect(): ThermalReadingEntity?
}

/**
 * Data Access Object for biometric telemetry records.
 */
@Dao
interface BiometricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BiometricReadingEntity): Long

    @Query("SELECT * FROM biometric_readings ORDER BY timestampMs DESC LIMIT 1")
    fun getLatest(): Flow<BiometricReadingEntity?>

    @Query("SELECT * FROM biometric_readings ORDER BY timestampMs DESC LIMIT 1")
    suspend fun getLatestDirect(): BiometricReadingEntity?

    @Query("SELECT * FROM biometric_readings WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    fun getHistorySince(sinceMs: Long): Flow<List<BiometricReadingEntity>>

    @Query("SELECT * FROM biometric_readings WHERE timestampMs BETWEEN :sinceMs AND :untilMs ORDER BY timestampMs ASC")
    suspend fun getReadingsBetween(sinceMs: Long, untilMs: Long): List<BiometricReadingEntity>
}

/**
 * Data Access Object for daily briefings.
 */
@Dao
interface BriefingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(briefing: BriefingEntity): Long

    @Query("SELECT * FROM briefings ORDER BY createdAt DESC LIMIT 1")
    fun getLatest(): Flow<BriefingEntity?>

    @Query("SELECT * FROM briefings ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestDirect(): BriefingEntity?

    @Query("SELECT * FROM briefings ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BriefingEntity>>
}

/**
 * Data Access Object for meteorological telemetry cache.
 */
@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: WeatherCacheEntity): Long

    @Query("SELECT * FROM weather_cache ORDER BY updatedAt DESC LIMIT 1")
    fun getLatest(): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDirect(): WeatherCacheEntity?
}

/**
 * Room Type Converters for non-primitive attributes.
 */
class UnlockedConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRingMode(mode: RingMode): String = mode.name

    @TypeConverter
    fun toRingMode(value: String): RingMode = try {
        RingMode.valueOf(value)
    } catch (_: Exception) {
        RingMode.HEALTH
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> = try {
        json.decodeFromString(value)
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Room database definition for UnlockedWear Mobile Hub.
 */
@Database(
    entities = [
        ThermalReadingEntity::class,
        BiometricReadingEntity::class,
        BriefingEntity::class,
        WeatherCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(UnlockedConverters::class)
abstract class UnlockedDatabase : RoomDatabase() {
    abstract fun thermalDao(): ThermalDao
    abstract fun biometricDao(): BiometricDao
    abstract fun briefingDao(): BriefingDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        const val DATABASE_NAME = "unlockedwear_mobile.db"
    }
}
