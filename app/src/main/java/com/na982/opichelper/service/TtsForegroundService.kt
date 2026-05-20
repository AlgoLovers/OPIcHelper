package com.na982.opichelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.na982.opichelper.MainActivity
import com.na982.opichelper.R

@RequiresApi(Build.VERSION_CODES.O)
class TtsForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "tts_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.na982.opichelper.TTS_START"
        const val ACTION_STOP = "com.na982.opichelper.TTS_STOP"
        const val ACTION_UPDATE_SENTENCE = "com.na982.opichelper.TTS_UPDATE_SENTENCE"
        const val EXTRA_SENTENCE_EN = "sentence_en"
        const val EXTRA_SENTENCE_KO = "sentence_ko"

        private var currentSentenceEn: String? = null
        private var currentSentenceKo: String? = null

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

        fun updateSentenceIntent(
            context: android.content.Context,
            sentenceEn: String?,
            sentenceKo: String?
        ): Intent {
            return Intent(context, TtsForegroundService::class.java).apply {
                action = ACTION_UPDATE_SENTENCE
                putExtra(EXTRA_SENTENCE_EN, sentenceEn ?: "")
                putExtra(EXTRA_SENTENCE_KO, sentenceKo ?: "")
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
                val notification = createNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_UPDATE_SENTENCE -> {
                currentSentenceEn = intent.getStringExtra(EXTRA_SENTENCE_EN)?.takeIf { it.isNotEmpty() }
                currentSentenceKo = intent.getStringExtra(EXTRA_SENTENCE_KO)?.takeIf { it.isNotEmpty() }
                updateNotification()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("NewApi")
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
        return buildNotification(currentSentenceEn, currentSentenceKo)
    }

    private fun updateNotification() {
        val notification = buildNotification(currentSentenceEn, currentSentenceKo)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(sentenceEn: String?, sentenceKo: String?): Notification {
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
            Icon.createWithResource(this, R.drawable.ic_notification),
            "정지",
            stopIntent
        ).build()

        val bigText = buildString {
            if (!sentenceEn.isNullOrBlank()) append(sentenceEn)
            if (!sentenceKo.isNullOrBlank()) {
                if (isNotEmpty()) append("\n")
                append(sentenceKo)
            }
            if (isEmpty()) append("TTS 재생 중")
        }

        val contentText = if (!sentenceEn.isNullOrBlank()) sentenceEn else "TTS 재생 중"

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("OPIc Helper")
            .setContentText(contentText)
            .setStyle(
                Notification.BigTextStyle().bigText(bigText)
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }
}
