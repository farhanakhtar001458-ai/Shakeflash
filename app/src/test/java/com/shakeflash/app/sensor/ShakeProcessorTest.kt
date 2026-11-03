package com.shakeflash.app.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the shake algorithm. The scenarios mirror what the phone actually
 * reports on the Z axis: gravity (9.81) plus a burst of linear acceleration.
 *
 * These tests are the reason [ShakeProcessor] has no Android dependencies.
 */
class ShakeProcessorTest {

    private companion object {
        const val EARTH = 9.81f
        const val SAMPLE_MS = 20L
    }

    /** Feeds a constant Z reading for a duration, counting accepted shakes. */
    private class Feeder(val processor: ShakeProcessor) {
        private var timeMs = 0L
        var shakes = 0
            private set

        fun hold(durationMs: Long, z: Float): Feeder {
            val samples = (durationMs / SAMPLE_MS).toInt()
            repeat(samples) {
                timeMs += SAMPLE_MS
                if (processor.onSample(0f, 0f, z, timeMs)) shakes++
            }
            return this
        }

        fun idle(durationMs: Long): Feeder = hold(durationMs, EARTH)
        fun atRest(durationMs: Long): Feeder = idle(durationMs)
    }

    private fun feeder(sensitivity: Int = 60) = Feeder(ShakeProcessor(sensitivity = sensitivity))

    /** A realistic two-beat shake: forward burst, reverse burst, done in ~500 ms. */
    private fun Feeder.realisticShake(): Feeder = this
        .hold(70, EARTH + 30f)
        .hold(80, EARTH)
        .hold(70, EARTH - 20f)
        .hold(80, EARTH)
        .hold(70, EARTH + 30f)
        .idle(300)

    @Test
    fun `one sharp knock does not toggle the torch`() {
        val feeder = feeder().idle(500).hold(80, EARTH + 35f).idle(400)
        assertEquals(0, feeder.shakes)
    }

    @Test
    fun `a realistic shake toggles exactly once`() {
        val feeder = feeder().idle(500)
        feeder.realisticShake()
        assertEquals(1, feeder.shakes)
    }

    @Test
    fun `a sustained acceleration (bump or car door) is not a shake`() {
        val feeder = feeder()
            .idle(500)
            .hold(600, EARTH + 20f)
            .idle(400)
        assertEquals(0, feeder.shakes)
    }

    @Test
    fun `walking jitter never triggers`() {
        val feeder = feeder().idle(500)
        repeat(10) {
            feeder.hold(100, EARTH + 4f).hold(100, EARTH - 4f)
        }
        assertEquals(0, feeder.shakes)
    }

    @Test
    fun `samples during the settle window are ignored`() {
        // Both bursts happen before SETTLE_MS has elapsed.
        val feeder = Feeder(ShakeProcessor(sensitivity = 100))
            .hold(50, EARTH)
            .hold(100, EARTH + 40f)
            .hold(80, EARTH - 20f)
            .hold(100, EARTH + 40f)
            .idle(400)
        assertEquals(0, feeder.shakes)
    }

    @Test
    fun `cooldown swallows a second shake inside one second`() {
        val feeder = feeder().idle(500)
        feeder.realisticShake()
        // Immediately shake again — still inside COOLDOWN_MS.
        feeder.realisticShake()
        assertEquals(1, feeder.shakes)
    }

    @Test
    fun `shaking again after the cooldown toggles again`() {
        val feeder = feeder().idle(500)
        feeder.realisticShake()
        feeder.idle(1_200)
        feeder.realisticShake()
        assertEquals(2, feeder.shakes)
    }

    @Test
    fun `low sensitivity ignores a gentle shake that max sensitivity accepts`() {
        val gentle = { sensitivity: Int ->
            val feeder = feeder(sensitivity).idle(500)
            feeder
                .hold(70, EARTH + 20f)
                .hold(80, EARTH)
                .hold(70, EARTH - 10f)
                .idle(300)
            feeder.shakes
        }
        assertEquals(0, gentle(20))
        assertEquals(1, gentle(100))
    }

    @Test
    fun `threshold mapping is bounded and monotonic`() {
        assertEquals(ShakeProcessor.MAX_THRESHOLD, ShakeProcessor.thresholdFor(0), 0.001f)
        assertEquals(ShakeProcessor.MIN_THRESHOLD, ShakeProcessor.thresholdFor(100), 0.001f)

        var previous = Float.MAX_VALUE
        for (sensitivity in 0..100) {
            val threshold = ShakeProcessor.thresholdFor(sensitivity)
            assertTrue("threshold must drop as sensitivity rises", threshold <= previous)
            previous = threshold
        }
        // Out-of-range values are clamped instead of crashing.
        assertEquals(ShakeProcessor.MAX_THRESHOLD, ShakeProcessor.thresholdFor(-40), 0.001f)
        assertEquals(ShakeProcessor.MIN_THRESHOLD, ShakeProcessor.thresholdFor(500), 0.001f)
    }

    @Test
    fun `reset clears any pending peaks`() {
        val processor = ShakeProcessor(sensitivity = 60)
        val feeder = Feeder(processor).idle(500)
        feeder.hold(70, EARTH + 35f) // first peak registered, not yet a shake
        processor.reset()
        feeder.idle(600).hold(70, EARTH + 35f).idle(400)
        assertEquals(0, feeder.shakes)
        assertFalse(processor.lastMagnitude.isNaN())
    }
}
