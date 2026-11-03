package com.shakeflash.app.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

data class BatterySnapshot(
    val levelPercent: Int,
    val isCharging: Boolean,
    val temperatureC: Float?,
    val powerSaveMode: Boolean,
    val optimized: Boolean,
)

/**
 * Battery / power facts shown in the UI. Values come straight from the sticky
 * ACTION_BATTERY_CHANGED broadcast, so nothing extra is registered or polled.
 */
object BatteryInfo {

    fun snapshot(context: Context): BatterySnapshot {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val intent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val temperatureTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE

        val percent = when {
            level < 0 || scale <= 0 -> -1
            else -> (level * 100f / scale).toInt().coerceIn(0, 100)
        }
        return BatterySnapshot(
            levelPercent = percent,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            temperatureC = if (temperatureTenths == Int.MIN_VALUE) null else temperatureTenths / 10f,
            powerSaveMode = powerManager?.isPowerSaveMode == true,
            optimized = powerManager?.isIgnoringBatteryOptimizations(context.packageName) != true,
        )
    }

    /**
     * Rough, deliberately conservative estimate of the extra drain caused by keeping the
     * accelerometer registered. Displayed as "≈" in the UI because real drain depends on
     * the sensor hub, screen and torch usage.
     */
    fun estimatedExtraDrainPerHour(lowPowerSensor: Boolean, torchOn: Boolean): Float {
        val sensor = if (lowPowerSensor) 0.4f else 1.0f
        val torch = if (torchOn) 7.5f else 0f
        return sensor + torch
    }
}
