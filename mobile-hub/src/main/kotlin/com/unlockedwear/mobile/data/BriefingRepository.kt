package com.unlockedwear.mobile.data

import com.unlockedwear.protocol.DailyBriefing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing daily intelligence briefing records.
 */
@Singleton
class BriefingRepository @Inject constructor(
    private val briefingDao: BriefingDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Observes the most recently synthesized [DailyBriefing], or emits `null` if none exist.
     */
    fun getLatestBriefing(): Flow<DailyBriefing?> {
        return briefingDao.getLatest().map { entity ->
            entity?.let { toDomain(it) }
        }
    }

    /**
     * Synchronously queries the latest briefing record.
     */
    suspend fun getLatestBriefingDirect(): DailyBriefing? {
        return briefingDao.getLatestDirect()?.let { toDomain(it) }
    }

    /**
     * Returns an observable stream of all historical [BriefingEntity] records.
     */
    fun getAllBriefings(): Flow<List<BriefingEntity>> {
        return briefingDao.getAll()
    }

    /**
     * Persists a synthesized [DailyBriefing] into the local database.
     *
     * @param briefing The briefing model to save.
     */
    suspend fun saveBriefing(briefing: DailyBriefing) {
        val alertsJson = try {
            json.encodeToString(briefing.alerts)
        } catch (_: Exception) {
            "[]"
        }

        val entity = BriefingEntity(
            headline = briefing.coPilotHeadline,
            priorityTaskId = briefing.priorityTaskId,
            focusBlocksMin = briefing.recommendedFocusBlocksMin,
            thermalAssessment = briefing.thermalRecoveryAssessment,
            alertsJson = alertsJson,
            ringMode = briefing.ringRecommendedMode,
            createdAt = System.currentTimeMillis()
        )
        briefingDao.insert(entity)
    }

    private fun toDomain(entity: BriefingEntity): DailyBriefing {
        val alerts = try {
            json.decodeFromString<List<String>>(entity.alertsJson)
        } catch (_: Exception) {
            emptyList()
        }

        return DailyBriefing(
            ringRecommendedMode = entity.ringMode,
            coPilotHeadline = entity.headline,
            priorityTaskId = entity.priorityTaskId,
            recommendedFocusBlocksMin = entity.focusBlocksMin,
            thermalRecoveryAssessment = entity.thermalAssessment,
            alerts = alerts
        )
    }
}
