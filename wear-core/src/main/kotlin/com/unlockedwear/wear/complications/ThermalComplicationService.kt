package com.unlockedwear.wear.complications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.unlockedwear.wear.data.WearDatabase
import com.unlockedwear.wear.ui.ThermalDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wear OS Complication providing current skin temperature deviation (ΔT).
 *
 * Supported Complication Types:
 * - [ComplicationType.SHORT_TEXT]: Shows delta relative to baseline (e.g. "+0.3°", "-0.2°").
 */
@AndroidEntryPoint
class ThermalComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "ThermalComplication"

        /**
         * Triggers an immediate refresh of all Thermal complications.
         */
        fun triggerImmediateRefresh(context: Context) {
            try {
                val requester = ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, ThermalComplicationService::class.java)
                )
                requester.requestUpdateAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh Thermal complication", e)
            }
        }
    }

    @Inject
    lateinit var database: WearDatabase

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            return null
        }

        val latestReading = try {
            database.thermalDao().getLatest()
        } catch (e: Exception) {
            null
        }

        val delta = latestReading?.deltaFromBaseline ?: 0.0f
        val sign = if (delta >= 0f) "+" else ""
        val deltaFormatted = "$sign${String.format("%.1f", delta)}°C"

        val tapIntent = Intent(this, ThermalDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            1003,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(deltaFormatted).build(),
            contentDescription = PlainComplicationText.Builder("Skin Temp Delta: $deltaFormatted").build()
        )
            .setTitle(PlainComplicationText.Builder("Δ TEMP").build())
            .setTapAction(tapPendingIntent)
            .build()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("+0.2°C").build(),
            contentDescription = PlainComplicationText.Builder("Skin Temp Delta Preview: +0.2°C").build()
        )
            .setTitle(PlainComplicationText.Builder("Δ TEMP").build())
            .build()
    }
}
