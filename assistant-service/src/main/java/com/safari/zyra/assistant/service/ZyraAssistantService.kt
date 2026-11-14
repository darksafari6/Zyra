package com.safari.zyra.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * User-started microphone-service shell. Callers must request RECORD_AUDIO first
 * and start this service while an Activity is visible. Wake-word capture is not
 * enabled by this service; it is deliberately reported as off until a licensed
 * local detector and its model are shipped.
 */
class ZyraAssistantService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            isRunning = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }
        return try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning = true
            START_NOT_STICKY
        } catch (_: SecurityException) {
            isRunning = false
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun buildNotification(): Notification {
        val talkIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?.putExtra(EXTRA_OPEN_VOICE, true)
        val talkPending = talkIntent?.let {
            PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val stopPending = PendingIntent.getService(
            this, 2, Intent(this, ZyraAssistantService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Zyra assistant")
            .setContentText("Assistant service active · Hey Zyra wake word is off")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(Notification.PRIORITY_LOW)
        if (talkPending != null) {
            builder.addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_btn_speak_now),
                "Talk", talkPending
            ).build())
        }
        builder.addAction(Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Stop", stopPending
        ).build())
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "Zyra assistant", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Controls an explicitly started Zyra assistant session." })
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.safari.zyra.action.START_ASSISTANT"
        const val ACTION_STOP = "com.safari.zyra.action.STOP_ASSISTANT"
        const val EXTRA_OPEN_VOICE = "com.safari.zyra.extra.OPEN_VOICE"
        const val NOTIFICATION_ID = 8301
        const val CHANNEL_ID = "zyra_assistant"
        @Volatile var isRunning: Boolean = false
            private set
    }
}