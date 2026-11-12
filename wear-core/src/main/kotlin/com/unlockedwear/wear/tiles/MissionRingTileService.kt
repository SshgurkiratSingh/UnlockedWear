package com.unlockedwear.wear.tiles

import android.content.Context
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.unlockedwear.protocol.RingMode
import com.unlockedwear.protocol.RingState
import com.unlockedwear.wear.data.RingStateDataStore
import com.unlockedwear.wear.ui.MainWearActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import javax.inject.Inject

/**
 * Wear OS Tile providing glanceable Mission Ring telemetry directly in the carousel.
 */
@AndroidEntryPoint
class MissionRingTileService : TileService() {

    companion object {
        private const val RESOURCES_VERSION = "1"
    }

    @Inject
    lateinit var ringStateDataStore: RingStateDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return serviceScope.future {
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


            val launchActivityClickable = ModifiersBuilders.Clickable.Builder()
                .setOnClick(
                    ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(packageName)
                                .setClassName(MainWearActivity::class.java.name)
                                .build()
                        )
                        .build()
                )
                .build()

            val primaryColor = when (ringState.mode) {
                RingMode.UV -> 0xFFFFD166.toInt()
                RingMode.HEALTH -> 0xFFFF3366.toInt()
                RingMode.FOCUS -> 0xFF00D4FF.toInt()
                RingMode.POWER -> 0xFF4CAF50.toInt()
            }

            val rootLayout = LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(launchActivityClickable)
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setAll(dp(16f))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText("UNLOCKED MISSION")
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(11f))
                                .setColor(argb(0xFF00D4FF.toInt()))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build()
                )
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(ringState.mode.name)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(18f))
                                .setColor(argb(0xFFFFFFFF.toInt()))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(ringState.formattedMetric)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(22f))
                                .setColor(argb(primaryColor))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build()
                )
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(ringState.label)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(11f))
                                .setColor(argb(0xFF94A3B8.toInt()))
                                .build()
                        )
                        .build()
                )
                .build()

            val protolayoutTimeline = TimelineBuilders.Timeline.fromLayoutElement(rootLayout)

            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(protolayoutTimeline)
                .setFreshnessIntervalMillis(60_000L)
                .build()

        }
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources> {
        return serviceScope.future {
            androidx.wear.tiles.ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
