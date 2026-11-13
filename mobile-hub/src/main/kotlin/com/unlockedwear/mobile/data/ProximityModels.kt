package com.unlockedwear.mobile.data

import kotlinx.serialization.Serializable

enum class ProximityRadio { BLUETOOTH, WIFI }

@Serializable
enum class WallCondition(val label: String) {
    OPEN_AIR("Open air"),
    DRYWALL("Drywall"),
    BRICK("Brick"),
    CONCRETE("Concrete"),
    MULTI_WALL("Multiple walls")
}

@Serializable
data class CalibrationPoint(
    val distanceMeters: Float,
    val rssi: Int
)

@Serializable
data class CalibrationProfile(
    val condition: WallCondition,
    val points: List<CalibrationPoint> = emptyList(),
    val pathLossExponent: Float = 2.0f,
    val referenceRssiAtOneMeter: Float = -59.0f
)

data class ProximityDevice(
    val id: String,
    val name: String,
    val radio: ProximityRadio,
    val rssi: Int,
    val estimatedDistanceMeters: Float,
    val isEsp32: Boolean = false,
    val isEsp8266: Boolean = false,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class ProximityState(
    val devices: List<ProximityDevice> = emptyList(),
    val selectedCondition: WallCondition = WallCondition.OPEN_AIR,
    val calibrationProfiles: Map<WallCondition, CalibrationProfile> = emptyMap(),
    val isScanning: Boolean = false,
    val hasRequiredPermissions: Boolean = false,
    val errorMessage: String? = null,
    val lastScanAt: Long? = null
)
