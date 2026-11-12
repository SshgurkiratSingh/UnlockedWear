package com.unlockedwear.protocol

import kotlinx.serialization.Serializable

/**
 * Common data packet exchanged between Wear OS watch (:wear-core) and Mobile Hub (:mobile-hub).
 */
@Serializable
data class SensorDataPacket(
    val timestamp: Long,
    val heartRate: Float? = null,
    val skinTemperatureCelsius: Float? = null,
    val stepCount: Long? = null,
    val status: String = "ACTIVE"
)

@Serializable
data class SyncCommand(
    val commandType: String,
    val payload: String = ""
)
