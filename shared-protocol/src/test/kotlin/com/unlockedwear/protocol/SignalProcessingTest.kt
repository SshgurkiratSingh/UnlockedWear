package com.unlockedwear.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class SignalProcessingTest {

    @Test
    fun testEwmaCalculatorBasicConvergence() {
        val calculator = EwmaCalculator(alpha = 0.2)
        assertEquals(0.0, calculator.smoothedValue, 0.0001)

        // First sample initializes mean and smoothed value
        val result1 = calculator.update(30.0)
        assertEquals(30.0, result1.smoothedValue, 0.0001)
        assertEquals(30.0, result1.mean, 0.0001)
        assertEquals(0.0, result1.variance, 0.0001)
        assertEquals(0.0, result1.zScore, 0.0001)

        // Second sample updates mean
        // diff = 40.0 - 30.0 = 10.0
        // mean = 30.0 + 0.2 * 10.0 = 32.0
        val result2 = calculator.update(40.0)
        assertEquals(32.0, result2.smoothedValue, 0.0001)
        assertEquals(32.0, result2.mean, 0.0001)
        assertTrue(result2.variance > 0.0)
        assertTrue(result2.zScore > 0.0)

        // Reset
        calculator.reset()
        assertEquals(0.0, calculator.smoothedValue, 0.0001)
        assertEquals(false, calculator.isInitialized)
    }

    @Test
    fun testComputeRmssdKnownValues() {
        // Empty list or single element -> 0.0
        assertEquals(0.0, computeRmssd(emptyList()), 0.0001)
        assertEquals(0.0, computeRmssd(listOf(800.0)), 0.0001)

        // Given IBIs: [800, 810, 790]
        // diff1 = 810 - 800 = 10 -> diff1^2 = 100
        // diff2 = 790 - 810 = -20 -> diff2^2 = 400
        // mean squared diff = (100 + 400) / 2 = 250
        // sqrt(250) = ~15.8113883
        val ibiList = listOf(800.0, 810.0, 790.0)
        val rmssd = computeRmssd(ibiList)
        val expected = sqrt(250.0)
        assertEquals(expected, rmssd, 0.001)
    }

    @Test
    fun testCompensateSkinTemp() {
        // At reference ambient temperature (25.0 C), compensated equals skin temp
        val skinTemp = 34.0f
        val ambientAtRef = 25.0f
        assertEquals(34.0f, compensateSkinTemp(skinTemp, ambientAtRef), 0.001f)

        // In colder ambient (20.0 C), skin loses heat, compensation raises estimate
        // T = 34.0 + 0.18 * (25.0 - 20.0) = 34.0 + 0.18 * 5.0 = 34.0 + 0.9 = 34.9
        val coldAmbient = 20.0f
        assertEquals(34.9f, compensateSkinTemp(skinTemp, coldAmbient, beta = 0.18f), 0.001f)

        // In warmer ambient (30.0 C), skin gains heat, compensation lowers estimate
        // T = 34.0 + 0.18 * (25.0 - 30.0) = 34.0 - 0.9 = 33.1
        val warmAmbient = 30.0f
        assertEquals(33.1f, compensateSkinTemp(skinTemp, warmAmbient, beta = 0.18f), 0.001f)
    }

    @Test
    fun testComputeUvDoseOutdoorIntegration() {
        // Constant UVI = 4.0 for 1 hour (3600 seconds)
        // 1 UVI = 0.025 W/m² -> 4.0 * 0.025 = 0.1 W/m²
        // Dose = 0.1 W/m² * 3600 s = 360 J/m²
        val startTime = 1000000L
        val endTime = startTime + 3600L * 1000L
        val uviReadings = listOf(
            Pair(startTime, 4.0f),
            Pair(endTime, 4.0f)
        )
        // Outdoor throughout
        val isOutdoor = listOf(
            Pair(startTime, true)
        )

        val dose = computeUvDose(uviReadings, isOutdoor)
        assertEquals(360.0, dose, 0.1)

        // If indoor throughout, dose should be 0.0
        val indoorState = listOf(
            Pair(startTime, false)
        )
        val indoorDose = computeUvDose(uviReadings, indoorState)
        assertEquals(0.0, indoorDose, 0.001)
    }

    @Test
    fun testUvRiskLevelClassification() {
        assertEquals(UvRiskLevel.LOW, uvRiskLevel(0.0f))
        assertEquals(UvRiskLevel.LOW, uvRiskLevel(2.4f))
        assertEquals(UvRiskLevel.MODERATE, uvRiskLevel(3.0f))
        assertEquals(UvRiskLevel.MODERATE, uvRiskLevel(5.9f))
        assertEquals(UvRiskLevel.HIGH, uvRiskLevel(6.0f))
        assertEquals(UvRiskLevel.HIGH, uvRiskLevel(7.5f))
        assertEquals(UvRiskLevel.VERY_HIGH, uvRiskLevel(8.0f))
        assertEquals(UvRiskLevel.VERY_HIGH, uvRiskLevel(10.9f))
        assertEquals(UvRiskLevel.EXTREME, uvRiskLevel(11.0f))
        assertEquals(UvRiskLevel.EXTREME, uvRiskLevel(14.5f))
        // Negative clamped to LOW
        assertEquals(UvRiskLevel.LOW, uvRiskLevel(-1.0f))
    }
}
