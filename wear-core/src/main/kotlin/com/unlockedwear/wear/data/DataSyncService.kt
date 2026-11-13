package com.unlockedwear.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.unlockedwear.protocol.BiometricSnapshot
import com.unlockedwear.protocol.DataLayerPaths
import com.unlockedwear.protocol.DailyBriefing
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.RingState
import com.unlockedwear.protocol.ThermalFrame
import com.unlockedwear.protocol.WatchFaceFeatureFlags
import com.unlockedwear.protocol.WeatherState
import com.unlockedwear.wear.complications.CoPilotComplicationService
import com.unlockedwear.wear.complications.MissionRingComplicationService
import com.unlockedwear.wear.complications.ThermalComplicationService
import com.unlockedwear.wear.sensors.UnlockedThermalManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Background synchronization service implementing WearableListenerService.
 *
 * Responsibilities:
 * - Listens for incoming phone messages and data changes via Wearable Data Layer API.
 * - Ingests AI daily briefings at [DataLayerPaths.STATE_DAILY_BRIEFING] and persists them into Room.
 * - Ingests real-time weather updates at [DataLayerPaths.STATE_WEATHER] and updates RingStateDataStore.
 * - Dispatches sensor telemetry to paired phone via [DataLayerPaths.SENSOR_THERMAL] and [DataLayerPaths.SENSOR_BIOMETRIC].
 * - Triggers immediate complication updates upon receiving new phone briefings or weather data.
 */
@AndroidEntryPoint
class DataSyncService : WearableListenerService() {

