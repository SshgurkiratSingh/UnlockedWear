package com.unlockedwear.protocol

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * Encapsulates the output of an Exponentially Weighted Moving Average (EWMA) step,
 * including online running mean, variance, and standardized z-score.
 *
 * @property smoothedValue The current exponentially filtered estimate (equal to [mean]).
 * @property mean The exponentially smoothed running mean.
 * @property variance The exponentially smoothed running sample variance.
 * @property zScore The standardized score `(value - mean) / stdDev` measuring deviation from baseline.
 */
@Serializable
data class EwmaResult(
    val smoothedValue: Double,
    val mean: Double,
    val variance: Double,
    val zScore: Double
) {
    /**
     * Standard deviation computed as the square root of the running [variance].
     */
    val standardDeviation: Double
        get() = sqrt(variance.coerceAtLeast(0.0))
}

/**
 * Online streaming calculator for Exponentially Weighted Moving Average (EWMA) and
 * Exponentially Weighted Moving Variance (EWMV).
 *
 * Provides real-time baseline tracking and anomaly detection (z-score) for physiological
 * signals such as skin temperature deviations and heart rate surges.
 *
 * Algorithm details:
 * - Mean update: `mean_t = (1 - alpha) * mean_{t-1} + alpha * value`
 * - Variance update (West/Finch numerically stable formulation):
 *   `diff = value - mean_{t-1}`
 *   `variance_t = (1 - alpha) * (variance_{t-1} + alpha * diff^2)`
 * - Z-Score: `(value - mean_t) / sqrt(variance_t)`
 *
 * @param alpha Smoothing factor between `0.0` (infinite memory) and `1.0` (no smoothing).
 *              Higher values discount older observations faster.
 */
class EwmaCalculator(val alpha: Double) {

    init {
        require(alpha in 0.0..1.0) { "alpha must be within [0.0, 1.0], but was $alpha" }
    }

    /** The latest smoothed value (equivalent to [mean]). */
    var smoothedValue: Double = 0.0
        private set

    /** The current running exponentially weighted mean. */
    var mean: Double = 0.0
        private set

    /** The current running exponentially weighted variance. */
    var variance: Double = 0.0
        private set

    /** The z-score of the most recently processed observation. */
    var zScore: Double = 0.0
        private set

    /** Total number of valid samples ingested since initialization or last [reset]. */
    var sampleCount: Long = 0L
        private set

    /** Whether at least one observation has been ingested. */
    var isInitialized: Boolean = false
        private set

    /**
     * Updates the calculator with a new observation and returns the statistical snapshot.
     *
     * @param value The latest measurement.
     * @return An [EwmaResult] containing the updated smoothed value, mean, variance, and z-score.
     */
    fun update(value: Double): EwmaResult {
        if (!value.isFinite()) {
            return EwmaResult(smoothedValue, mean, variance, zScore)
        }

        if (!isInitialized) {
            mean = value
            smoothedValue = value
            variance = 0.0
            zScore = 0.0
            isInitialized = true
            sampleCount = 1L
        } else {
            sampleCount++
            val diff = value - mean
            // Update mean
            mean += alpha * diff
            smoothedValue = mean
            // Update exponentially weighted sample variance
            variance = (1.0 - alpha) * (variance + alpha * diff * diff)

            val stdDev = sqrt(variance.coerceAtLeast(0.0))
            zScore = if (stdDev > 1e-6) {
                (value - mean) / stdDev
            } else {
                0.0
            }
        }

        return EwmaResult(
            smoothedValue = smoothedValue,
            mean = mean,
            variance = variance,
            zScore = zScore
        )
    }

    /**
     * Convenience overload for [Float] input values.
     */
    fun update(value: Float): EwmaResult = update(value.toDouble())

    /**
     * Resets internal statistics back to uninitialized state.
     */
    fun reset() {
        smoothedValue = 0.0
        mean = 0.0
        variance = 0.0
        zScore = 0.0
        sampleCount = 0L
        isInitialized = false
    }
}

/**
 * Computes the Root Mean Square of Successive Differences (RMSSD) from a sequence of
 * consecutive Inter-Beat Intervals (IBIs) in milliseconds.
 *
 * RMSSD is the standard time-domain metric reflecting short-term heart rate variability (HRV)
 * and parasympathetic / vagal tone.
 *
 * Mathematical formula:
 * ```
 * RMSSD = sqrt( 1 / (N - 1) * sum_{i=0}^{N-2} (IBI_{i+1} - IBI_i)^2 )
 * ```
 *
 * @param ibiListMs List of consecutive peak-to-peak inter-beat intervals in milliseconds.
 * @return RMSSD in milliseconds, or `0.0` if fewer than 2 valid intervals are provided.
 */
fun computeRmssd(ibiListMs: List<Double>): Double {
    if (ibiListMs.size < 2) return 0.0

    var sumSquaredDiffs = 0.0
    var validPairs = 0

    for (i in 0 until ibiListMs.size - 1) {
        val current = ibiListMs[i]
        val next = ibiListMs[i + 1]

        // Only include physiologically plausible finite positive intervals (typically 250ms - 2000ms)
        if (current.isFinite() && next.isFinite() && current > 0.0 && next > 0.0) {
            val diff = next - current
            sumSquaredDiffs += diff * diff
            validPairs++
        }
    }

    return if (validPairs > 0) {
        sqrt(sumSquaredDiffs / validPairs)
    } else {
        0.0
    }
}

