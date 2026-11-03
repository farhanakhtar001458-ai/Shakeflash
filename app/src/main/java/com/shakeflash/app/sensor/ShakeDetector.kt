package com.shakeflash.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Thin Android wrapper around [ShakeProcessor].
 *
 * Sensor callbacks run on a dedicated [HandlerThread] — never on the main thread — so a
 * 50 Hz accelerometer stream can not cause UI jank while the app is open, and the work
 * keeps flowing while the screen is off.
 */
class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShake: () -> Unit,
) : SensorEventListener {

    private val processor = ShakeProcessor()
    private var sensor: Sensor? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile
    private var running = false

    val isRunning: Boolean get() = running
    val isSupported: Boolean get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    /**
     * Starts listening.
     *
     * @param sensitivity 0..100, applied immediately to the live detector.
     * @param lowPower true → SENSOR_DELAY_UI (~16 Hz), false → SENSOR_DELAY_GAME (~50 Hz).
     * @return true when the sensor was registered.
     */
    @Synchronized
    fun start(sensitivity: Int, lowPower: Boolean): Boolean {
        if (running) {
            updateSensitivity(sensitivity)
            return true
        }
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            Log.w(TAG, "No accelerometer on this device")
            return false
        }
        sensor = accelerometer
        processor.reset()
        processor.sensitivity = sensitivity

        val thread = HandlerThread("shakeflash-sensor").apply { start() }
        handlerThread = thread
        val callbackHandler = Handler(thread.looper)
        handler = callbackHandler

        val samplingPeriod = if (lowPower) {
            SensorManager.SENSOR_DELAY_UI
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }
        running = sensorManager.registerListener(this, accelerometer, samplingPeriod, callbackHandler)
        if (!running) {
            Log.w(TAG, "registerListener failed")
            teardownThread()
        }
        return running
    }

    /** Re-registers the listener when the user flips the battery-saver setting. */
    @Synchronized
    fun restart(sensitivity: Int, lowPower: Boolean) {
        stop()
        start(sensitivity, lowPower)
    }

    fun updateSensitivity(sensitivity: Int) {
        processor.sensitivity = sensitivity
    }

    @Synchronized
    fun stop() {
        if (running) {
            runCatching { sensorManager.unregisterListener(this) }
            running = false
        }
        processor.reset()
        teardownThread()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (values.size < 3) return
        val timestampMs = event.timestamp / 1_000_000L // ns -> ms, monotonic
        if (processor.onSample(values[0], values[1], values[2], timestampMs)) {
            Log.d(TAG, "Shake detected (magnitude=${processor.lastMagnitude})")
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun teardownThread() {
        handler = null
        handlerThread?.quitSafely()
        handlerThread = null
        sensor = null
    }

    private companion object {
        const val TAG = "ShakeDetector"
    }
}
