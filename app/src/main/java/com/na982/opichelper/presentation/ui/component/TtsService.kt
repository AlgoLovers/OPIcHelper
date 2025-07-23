package com.na982.opichelper.presentation.ui.component

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.TtsPlayerManager

internal class TtsService : Service(), TtsPlayer {
    private val binder = TtsBinder()
    private val CHANNEL_ID = "TTS_FOREGROUND_CHANNEL"
    private val NOTIFICATION_ID = 1001
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var noisyReceiverRegistered = false
    
    // 새로운 TTS 매니저 사용
    private lateinit var ttsPlayerManager: TtsPlayerManager

    interface HighlightCallback {
        fun onQuestionHighlight(index: Int?)
        fun onAnswerHighlight(index: Int?)
    }
    internal var highlightCallback: HighlightCallback? = null
    fun setHighlightCallback(callback: HighlightCallback?) {
        highlightCallback = callback
    }

    companion object {
        const val ACTION_TTS_HIGHLIGHT = "com.na982.opichelper.ACTION_TTS_HIGHLIGHT"
        const val EXTRA_HIGHLIGHT_INDEX = "highlight_index"
        const val EXTRA_HIGHLIGHT_MODE = "highlight_mode" // "question" or "answer"
    }

    override fun onCreate() {
        super.onCreate()
        ttsPlayerManager = TtsPlayerManager(this)
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        Log.d("TtsService", "onDestroy() - TTS 서비스 종료")
        destroy()
        super.onDestroy()
    }

    /**
     * 서비스 리소스 정리 함수
     */
    fun destroy() {
        Log.d("TtsService", "destroy() - 리소스 정리 시작")
        
        try {
            // TTS 매니저 정리
            ttsPlayerManager.stop()
            
            // WakeLock 해제
            releaseWakeLock()
            
            // 오디오 포커스 해제
            abandonAudioFocus()
            
            // 노이즈 리시버 해제
            unregisterNoisyReceiver()
            
            // 하이라이트 콜백 해제
            highlightCallback = null
            
            // 포그라운드 서비스 중지
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            
            Log.d("TtsService", "destroy() - 리소스 정리 완료")
        } catch (e: Exception) {
            Log.e("TtsService", "destroy() - 리소스 정리 중 오류", e)
        }
    }

    inner class TtsBinder : Binder() {
        fun getService(): TtsService = this@TtsService
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TTS 재생 중")
            .setContentText("음성이 재생되고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TTS Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // WakeLock 관련
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TtsService::TTSWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10*60*1000L /*10분 제한*/)
        }
    }
    
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    // 오디오 포커스 관련
    private fun requestAudioFocus(): Boolean {
        audioManager ?: return false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stop()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            val result = audioManager!!.requestAudioFocus(focusRequest)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager!!.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stop()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    private fun abandonAudioFocus() {
        audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager!!.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager!!.abandonAudioFocus(null)
        }
    }

    // 이어폰 분리(BECOMING_NOISY) 처리
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                stop()
            }
        }
    }
    
    private fun registerNoisyReceiver() {
        if (!noisyReceiverRegistered) {
            registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            noisyReceiverRegistered = true
        }
    }
    
    private fun unregisterNoisyReceiver() {
        if (noisyReceiverRegistered) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverRegistered = false
        }
    }

    /**
     * 현재 사용 중인 한글 TTS 서비스 이름 반환
     */
    fun getCurrentKoreanTtsServiceName(): String {
        return ttsPlayerManager.getCurrentKoreanTtsServiceName()
    }
    
    override fun isAvailable(): Boolean {
        return true // TTS 매니저는 항상 사용 가능
    }
    
    override fun getServiceName(): String {
        return "TTS Manager"
    }
    
    override suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        return ttsPlayerManager.speak(text, onComplete)
    }
    
    override fun isPlaying(): Boolean {
        return ttsPlayerManager.isPlaying()
    }
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {
        ttsPlayerManager.speakWithHighlight(text, onHighlight)
    }
    
    override suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long {
        return ttsPlayerManager.speakAndGetDuration(text, isKorean, rate)
    }

    override fun stop() {
        Log.d("TtsService", "stopTts called")
        ttsPlayerManager.stop()
        releaseWakeLock()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        Log.d("TtsService", "stopTts completed")
    }
} 