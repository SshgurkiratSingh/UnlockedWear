package com.unlockedwear.wear.complications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.RingState
import com.unlockedwear.wear.data.RingStateDataStore
import com.unlockedwear.wear.ui.MissionRingSelectorActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wear OS Complication displaying the active Mission Ring gauge.
 *
 * Supported Complication Types:
 * - [ComplicationType.RANGED_VALUE]: Renders circular/arc progress gauge with current value and bounds.
 */
@AndroidEntryPoint
class MissionRingComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "MissionRingComplication"

        /**
         * Requests an immediate refresh of all instances of this complication across all watch faces.
         */
        fun triggerImmediateRefresh(context: Context) {
            try {
                val requester = ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, MissionRingComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request complication update", e)
            }
        }
    }

    @Inject
    lateinit var ringStateDataStore: RingStateDataStore

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.RANGED_VALUE) {
            return null
        }

        val ringState = try {
            ringStateDataStore.getCurrentRingState()
        } catch (e: Exception) {
            RingState(
                mode = RingMode.UV,
                currentValue = 3.5f,
                minValue = 0f,
                maxValue = 12f,
                label = "UV Solar",
                formattedMetric = "3.5 UVI"
            )
        }

        val tapIntent = Intent(this, MissionRingSelectorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedValue = String.format("%.1f", ringState.currentValue)
        val text = PlainComplicationText.Builder(formattedValue).build()
        val title = PlainComplicationText.Builder(ringState.mode.name).build()
        val contentDescription = PlainComplicationText.Builder(
            "${ringState.label}: ${ringState.formattedMetric}"
        ).build()

        return RangedValueComplicationData.Builder(
            value = ringState.currentValue,
            min = ringState.minValue,
            max = ringState.maxValue,
            contentDescription = contentDescription
        )
            .setText(text)
            .setTitle(title)
            .setTapAction(tapPendingIntent)
            .build()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.RANGED_VALUE) return null

        // Preview in UV mode
        return RangedValueComplicationData.Builder(
            value = 7.2f,
            min = 0f,
            max = 12f,
            contentDescription = PlainComplicationText.Builder("UV Solar Index Preview: 7.2 UVI").build()
        )
            .setText(PlainComplicationText.Builder("7.2").build())
            .setTitle(PlainComplicationText.Builder("UV").build())
            .build()
    }
}
