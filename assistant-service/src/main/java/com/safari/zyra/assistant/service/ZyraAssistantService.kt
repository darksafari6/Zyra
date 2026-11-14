package com.safari.zyra.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safari.zyra.assistant.service.R

/**
 * User-started assistant lifetime shell. The service does not open the microphone
 * by itself; microphone capture belongs to an explicitly started voice/wake session.
 */
class ZyraAssistantService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TALK -> {
                val launch = packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    ?.putExtra(EXTRA_OPEN_VOICE, true)
                if (launch != null) startActivity(launch)
            }
        }
        return try {
            isRunning = true
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            START_STICKY
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Zyra assistant")
            .setContentText("Assistant ready · Wake word is off")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_btn_speak_now, "Talk", talkPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Zyra assistant", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Persistent controls for an explicitly started Zyra assistant session."
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.safari.zyra.assistant.START"
        const val ACTION_TALK = "com.safari.zyra.assistant.TALK"
        const val ACTION_STOP = "com.safari.zyra.assistant.STOP"
        const val EXTRA_OPEN_VOICE = "com.safari.zyra.OPEN_VOICE"
        const val CHANNEL_ID = "zyra_assistant"
        const val NOTIFICATION_ID = 8301
        @Volatile var isRunning: Boolean = false
            private set
    }
}
