package com.shakeflash.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Intents that send the user to the right system screen. We never try to flip these
 * switches programmatically: the user stays in control of battery/background policy.
 */
object SystemIntents {

    fun appDetails(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun notificationSettings(context: Context): Intent = Intent(
        Settings.ACTION_APP_NOTIFICATION_SETTINGS,
    )
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Battery optimisation list. Safe on every OEM; no special permission required. */
    fun batteryOptimizationSettings(): Intent = Intent(
        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** OEM "autostart / protected apps" screens where they exist, else app details. */
    fun autostartSettings(context: Context): Intent {
        val candidates = listOf(
            // Xiaomi / Redmi / POCO
            Intent().setComponent(
                android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                ),
            ),
            // Oppo / Realme
            Intent().setComponent(
                android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                ),
            ),
            // Vivo / iQOO
            Intent().setComponent(
                android.content.ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                ),
            ),
            // Huawei / Honor
            Intent().setComponent(
                android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                ),
            ),
            // Let the OS decide, then fall back to app details.
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                Uri.fromParts("package", context.packageName, null),
            ),
        )
        val resolved = candidates.firstOrNull {
            context.packageManager.resolveActivity(it, 0) != null
        } ?: candidates.last()
        return resolved.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun launch(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent) }
    }
}
