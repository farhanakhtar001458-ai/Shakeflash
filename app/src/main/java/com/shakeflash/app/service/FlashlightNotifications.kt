package com.shakeflash.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shakeflash.app.MainActivity
import com.shakeflash.app.R

/**
 * Builds the ongoing notification that makes [FlashlightService] a foreground service.
 * Low importance + silent: it informs, it never buzzes or pops up.
 */
object FlashlightNotifications {

    const val CHANNEL_ID = "shakeflash_detection"
    const val NOTIFICATION_ID = 1001

    private const val REQUEST_CONTENT = 10
    private const val REQUEST_TOGGLE = 11
    private const val REQUEST_STOP = 12

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_service_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_service_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        torchOn: Boolean,
        detecting: Boolean,
        errorText: String? = null,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CONTENT,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val toggleIntent = PendingIntent.getService(
            context,
            REQUEST_TOGGLE,
            Intent(context, FlashlightService::class.java).setAction(FlashlightService.ACTION_TOGGLE_TORCH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val stopIntent = PendingIntent.getService(
            context,
            REQUEST_STOP,
            Intent(context, FlashlightService::class.java).setAction(FlashlightService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = when {
            errorText != null -> context.getString(R.string.app_name)
            torchOn -> context.getString(R.string.notification_title_on)
            detecting -> context.getString(R.string.notification_title_off)
            else -> context.getString(R.string.notification_title_stopped)
        }
        val text = errorText ?: when {
            torchOn -> context.getString(R.string.notification_text_on)
            detecting -> context.getString(R.string.notification_text_off)
            else -> context.getString(R.string.notification_text_stopped)
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_flashlight)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                0,
                context.getString(
                    if (torchOn) R.string.notification_action_toggle_off
                    else R.string.notification_action_toggle_on,
                ),
                toggleIntent,
            )
            .addAction(0, context.getString(R.string.notification_action_stop), stopIntent)
            .build()
    }
}
