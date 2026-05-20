package com.na982.opichelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.graphics.drawable.Icon
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.na982.opichelper.MainActivity
import com.na982.opichelper.R

class TtsForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "tts_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.na982.opichelper.TTS_START"
        const val ACTION_STOP = "com.na982.opichelper.TTS_STOP"

        fun startIntent(context: android.content.Context): Intent {
            return Intent(context, TtsForegroundService::class.java).apply {
                action = ACTION_START
            }
        }

        fun stopIntent(context: android.content.Context): Intent {
            return Intent(context, TtsForegroundService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TTS 재생",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "백그라운드 TTS 재생 상태 표시"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            TtsForegroundService.stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_launcher_foreground),
            "정지",
            stopIntent
        ).build()

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("OPIc Helper")
            .setContentText("TTS 재생 중")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }
}