/**
 * Compensates wrist-measured skin temperature for ambient environmental air temperature variations.
 *
 * Wrist skin temperature sensors are susceptible to convective heat loss or gain depending on
 * environmental conditions. This linear model corrects the reading towards a neutral ambient
 * reference baseline:
 *
 * Formula:
 * ```
 * T_compensated = T_skin + beta * (T_ref - T_ambient)
 * ```
 *
 * @param skinTemp Raw skin temperature measurement from watch sensor in degrees Celsius.
 * @param ambientTemp Ambient air temperature in degrees Celsius (from on-watch ambient sensor or weather station).
 * @param beta Thermal coupling heat-transfer coefficient (default: `0.18f`).
 * @param tRef Neutral reference ambient temperature baseline in degrees Celsius (default: `25.0f`).
 * @return Compensated skin temperature in degrees Celsius.
 */
fun compensateSkinTemp(
    skinTemp: Float,
    ambientTemp: Float,
    beta: Float = 0.18f,
    tRef: Float = 25.0f
): Float {
    return skinTemp + beta * (tRef - ambientTemp)
}

/**
 * Computes the cumulative erythemal UV dose in Joules per square meter (J/m²) by integrating
 * time-series UV Index measurements filtered by outdoor presence.
 *
 * Erythemal UV irradiance reference (WHO / CIE):
 * `1.0 UV Index = 25 mW/m² = 0.025 W/m² = 0.025 J/(s·m²)`
 *
 * Integration formula:
 * ```
 * Dose (J/m²) = sum( avg(UVI_i, UVI_{i+1}) * 0.025 * delta_t_seconds )
 * ```
 *
 * @param uviReadings Chronological time series of pairs `(timestampMs, uvIndex)`.
 * @param isOutdoor Chronological time series of pairs `(timestampMs, isOutdoor)`.
 *                  If empty, all intervals are assumed to be outdoors.
 * @return Cumulative erythemal UV dose received in `J/m²`.
 */
fun computeUvDose(
    uviReadings: List<Pair<Long, Float>>,
    isOutdoor: List<Pair<Long, Boolean>>
): Double {
    if (uviReadings.size < 2) return 0.0

    val sortedUvi = uviReadings.sortedBy { it.first }
    val sortedOutdoor = isOutdoor.sortedBy { it.first }

    var totalDose = 0.0
    // Maximum gap between consecutive readings to integrate across (2 hours)
    val maxGapMs = 2L * 3600L * 1000L

    var outdoorIdx = 0

    for (i in 0 until sortedUvi.size - 1) {
        val (t1, uvi1) = sortedUvi[i]
        val (t2, uvi2) = sortedUvi[i + 1]

        val deltaMs = t2 - t1
        if (deltaMs <= 0 || deltaMs > maxGapMs) continue

        val midTime = t1 + deltaMs / 2

        // Determine outdoor status at midTime
        val outdoors = if (sortedOutdoor.isEmpty()) {
            true
        } else {
            while (outdoorIdx + 1 < sortedOutdoor.size && sortedOutdoor[outdoorIdx + 1].first <= midTime) {
                outdoorIdx++
            }
            sortedOutdoor[outdoorIdx].second
        }

        if (outdoors) {
            val avgUvi = ((uvi1 + uvi2) / 2.0f).coerceAtLeast(0.0f)
            val deltaSeconds = deltaMs / 1000.0
            // 1 UVI = 0.025 W/m²
            val irradianceWPerM2 = avgUvi * 0.025
            totalDose += irradianceWPerM2 * deltaSeconds
        }
    }

    return totalDose
}

/**
 * Classifies an Erythemal UV Index value into the corresponding WHO [UvRiskLevel].
 *
 * Risk brackets:
 * - 0.0 ..< 3.0: [UvRiskLevel.LOW] (#4CAF50, "Low")
 * - 3.0 ..< 6.0: [UvRiskLevel.MODERATE] (#FFEB3B, "Moderate")
 * - 6.0 ..< 8.0: [UvRiskLevel.HIGH] (#FF9800, "High")
 * - 8.0 ..< 11.0: [UvRiskLevel.VERY_HIGH] (#F44336, "Very High")
 * - 11.0+: [UvRiskLevel.EXTREME] (#9C27B0, "Extreme")
 *
 * @param uvIndex Raw or forecasted UV Index.
 * @return The matching [UvRiskLevel].
 */
fun uvRiskLevel(uvIndex: Float): UvRiskLevel {
    val clamped = uvIndex.coerceAtLeast(0.0f)
    return when {
        clamped < 3.0f -> UvRiskLevel.LOW
        clamped < 6.0f -> UvRiskLevel.MODERATE
        clamped < 8.0f -> UvRiskLevel.HIGH
        clamped < 11.0f -> UvRiskLevel.VERY_HIGH
        else -> UvRiskLevel.EXTREME
    }
}
