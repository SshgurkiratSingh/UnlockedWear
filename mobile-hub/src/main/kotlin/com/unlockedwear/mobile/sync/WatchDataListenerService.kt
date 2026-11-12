package com.unlockedwear.mobile.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.unlockedwear.mobile.data.SensorRepository
import com.unlockedwear.protocol.BiometricSnapshot
import com.unlockedwear.protocol.DataLayerPaths
import com.unlockedwear.protocol.ThermalFrame
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Android service listening for Wearable Data Layer events and messages broadcast
 * from the Galaxy Watch 8.
 *
 * Ingests raw and compensated thermal readings and biometric snapshots and persists
 * them into Room via [SensorRepository].
 */
@AndroidEntryPoint
class WatchDataListenerService : WearableListenerService() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                val path = uri.path ?: continue
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                when (path) {
                    DataLayerPaths.SENSOR_THERMAL -> {
                        val skinTemp = dataMap.getFloat("skinTempCelsius", 33.5f)
                        val ambientTemp = dataMap.getFloat("ambientTempCelsius", 24.0f)
                        val compensatedTemp = dataMap.getFloat("compensatedTempCelsius", skinTemp)
                        val timestampMs = dataMap.getLong("timestampMs", System.currentTimeMillis())
                        val status = dataMap.getInt("status", 1)

                        val frame = ThermalFrame(
                            skinTempCelsius = skinTemp,
                            ambientTempCelsius = ambientTemp,
                            compensatedTempCelsius = compensatedTemp,
                            timestampMs = timestampMs,
                            status = status
                        )
                        serviceScope.launch {
                            sensorRepository.insertThermalReading(frame)
                        }
                    }

                    DataLayerPaths.SENSOR_BIOMETRIC -> {
                        val hr = dataMap.getDouble("heartRateBpm", 70.0)
                        val hrv = dataMap.getDouble("hrvRmssdMs", 45.0)
                        val spo2 = dataMap.getInt("spo2Percent", 98)
                        val skinTemp = dataMap.getFloat("skinTempCelsius", 33.5f)
                        val ambientTemp = dataMap.getFloat("ambientTempCelsius", 24.0f)
                        val zScore = dataMap.getDouble("thermalZScore", 0.0)
                        val timestampMs = dataMap.getLong("timestampMs", System.currentTimeMillis())

                        val snapshot = BiometricSnapshot(
                            heartRateBpm = hr,
                            hrvRmssdMs = hrv,
                            spo2Percent = spo2,
                            skinTempCelsius = skinTemp,
                            ambientTempCelsius = ambientTemp,
                            thermalZScore = zScore,
                            timestampMs = timestampMs
                        )
                        serviceScope.launch {
                            sensorRepository.insertBiometricReading(snapshot)
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = messageEvent.data

        serviceScope.launch {
            try {
                when (path) {
                    DataLayerPaths.SENSOR_THERMAL -> {
                        val frame = json.decodeFromString<ThermalFrame>(String(payload, Charsets.UTF_8))
                        sensorRepository.insertThermalReading(frame)
                    }

                    DataLayerPaths.SENSOR_BIOMETRIC -> {
                        val snapshot = json.decodeFromString<BiometricSnapshot>(String(payload, Charsets.UTF_8))
                        sensorRepository.insertBiometricReading(snapshot)
                    }
                }
            } catch (e: Exception) {
                // Ignore parse errors from malformed direct messages
            }
        }
    }
}
