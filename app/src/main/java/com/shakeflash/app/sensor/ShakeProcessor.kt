package com.shakeflash.app.sensor

import kotlin.math.sqrt

/**
 * Framework-free shake math, kept separate from [ShakeDetector] so it can be unit tested
 * on the JVM (see `ShakeProcessorTest`).
 *
 * How a "shake" is recognised — designed to avoid accidental activations:
 *
 * 1. A low-pass filter estimates gravity; subtracting it leaves *linear* acceleration,
 *    so tilting or carrying the phone in a pocket does not look like a shake.
 * 2. The linear magnitude must cross the sensitivity threshold (a sharp spike).
 * 3. The signal must fall back below a release level before the next spike counts
 *    (`armed` flag). This rejects one long, sustained acceleration (a bump, a car ride).
 * 4. Two or more distinct spikes must land inside [windowMs] — a single knock is ignored.
 * 5. A [cooldownMs] lock-out after a detected shake prevents a double toggle.
 * 6. The first [SETTLE_MS] after registration is ignored (the user is still holding the
 *    phone after opening the app / unlocking it).
 *
 * All timestamps are milliseconds and must come from a monotonic clock.
 */
class ShakeProcessor(
    sensitivity: Int = DEFAULT_SENSITIVITY,
    private val requiredPeaks: Int = REQUIRED_PEAKS,
    private val windowMs: Long = WINDOW_MS,
    private val minPeakGapMs: Long = MIN_PEAK_GAP_MS,
    private val cooldownMs: Long = COOLDOWN_MS,
) {

    /** 0 = least sensitive (hard shakes only), 100 = most sensitive. */
    @Volatile
    var sensitivity: Int = sensitivity.coerceIn(0, 100)
        set(value) {
            field = value.coerceIn(0, 100)
        }

    private val gravity = FloatArray(3)
    private var gravityInitialised = false
    private val peaks = ArrayDeque<Long>()
    private var lastPeakMs = 0L
    private var lastShakeMs = Long.MIN_VALUE / 2
    private var armed = true
    private var firstSampleMs = -1L

    /** Magnitude of the most recent linear acceleration, in m/s². Handy for debugging. */
    var lastMagnitude: Float = 0f
        private set

    /** Current trigger threshold in m/s², derived from [sensitivity]. */
    val threshold: Float
        get() = thresholdFor(sensitivity)

    /** Call when (re)starting the sensor so stale peaks are dropped. */
    fun reset() {
        gravityInitialised = false
        gravity.fill(0f)
        peaks.clear()
        lastPeakMs = 0L
        lastShakeMs = Long.MIN_VALUE / 2
        armed = true
        firstSampleMs = -1L
        lastMagnitude = 0f
    }

    /**
     * Feeds one accelerometer sample.
     *
     * @param timestampMs sample time in milliseconds from a monotonic clock.
     * @return true exactly once per accepted shake gesture.
     */
    fun onSample(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        if (firstSampleMs < 0L) firstSampleMs = timestampMs

        if (!gravityInitialised) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            gravityInitialised = true
            return false
        }

        // Low-pass filter: gravity changes slowly, a shake changes fast.
        gravity[0] = GRAVITY_ALPHA * gravity[0] + (1f - GRAVITY_ALPHA) * x
        gravity[1] = GRAVITY_ALPHA * gravity[1] + (1f - GRAVITY_ALPHA) * y
        gravity[2] = GRAVITY_ALPHA * gravity[2] + (1f - GRAVITY_ALPHA) * z

        val linearX = x - gravity[0]
        val linearY = y - gravity[1]
        val linearZ = z - gravity[2]
        val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)
        lastMagnitude = magnitude

        val trigger = threshold
        val settled = timestampMs - firstSampleMs >= SETTLE_MS

        if (magnitude >= trigger) {
            if (armed && settled && timestampMs - lastPeakMs >= minPeakGapMs) {
                armed = false
                lastPeakMs = timestampMs
                return registerPeak(timestampMs)
            }
        } else if (magnitude <= trigger * RELEASE_RATIO) {
            // The gesture relaxed: the next spike may count again.
            armed = true
        }
        return false
    }

    private fun registerPeak(timestampMs: Long): Boolean {
        if (timestampMs - lastShakeMs < cooldownMs) return false

        peaks.addLast(timestampMs)
        while (peaks.isNotEmpty() && timestampMs - peaks.first() > windowMs) {
            peaks.removeFirst()
        }
        if (peaks.size >= requiredPeaks) {
            peaks.clear()
            lastShakeMs = timestampMs
            return true
        }
        return false
    }

    companion object {
        const val DEFAULT_SENSITIVITY = 60

        /** Threshold range in m/s² of linear acceleration (gravity excluded). */
        const val MAX_THRESHOLD = 26f // sensitivity 0  — only vigorous shakes
        const val MIN_THRESHOLD = 9.5f // sensitivity 100 — light flicks

        const val REQUIRED_PEAKS = 2
        const val WINDOW_MS = 450L
        const val MIN_PEAK_GAP_MS = 80L
        const val COOLDOWN_MS = 1_000L
        const val SETTLE_MS = 350L

        /** Weight of the previous gravity estimate (0.8 ≈ 200 ms time constant at 50 Hz). */
        private const val GRAVITY_ALPHA = 0.8f

        /** The signal must fall to this fraction of the threshold before re-arming. */
        private const val RELEASE_RATIO = 0.55f

        /**
         * Maps the 0..100 slider onto the trigger threshold. Higher sensitivity means a
         * lower threshold, i.e. easier to trigger.
         */
        fun thresholdFor(sensitivity: Int): Float {
            val fraction = sensitivity.coerceIn(0, 100) / 100f
            return MAX_THRESHOLD - (MAX_THRESHOLD - MIN_THRESHOLD) * fraction
        }
    }
}