    companion object {
        private const val TAG = "DataSyncService"

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Sends a message to all connected phone nodes.
         */
        suspend fun sendMessageToPhone(context: Context, path: String, payload: ByteArray): Boolean {
            return try {
                val nodeClient = Wearable.getNodeClient(context)
                val nodes = nodeClient.connectedNodes.await()
                var sentAny = false
                val messageClient = Wearable.getMessageClient(context)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, payload).await()
                    sentAny = true
                }
                sentAny
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending message to phone on path $path", e)
                false
            }
        }

        /**
         * Dispatches thermal telemetry frame to phone via Data Layer API.
         * Includes delta-from-baseline and z-score computed by [UnlockedThermalManager].
         */
        suspend fun dispatchThermalFrame(
            context: Context,
            frame: ThermalFrame,
            deltaFromBaseline: Float = 0f,
            zScore: Float = 0f
        ) {
            try {
                val request = PutDataMapRequest.create(DataLayerPaths.SENSOR_THERMAL).apply {
                    dataMap.putLong("timestampMs", frame.timestampMs)
                    dataMap.putFloat("skinTempCelsius", frame.skinTempCelsius)
                    dataMap.putFloat("ambientTempCelsius", frame.ambientTempCelsius)
                    dataMap.putFloat("compensatedTempCelsius", frame.compensatedTempCelsius)
                    dataMap.putInt("status", frame.status)
                    dataMap.putFloat("deltaFromBaseline", deltaFromBaseline)
                    dataMap.putFloat("zScore", zScore)
                    // Force DataItem to be re-sent even if values haven't changed
                    dataMap.putLong("_ts", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching thermal frame to phone", e)
            }
        }

        /**
         * Dispatches biometric telemetry snapshot to phone via Data Layer API.
         */
        suspend fun dispatchBiometricSnapshot(
            context: Context,
            snapshot: BiometricSnapshot
        ) {
            try {
                val request = PutDataMapRequest.create(DataLayerPaths.SENSOR_BIOMETRIC).apply {
                    dataMap.putLong("timestampMs", snapshot.timestampMs)
                    dataMap.putDouble("heartRateBpm", snapshot.heartRateBpm)
                    dataMap.putDouble("hrvRmssdMs", snapshot.hrvRmssdMs)
                    dataMap.putInt("spo2Percent", snapshot.spo2Percent)
                    dataMap.putFloat("skinTempCelsius", snapshot.skinTempCelsius)
                    dataMap.putFloat("ambientTempCelsius", snapshot.ambientTempCelsius)
                    dataMap.putDouble("thermalZScore", snapshot.thermalZScore)
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context).putDataItem(request).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching biometric telemetry to phone", e)
            }
        }
    }

    @Inject
    lateinit var database: WearDatabase

    @Inject
    lateinit var ringStateDataStore: RingStateDataStore

    @Inject
    lateinit var watchFaceConfigDataStore: WatchFaceConfigDataStore

    @Inject
    lateinit var thermalManager: UnlockedThermalManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data
        Log.i(TAG, "Message received at path: $path, size: ${data.size} bytes")

        when (path) {
            DataLayerPaths.STATE_DAILY_BRIEFING -> {
                serviceScope.launch {
                    try {
                        val payloadString = String(data, Charsets.UTF_8)
                        val briefing = json.decodeFromString<DailyBriefing>(payloadString)
                        val entity = BriefingEntity(
                            date = "TODAY",
                            coPilotHeadline = briefing.coPilotHeadline,
                            priorityTask = briefing.priorityTaskId ?: "Focus Block",
                            scheduleBlocksJson = "[]",
                            alertsJson = json.encodeToString(briefing.alerts),
                            timestamp = System.currentTimeMillis()
                        )
                        database.briefingDao().insertOrUpdate(entity)
                        Log.i(TAG, "Persisted daily briefing from phone: ${briefing.coPilotHeadline}")
                        CoPilotComplicationService.triggerImmediateRefresh(applicationContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing daily briefing message payload", e)
                    }
                }
            }

            DataLayerPaths.STATE_WEATHER -> {
                serviceScope.launch {
                    try {
                        val payloadString = String(data, Charsets.UTF_8)
                        val weather = json.decodeFromString<WeatherState>(payloadString)
                        Log.i(TAG, "Received weather update: ${weather.condition}, UV: ${weather.uvIndex}")

                        val currentState = ringStateDataStore.getCurrentRingState()
                        if (currentState.mode == RingMode.UV) {
                            val updatedState = currentState.copy(
                                currentValue = weather.uvIndex,
                                label = "UV Index (${weather.condition})",
                                formattedMetric = "${String.format("%.1f", weather.uvIndex)} UVI"
                            )
                            ringStateDataStore.updateRingState(updatedState)
                            MissionRingComplicationService.triggerImmediateRefresh(applicationContext)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing weather message payload", e)
                    }
                }
            }

            DataLayerPaths.CONFIG_SYNC -> {
                // Pipe-delimited payload with an optional custom slot list.
                serviceScope.launch {
                    try {
                        val payload = String(data, Charsets.UTF_8)
                        val parts = payload.split("|")
                        if (parts.size >= 5) {
                            val ringMode = try { RingMode.valueOf(parts[0]) } catch (_: Exception) { RingMode.HEALTH }
                            val accentColorHex  = parts[1]
                            val topComplication = parts[2]
                            val bottomComplication = parts[3]
                            val dialStyle = parts[4]
                            val complicationSlots = parts.getOrNull(5)?.split("||")?.filter { it.isNotBlank() }
                                ?.ifEmpty { listOf(topComplication, bottomComplication) }
                                ?: listOf(topComplication, bottomComplication)

                            watchFaceConfigDataStore.applyConfig(
                                accentColorHex     = accentColorHex,
                                topComplication    = topComplication,
                                bottomComplication = bottomComplication,
                                dialStyle          = dialStyle,
                                ringMode           = ringMode,
                                complicationSlots  = complicationSlots
                            )
                            // Keep ring-gauge DataStore in sync with the new mode
                            ringStateDataStore.setRingMode(ringMode)
                            MissionRingComplicationService.triggerImmediateRefresh(applicationContext)
                            CoPilotComplicationService.triggerImmediateRefresh(applicationContext)
                            ThermalComplicationService.triggerImmediateRefresh(applicationContext)
                            Log.i(TAG, "Watch face config applied: ringMode=$ringMode accentColor=$accentColorHex dialStyle=$dialStyle")
                        } else {
                            Log.w(TAG, "CONFIG_SYNC message has unexpected part count: ${parts.size}, payload: $payload")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error applying CONFIG_SYNC message", e)
                    }
                }
            }

            DataLayerPaths.CONFIG_SYNC_FEATURES -> {
                serviceScope.launch {
                    try {
                        val flags = json.decodeFromString<WatchFaceFeatureFlags>(String(data, Charsets.UTF_8))
                        watchFaceConfigDataStore.applyFeatureFlags(flags)
                        Log.i(TAG, "Feature flags applied: bioReactive=${flags.bioReactiveArc} hrv=${flags.hrvBand} thermal=${flags.thermalStrip}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error applying CONFIG_SYNC_FEATURES message", e)
                    }
                }
            }

            DataLayerPaths.COMMAND_READ_TEMP -> {                serviceScope.launch {
                    try {
                        Log.i(TAG, "Received on-demand temperature measurement command from mobile")
                        thermalManager.streamSkinTemperature(onDemand = true)
                            .take(5)
                            .collect { frame ->
                                val delta  = frame.compensatedTempCelsius - thermalManager.getBaselineTemperature()
                                val zScore = thermalManager.getCurrentZScore()
                                dispatchThermalFrame(
                                    context           = applicationContext,
                                    frame             = frame,
                                    deltaFromBaseline = delta,
                                    zScore            = zScore
                                )
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling COMMAND_READ_TEMP", e)
                    }
                }
            }

            else -> {
                Log.d(TAG, "Unhandled message path: $path")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                val path = uri.path
                if (path == DataLayerPaths.STATE_DAILY_BRIEFING) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val map = dataMapItem.dataMap
                    val headline = map.getString("coPilotHeadline", "")
                    val priorityTask = map.getString("priorityTaskId", "")
                    val alertsJson = map.getString("alertsJson", "[]")

                    serviceScope.launch {
                        val entity = BriefingEntity(
                            date = "TODAY",
                            coPilotHeadline = headline,
                            priorityTask = priorityTask,
                            scheduleBlocksJson = "[]",
                            alertsJson = alertsJson,
                            timestamp = System.currentTimeMillis()
                        )
                        database.briefingDao().insertOrUpdate(entity)
                        CoPilotComplicationService.triggerImmediateRefresh(applicationContext)
                    }
                } else if (path == DataLayerPaths.STATE_WEATHER) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val map = dataMapItem.dataMap
                    val uvIndex = map.getFloat("uvIndex", 0f)
                    val condition = map.getString("condition", "")

                    serviceScope.launch {
                        val currentState = ringStateDataStore.getCurrentRingState()
                        if (currentState.mode == RingMode.UV) {
                            val updatedState = currentState.copy(
                                currentValue = uvIndex,
                                label = "UV Index ($condition)",
                                formattedMetric = "${String.format("%.1f", uvIndex)} UVI"
                            )
                            ringStateDataStore.updateRingState(updatedState)
                            MissionRingComplicationService.triggerImmediateRefresh(applicationContext)
                        }
                    }
                } else if (path == DataLayerPaths.CONFIG_SYNC) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val map = dataMapItem.dataMap

                    serviceScope.launch {
                        try {
                            val ringModeStr        = map.getString("ringMode", RingMode.HEALTH.name) ?: RingMode.HEALTH.name
                            val accentColorHex     = map.getString("accentColorHex", "#00D4FF") ?: "#00D4FF"
                            val topComplication    = map.getString("topComplication", "Heart Rate (BPM)") ?: "Heart Rate (BPM)"
                            val bottomComplication = map.getString("bottomComplication", "UV Index") ?: "UV Index"
                            val dialStyle          = map.getString("dialStyle", "Digital Modern") ?: "Digital Modern"
                            val complicationSlots = (map.getString("complicationSlots", "") ?: "")
                                .split("||").filter { it.isNotBlank() }
                            val ringMode = try { RingMode.valueOf(ringModeStr) } catch (_: Exception) { RingMode.HEALTH }

                            watchFaceConfigDataStore.applyConfig(
                                accentColorHex     = accentColorHex,
                                topComplication    = topComplication,
                                bottomComplication = bottomComplication,
                                dialStyle          = dialStyle,
                                ringMode           = ringMode,
                                complicationSlots  = complicationSlots.ifEmpty { listOf(topComplication, bottomComplication) }
                            )
                            ringStateDataStore.setRingMode(ringMode)
                            MissionRingComplicationService.triggerImmediateRefresh(applicationContext)
                            CoPilotComplicationService.triggerImmediateRefresh(applicationContext)
                            ThermalComplicationService.triggerImmediateRefresh(applicationContext)
                            Log.i(TAG, "Watch face config applied via DataItem: ringMode=$ringMode accentColor=$accentColorHex")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying CONFIG_SYNC DataItem", e)
                        }
                    }
                } else if (path == DataLayerPaths.CONFIG_SYNC_FEATURES) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val map = dataMapItem.dataMap
                    serviceScope.launch {
                        try {
                            val jsonStr = map.getString("json", "").takeIf { it.isNotEmpty() }
                            if (jsonStr != null) {
                                val flags = json.decodeFromString<WatchFaceFeatureFlags>(jsonStr)
                                watchFaceConfigDataStore.applyFeatureFlags(flags)
                            } else {
                                watchFaceConfigDataStore.applyFeatureFlags(
                                    WatchFaceFeatureFlags(
                                        bioReactiveArc  = map.getBoolean("bioReactiveArc",  true),
                                        hrvBand         = map.getBoolean("hrvBand",          true),
                                        circadianSector = map.getBoolean("circadianSector",  false),
                                        thermalStrip    = map.getBoolean("thermalStrip",     true),
                                        coPilotTicker   = map.getBoolean("coPilotTicker",    true),
                                        uvZoneArc       = map.getBoolean("uvZoneArc",        false),
                                        recoveryScore   = map.getBoolean("recoveryScore",    false),
                                        focusCountdown  = map.getBoolean("focusCountdown",   false)
                                    )
                                )
                            }
                            Log.i(TAG, "Feature flags applied via DataItem")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying CONFIG_SYNC_FEATURES DataItem", e)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
