package com.unlockedwear.wear.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.unlockedwear.protocol.DataLayerPaths
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.wear.complications.MissionRingComplicationService
import com.unlockedwear.wear.data.RingStateDataStore
import com.unlockedwear.wear.data.WatchFaceConfigDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Secondary Wearable Data Layer listener — handles CONFIG_SYNC DataItem events as a
 * fallback / secondary path alongside [com.unlockedwear.wear.data.DataSyncService].
 *
 * Both services listen on the same intent filter but only one is guaranteed to receive
 * a given event. Having CONFIG_SYNC handled in both maximises delivery reliability.
 */
@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataListenerService"
    }

    @Inject
    lateinit var watchFaceConfigDataStore: WatchFaceConfigDataStore

    @Inject
    lateinit var ringStateDataStore: RingStateDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == DataLayerPaths.CONFIG_SYNC) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val map = dataMapItem.dataMap

                    serviceScope.launch {
                        try {
                            val ringModeStr        = map.getString("ringMode", RingMode.HEALTH.name) ?: RingMode.HEALTH.name
                            val accentColorHex     = map.getString("accentColorHex", "#00D4FF") ?: "#00D4FF"
                            val topComplication    = map.getString("topComplication", "Heart Rate (BPM)") ?: "Heart Rate (BPM)"
                            val bottomComplication = map.getString("bottomComplication", "UV Index") ?: "UV Index"
                            val dialStyle          = map.getString("dialStyle", "Digital Modern") ?: "Digital Modern"
                            val ringMode = try { RingMode.valueOf(ringModeStr) } catch (_: Exception) { RingMode.HEALTH }

                            watchFaceConfigDataStore.applyConfig(
                                accentColorHex     = accentColorHex,
                                topComplication    = topComplication,
                                bottomComplication = bottomComplication,
                                dialStyle          = dialStyle,
                                ringMode           = ringMode
                            )
                            ringStateDataStore.setRingMode(ringMode)
                            MissionRingComplicationService.triggerImmediateRefresh(applicationContext)
                            Log.i(TAG, "CONFIG_SYNC applied via WearDataListenerService: ringMode=$ringMode")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying CONFIG_SYNC in WearDataListenerService", e)
                        }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // CONFIG_SYNC messages are handled by DataSyncService.onMessageReceived().
        // This service handles DataItem changes only.
        Log.d(TAG, "Message received at path: ${messageEvent.path} — deferred to DataSyncService")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
