package com.unlockedwear.mobile.sync

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.unlockedwear.protocol.DataLayerPaths
import com.unlockedwear.protocol.DailyBriefing
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.WeatherState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service orchestrating outbound synchronization from the Galaxy S25 companion hub
 * to the Galaxy Watch 8 over the Wearable Data Layer and Message API.
 */
@Singleton
class PhoneToWatchSyncer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val nodeClient: NodeClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Checks if at least one Wear OS watch node is currently connected via Bluetooth or Wi-Fi.
     */
    suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the list of display names of all connected watch nodes.
     */
    suspend fun getConnectedNodeNames(): List<String> {
        return try {
            nodeClient.connectedNodes.await().map { it.displayName }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Pushes a synthesized [DailyBriefing] to the connected watch.
     *
     * @param briefing The briefing containing co-pilot headline, mode recommendation, and alerts.
     * @return `true` if synchronized successfully to the Data Layer, `false` otherwise.
     */
    suspend fun pushDailyBriefing(briefing: DailyBriefing): Boolean {
        return try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.STATE_DAILY_BRIEFING)
            val dataMap = putDataMapReq.dataMap

            dataMap.putString("ringRecommendedMode", briefing.ringRecommendedMode.name)
            dataMap.putString("coPilotHeadline", briefing.coPilotHeadline)
            dataMap.putString("priorityTaskId", briefing.priorityTaskId ?: "")
            dataMap.putInt("recommendedFocusBlocksMin", briefing.recommendedFocusBlocksMin)
            dataMap.putString("thermalRecoveryAssessment", briefing.thermalRecoveryAssessment)
            dataMap.putStringArrayList("alerts", ArrayList(briefing.alerts))
            dataMap.putString("json", json.encodeToString(briefing))
            dataMap.putLong("updatedAt", System.currentTimeMillis())

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()

            // Also dispatch high-priority message wake-up to connected nodes
            val serializedBytes = json.encodeToString(briefing).toByteArray(Charsets.UTF_8)
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, DataLayerPaths.STATE_DAILY_BRIEFING, serializedBytes).await()
                } catch (_: Exception) {
                    // Message fallback failure is non-fatal since DataItem is queued
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pushes real-time [WeatherState] meteorological and UV data to the connected watch.
     *
     * @param weather The current weather, UV index, and atmospheric forecast.
     * @return `true` if transmission succeeded, `false` otherwise.
     */
    suspend fun pushWeatherUpdate(weather: WeatherState): Boolean {
        return try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.STATE_WEATHER)
            val dataMap = putDataMapReq.dataMap

            dataMap.putFloat("tempCelsius", weather.tempCelsius)
            dataMap.putFloat("humidity", weather.humidity)
            dataMap.putFloat("uvIndex", weather.uvIndex)
            dataMap.putString("condition", weather.condition)
            dataMap.putFloat("forecastMaxUvi", weather.forecastMaxUvi)
            dataMap.putString("peakUvWindow", weather.peakUvWindow)
            dataMap.putFloat("barometricTrendHpa", weather.barometricTrendHpa)
            dataMap.putString("json", json.encodeToString(weather))
            dataMap.putLong("updatedAt", System.currentTimeMillis())

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()

            val serializedBytes = json.encodeToString(weather).toByteArray(Charsets.UTF_8)
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, DataLayerPaths.STATE_WEATHER, serializedBytes).await()
                } catch (_: Exception) {
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pushes a recommended active [RingMode] configuration to the watch face bezel gauge.
     *
     * @param mode The target [RingMode] to activate.
     * @return `true` if transmission succeeded, `false` otherwise.
     */
    suspend fun pushRingModeRecommendation(mode: RingMode): Boolean {
        return try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.STATE_RING_MODE)
            val dataMap = putDataMapReq.dataMap

            dataMap.putString("ringMode", mode.name)
            dataMap.putLong("updatedAt", System.currentTimeMillis())

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()

            val bytes = mode.name.toByteArray(Charsets.UTF_8)
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, DataLayerPaths.STATE_RING_MODE, bytes).await()
                } catch (_: Exception) {
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pushes full watch face visual & complication customizations to Galaxy Watch 8.
     */
    suspend fun pushWatchFaceConfig(
        ringMode: RingMode,
        accentColorHex: String,
        topComplication: String,
        bottomComplication: String,
        dialStyle: String,
        complicationSlots: List<String> = listOf(topComplication, bottomComplication)
    ): Boolean {
        return try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.CONFIG_SYNC)
            val dataMap = putDataMapReq.dataMap

            dataMap.putString("ringMode", ringMode.name)
            dataMap.putString("accentColorHex", accentColorHex)
            dataMap.putString("topComplication", topComplication)
            dataMap.putString("bottomComplication", bottomComplication)
            dataMap.putString("dialStyle", dialStyle)
            dataMap.putString("complicationSlots", complicationSlots.joinToString("||"))
            dataMap.putLong("updatedAt", System.currentTimeMillis())

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()

            // Also send direct trigger to connected nodes
            val payload = "$ringMode|$accentColorHex|$topComplication|$bottomComplication|$dialStyle|${complicationSlots.joinToString("||")}".toByteArray(Charsets.UTF_8)
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, DataLayerPaths.CONFIG_SYNC, payload).await()
                } catch (_: Exception) {
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Requests an immediate on-demand temperature measurement sweep from the connected Galaxy Watch 8.
     */
    suspend fun requestOnDemandTemperatureMeasurement(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            val payload = "READ_TEMP_NOW".toByteArray(Charsets.UTF_8)
            for (node in nodes) {
                try { messageClient.sendMessage(node.id, DataLayerPaths.COMMAND_READ_TEMP, payload).await() } catch (_: Exception) {}
            }
            true
        } catch (_: Exception) { false }
    }

    /**
     * Pushes extended watch face feature flags to Galaxy Watch 8 via
     * [DataLayerPaths.CONFIG_SYNC_FEATURES].
     *
     * @param flags [WatchFaceFeatureFlags] data class — all boolean feature toggles.
     * @return `true` if dispatched successfully.
     */
    suspend fun pushWatchFaceFeatureFlags(flags: com.unlockedwear.protocol.WatchFaceFeatureFlags): Boolean {
        return try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.CONFIG_SYNC_FEATURES)
            with(putDataMapReq.dataMap) {
                putBoolean("bioReactiveArc",  flags.bioReactiveArc)
                putBoolean("hrvBand",         flags.hrvBand)
                putBoolean("circadianSector", flags.circadianSector)
                putBoolean("thermalStrip",    flags.thermalStrip)
                putBoolean("coPilotTicker",   flags.coPilotTicker)
                putBoolean("uvZoneArc",       flags.uvZoneArc)
                putBoolean("recoveryScore",   flags.recoveryScore)
                putBoolean("focusCountdown",  flags.focusCountdown)
                putLong("updatedAt", System.currentTimeMillis())
                // Also carry JSON for message path
                putString("json", json.encodeToString(flags))
            }
            dataClient.putDataItem(putDataMapReq.asPutDataRequest().setUrgent()).await()
            // Direct message to all connected nodes
            val payload = json.encodeToString(flags).toByteArray(Charsets.UTF_8)
            nodeClient.connectedNodes.await().forEach { node ->
                try { messageClient.sendMessage(node.id, DataLayerPaths.CONFIG_SYNC_FEATURES, payload).await() } catch (_: Exception) {}
            }
            true
        } catch (_: Exception) { false }
    }
}
