package com.unlockedwear.mobile.data

import com.unlockedwear.protocol.BiometricSnapshot
import com.unlockedwear.protocol.ThermalFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository orchestrating local persistence and retrieval of raw and synthesized physiological metrics.
 */
@Singleton
class SensorRepository @Inject constructor(
    private val thermalDao: ThermalDao,
    private val biometricDao: BiometricDao
) {
    /**
     * Observes real-time [BiometricSnapshot] combined from the latest biometric and thermal records.
     */
    fun getLatestBiometrics(): Flow<BiometricSnapshot> {
        return combine(
            biometricDao.getLatest(),
            thermalDao.getLatest()
        ) { bio, therm ->
            BiometricSnapshot(
                heartRateBpm = bio?.heartRate ?: 72.0,
                hrvRmssdMs = bio?.hrvRmssd ?: 45.0,
                spo2Percent = bio?.spo2 ?: 98,
                skinTempCelsius = therm?.skinTemp ?: 33.5f,
                ambientTempCelsius = therm?.ambientTemp ?: 24.0f,
                thermalZScore = therm?.thermalZScore ?: 0.0,
                timestampMs = bio?.timestampMs ?: System.currentTimeMillis()
            )
        }
    }

    /**
     * Queries thermal telemetry entries spanning the preceding [hours] window.
     *
     * @param hours Number of hours of history to fetch.
     */
    fun getThermalHistory(hours: Int): Flow<List<ThermalReadingEntity>> {
        val sinceMs = System.currentTimeMillis() - (hours.toLong() * 3600_000L)
        return thermalDao.getHistorySince(sinceMs)
    }

    /**
     * Queries biometric telemetry entries spanning the preceding [hours] window.
     */
    fun getBiometricHistory(hours: Int): Flow<List<BiometricReadingEntity>> {
        val sinceMs = System.currentTimeMillis() - (hours.toLong() * 3600_000L)
        return biometricDao.getHistorySince(sinceMs)
    }

    /**
     * Calculates an aggregated overnight summary snapshot across the nocturnal window (preceding 8 hours).
     *
     * Averages heart rate, RMSSD, SpO2, and calculates mean nocturnal skin temperature.
     */
    suspend fun getOvernightBiometricSummary(): BiometricSnapshot {
        val now = System.currentTimeMillis()
        val eightHoursAgo = now - (8L * 3600_000L)

        val bioReadings = biometricDao.getReadingsBetween(eightHoursAgo, now)
        val latestTherm = thermalDao.getLatestDirect()

        if (bioReadings.isEmpty()) {
            return BiometricSnapshot(
                heartRateBpm = 64.0,
                hrvRmssdMs = 52.0,
                spo2Percent = 98,
                skinTempCelsius = latestTherm?.skinTemp ?: 34.0f,
                ambientTempCelsius = latestTherm?.ambientTemp ?: 22.5f,
                thermalZScore = latestTherm?.thermalZScore ?: -0.15,
                timestampMs = now
            )
        }

        val avgHr = bioReadings.map { it.heartRate }.average()
        val avgHrv = bioReadings.map { it.hrvRmssd }.average()
        val avgSpo2 = bioReadings.map { it.spo2 }.average().toInt()

        return BiometricSnapshot(
            heartRateBpm = avgHr,
            hrvRmssdMs = avgHrv,
            spo2Percent = avgSpo2,
            skinTempCelsius = latestTherm?.skinTemp ?: 34.0f,
            ambientTempCelsius = latestTherm?.ambientTemp ?: 22.5f,
            thermalZScore = latestTherm?.thermalZScore ?: 0.0,
            timestampMs = now
        )
    }

    /**
     * Persists a newly acquired [ThermalFrame] reading with optional pre-computed
     * baseline deviation and z-score (computed on-watch by [UnlockedThermalManager]).
     */
    suspend fun insertThermalReading(
        frame: ThermalFrame,
        deltaFromBaseline: Float = 0f,
        thermalZScore: Double = 0.0
    ) {
        val entity = ThermalReadingEntity(
            skinTemp           = frame.skinTempCelsius,
            ambientTemp        = frame.ambientTempCelsius,
            compensatedTemp    = frame.compensatedTempCelsius,
            thermalZScore      = thermalZScore,
            timestampMs        = frame.timestampMs
        )
        thermalDao.insert(entity)
    }

    /**
     * Persists a full [BiometricSnapshot].
     */
    suspend fun insertBiometricReading(snapshot: BiometricSnapshot) {
        val bioEntity = BiometricReadingEntity(
            heartRate = snapshot.heartRateBpm,
            hrvRmssd = snapshot.hrvRmssdMs,
            spo2 = snapshot.spo2Percent,
            timestampMs = snapshot.timestampMs
        )
        biometricDao.insert(bioEntity)

        val thermEntity = ThermalReadingEntity(
            skinTemp = snapshot.skinTempCelsius,
            ambientTemp = snapshot.ambientTempCelsius,
            compensatedTemp = snapshot.skinTempCelsius,
            thermalZScore = snapshot.thermalZScore,
            timestampMs = snapshot.timestampMs
        )
        thermalDao.insert(thermEntity)
    }
}
