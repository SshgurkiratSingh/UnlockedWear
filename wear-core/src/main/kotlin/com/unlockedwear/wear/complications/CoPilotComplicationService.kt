package com.unlockedwear.wear.complications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.ui.CoPilotActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wear OS Complication providing glanceable DeepSeek AI Co-Pilot briefings.
 *
 * Supported Complication Types:
 * - [ComplicationType.LONG_TEXT]: Displays the executive briefing headline and status.
 */
@AndroidEntryPoint
class CoPilotComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "CoPilotComplication"

        /**
         * Triggers an immediate refresh of all Co-Pilot complications.
         */
        fun triggerImmediateRefresh(context: Context) {
            try {
                val requester = ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, CoPilotComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh CoPilot complication", e)
            }
        }
    }

    @Inject
    lateinit var database: WearDatabase

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.LONG_TEXT) {
            return null
        }

        val briefing = try {
            database.briefingDao().getLatestBriefing()
        } catch (e: Exception) {
            null
        }

        val headline = briefing?.coPilotHeadline ?: "Mission ready • Tap for briefing"
        val priorityTask = briefing?.priorityTask ?: "DeepSeek Standby"

        val tapIntent = Intent(this, CoPilotActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            1002,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(headline).build(),
            contentDescription = PlainComplicationText.Builder("Co-Pilot: $headline").build()
        )
            .setTitle(PlainComplicationText.Builder(priorityTask).build())
            .setTapAction(tapPendingIntent)
            .build()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.LONG_TEXT) return null

        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder("DeepSeek: Solar peak ahead • Wear UV shield").build(),
            contentDescription = PlainComplicationText.Builder("Co-Pilot Preview").build()
        )
            .setTitle(PlainComplicationText.Builder("High UV Alert").build())
            .build()
    }
}
