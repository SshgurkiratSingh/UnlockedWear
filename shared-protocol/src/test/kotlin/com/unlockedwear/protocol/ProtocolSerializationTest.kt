package com.unlockedwear.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProtocolSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testThermalFrameSerialization() {
        val frame = ThermalFrame(
            skinTempCelsius = 34.2f,
            ambientTempCelsius = 22.5f,
            compensatedTempCelsius = 34.65f,
            timestampMs = 1700000000000L,
            status = ThermalSensorStatus.STATUS_ACCURACY_HIGH
        )
        val serialized = json.encodeToString(frame)
        val deserialized = json.decodeFromString<ThermalFrame>(serialized)

        assertEquals(frame.skinTempCelsius, deserialized.skinTempCelsius, 0.001f)
        assertEquals(frame.ambientTempCelsius, deserialized.ambientTempCelsius, 0.001f)
        assertEquals(frame.compensatedTempCelsius, deserialized.compensatedTempCelsius, 0.001f)
        assertEquals(frame.timestampMs, deserialized.timestampMs)
        assertEquals(frame.status, deserialized.status)
        assertEquals(true, deserialized.isValid)
    }

    @Test
    fun testBiometricSnapshotSerialization() {
        val snapshot = BiometricSnapshot(
            heartRateBpm = 68.5,
            hrvRmssdMs = 42.1,
            spo2Percent = 98,
            skinTempCelsius = 33.9f,
            ambientTempCelsius = 21.0f,
            thermalZScore = 0.45,
            timestampMs = 1700000000100L
        )
        val serialized = json.encodeToString(snapshot)
        val deserialized = json.decodeFromString<BiometricSnapshot>(serialized)

        assertEquals(snapshot.heartRateBpm, deserialized.heartRateBpm, 0.001)
        assertEquals(snapshot.hrvRmssdMs, deserialized.hrvRmssdMs, 0.001)
        assertEquals(snapshot.spo2Percent, deserialized.spo2Percent)
        assertEquals(snapshot.skinTempCelsius, deserialized.skinTempCelsius, 0.001f)
        assertEquals(snapshot.ambientTempCelsius, deserialized.ambientTempCelsius, 0.001f)
        assertEquals(snapshot.thermalZScore, deserialized.thermalZScore, 0.001)
    }

    @Test
    fun testDailyBriefingSerialization() {
        val briefing = DailyBriefing(
            ringRecommendedMode = RingMode.FOCUS,
            coPilotHeadline = "Optimal recovery: deep work block at 10am",
            priorityTaskId = "task-arch-001",
            recommendedFocusBlocksMin = 90,
            thermalRecoveryAssessment = "Autonomic stability high; thermal baseline neutral",
            alerts = listOf("UV peak at 1:00 PM (UVI 8.2)", "Hydration advisory")
        )
        val serialized = json.encodeToString(briefing)
        val deserialized = json.decodeFromString<DailyBriefing>(serialized)

        assertEquals(briefing.ringRecommendedMode, deserialized.ringRecommendedMode)
        assertEquals(briefing.coPilotHeadline, deserialized.coPilotHeadline)
        assertEquals(briefing.priorityTaskId, deserialized.priorityTaskId)
        assertEquals(briefing.recommendedFocusBlocksMin, deserialized.recommendedFocusBlocksMin)
        assertEquals(briefing.alerts.size, deserialized.alerts.size)
    }

    @Test
    fun testWeatherStateSerialization() {
        val weather = WeatherState(
            tempCelsius = 24.5f,
            humidity = 55.0f,
            uvIndex = 6.4f,
            condition = "Partly Cloudy",
            forecastMaxUvi = 8.1f,
            peakUvWindow = "11:30 AM - 2:00 PM",
            barometricTrendHpa = -1.2f
        )
        val serialized = json.encodeToString(weather)
        val deserialized = json.decodeFromString<WeatherState>(serialized)

        assertEquals(weather.tempCelsius, deserialized.tempCelsius, 0.001f)
        assertEquals(weather.uvIndex, deserialized.uvIndex, 0.001f)
        assertEquals(UvRiskLevel.HIGH, deserialized.currentRiskLevel)
        assertEquals(UvRiskLevel.VERY_HIGH, deserialized.peakRiskLevel)
    }

    @Test
    fun testTaskItemSerialization() {
        val task = TaskItem(
            id = "task-101",
            title = "Implement Wearable Data Layer Client",
            deadlineHrs = 4.5,
            cognitiveLoad = CognitiveLoad.HIGH
        )
        val serialized = json.encodeToString(task)
        val deserialized = json.decodeFromString<TaskItem>(serialized)

        assertEquals(task.id, deserialized.id)
        assertEquals(task.cognitiveLoad, deserialized.cognitiveLoad)
    }

    @Test
    fun testIntelligenceRequestSerialization() {
        val request = IntelligenceRequest(
            biometricTelemetry = BiometricSnapshot(
                heartRateBpm = 72.0,
                hrvRmssdMs = 45.0,
                spo2Percent = 99,
                skinTempCelsius = 34.0f,
                ambientTempCelsius = 22.0f,
                thermalZScore = 0.1
            ),
            environmentalContext = WeatherState(
                tempCelsius = 22.0f,
                humidity = 50f,
                uvIndex = 3.5f,
                condition = "Clear",
                forecastMaxUvi = 6.0f,
                peakUvWindow = "12:00 PM - 2:30 PM",
                barometricTrendHpa = 0.5f
            ),
            workloadQueue = listOf(
                TaskItem(id = "1", title = "Morning Standup", deadlineHrs = 1.0, cognitiveLoad = CognitiveLoad.LOW),
                TaskItem(id = "2", title = "Kernel Debugging", deadlineHrs = 3.0, cognitiveLoad = CognitiveLoad.HIGH)
            ),
            requestType = IntelligenceRequestType.MORNING_BRIEFING
        )

        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<IntelligenceRequest>(serialized)

        assertNotNull(deserialized.biometricTelemetry)
        assertEquals(2, deserialized.workloadQueue.size)
        assertEquals(IntelligenceRequestType.MORNING_BRIEFING, deserialized.requestType)
    }

    @Test
    fun testRingStateProgressFraction() {
        val state = RingState(
            mode = RingMode.POWER,
            currentValue = 75f,
            minValue = 0f,
            maxValue = 100f,
            formattedMetric = "75%"
        )
        assertEquals(0.75f, state.progressFraction, 0.001f)
        assertEquals("#4CAF50", state.effectiveColorHex)
    }
}
