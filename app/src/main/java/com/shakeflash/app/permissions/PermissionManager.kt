package com.shakeflash.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * One place that knows which permissions ShakeFlash needs, why, and whether they are
 * currently granted. Used by the setup/permission card in the UI and by the service
 * before it starts listening.
 */
class PermissionManager(private val context: Context) {

    data class PermissionItem(
        val permission: String,
        val title: String,
        val reason: String,
        val granted: Boolean,
        val required: Boolean,
        val settingsHint: String? = null,
    )

    /** Permissions that show a runtime dialog on this OS version. */
    fun runtimePermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun missingRuntimePermissions(): List<String> = runtimePermissions().filterNot(::isGranted)

    fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasCameraPermission(): Boolean = isGranted(Manifest.permission.CAMERA)

    /** Notification permission only exists from Android 13; older versions are always fine. */
    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isGranted(Manifest.permission.POST_NOTIFICATIONS)

    /** True when Android may not show the foreground-service notification. */
    fun notificationBlocked(): Boolean = !hasNotificationPermission()

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Everything the UI shows in the permission card. Boot/foreground permissions are
     * install-time (or normal) permissions and are reported as granted — the card still
     * lists them so the user can see what the app declares.
     */
    fun statuses(): List<PermissionItem> = listOf(
        PermissionItem(
            permission = Manifest.permission.CAMERA,
            title = "Camera",
            reason = "Required by Android to control the LED through CameraManager.setTorchMode(). " +
                "ShakeFlash never takes photos or records video.",
            granted = hasCameraPermission(),
            required = true,
        ),
        PermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            title = "Notifications",
            reason = "Android 13+ requires this to show the ongoing “shake detection is on” " +
                "notification that keeps the service alive.",
            granted = hasNotificationPermission(),
            required = true,
            settingsHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) null else "Not needed on Android 12 and below",
        ),
        PermissionItem(
            permission = Manifest.permission.RECEIVE_BOOT_COMPLETED,
            title = "Start after reboot",
            reason = "Lets the service come back automatically after a restart, but only " +
                "while you have left shake detection enabled.",
            granted = isGranted(Manifest.permission.RECEIVE_BOOT_COMPLETED),
            required = false,
        ),
        PermissionItem(
            permission = Manifest.permission.FOREGROUND_SERVICE,
            title = "Foreground service",
            reason = "The service keeps a low-priority notification visible so Android " +
                "does not freeze shake detection in the background.",
            granted = isGranted(Manifest.permission.FOREGROUND_SERVICE),
            required = true,
        ),
    )

    fun allRequiredGranted(): Boolean = statuses().filter { it.required }.all { it.granted }

    companion object {
        const val CAMERA = Manifest.permission.CAMERA
        const val NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS
    }
}
