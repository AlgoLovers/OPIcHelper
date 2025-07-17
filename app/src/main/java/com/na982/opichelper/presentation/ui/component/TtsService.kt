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
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.PowerManager
import android.speech.tts.UtteranceProgressListener
import android.util.Log

class TtsService : Service(), TextToSpeech.OnInitListener {
    private val binder = TtsBinder()
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speakJob: Job? = null
    private val CHANNEL_ID = "TTS_FOREGROUND_CHANNEL"
    private val NOTIFICATION_ID = 1001
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var noisyReceiverRegistered = false

    companion object {
        const val ACTION_TTS_HIGHLIGHT = "com.na982.opichelper.ACTION_TTS_HIGHLIGHT"
        const val EXTRA_HIGHLIGHT_INDEX = "highlight_index"
        const val EXTRA_HIGHLIGHT_MODE = "highlight_mode" // "question" or "answer"
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isReady = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        speakJob?.cancel()
        tts?.shutdown()
        releaseWakeLock()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        super.onDestroy()
    }

    inner class TtsBinder : Binder() {
        fun getService(): TtsService = this@TtsService
    }

    fun speak(text: String, rate: Float = 0.8f, mode: String = "question") {
        if (!isReady) return
        Log.d("TtsService", "speak called: mode=$mode, text=$text")
        speakJob?.cancel()
        if (requestAudioFocus()) {
            acquireWakeLock()
            registerNoisyReceiver()
            speakJob = CoroutineScope(Dispatchers.Main).launch {
                val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                for (sentence in sentences) {
                    if (!isActive) return@launch
                    // TODO: highlight callback to ViewModel/UI (mode, sentenceIdx)
                    val utteranceId = "utt_${mode}_${System.currentTimeMillis()}"
                    tts?.setSpeechRate(rate)
                    val finished = CompletableDeferred<Unit>()
                    val listener = object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) { finished.complete(Unit) }
                        @Deprecated("Deprecated in Android API")
                        override fun onError(utteranceId: String?) { finished.complete(Unit) }
                        override fun onError(utteranceId: String?, errorCode: Int) { finished.complete(Unit) }
                    }
                    tts?.setOnUtteranceProgressListener(listener)
                    tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    finished.await()
                    if (!isActive) return@launch
                    delay(400L)
                }
                tts?.setOnUtteranceProgressListener(null)
                // TODO: highlight callback to ViewModel/UI (mode, -1)
                Log.d("TtsService", "speak finished: mode=$mode")
                releaseWakeLock()
                abandonAudioFocus()
                unregisterNoisyReceiver()
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    fun speakQuestion(text: String, rate: Float = 0.8f) = speak(text, rate, "question")
    fun speakAnswer(text: String, rate: Float = 0.8f) = speak(text, rate, "answer")

    fun speakBySentence(text: String, repeatCount: Int = 5, pauseRatio: Float = 1.5f, rate: Float = 0.8f) {
        if (!isReady) return
        Log.d("TtsService", "speakBySentence called: text=$text, repeatCount=$repeatCount")
        speakJob?.cancel()
        if (requestAudioFocus()) {
            acquireWakeLock()
            registerNoisyReceiver()
            speakJob = CoroutineScope(Dispatchers.Main).launch {
                val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                for (sentence in sentences) {
                    repeat(repeatCount) { i ->
                        if (!isActive) return@launch
                        // TODO: highlight callback to ViewModel/UI ("answer", sentenceIdx)
                        val utteranceId = "utt_abs_${System.currentTimeMillis()}_${i}"
                        tts?.setSpeechRate(rate)
                        val finished = CompletableDeferred<Unit>()
                        val listener = object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) { finished.complete(Unit) }
                            @Deprecated("Deprecated in Android API")
                            override fun onError(utteranceId: String?) { finished.complete(Unit) }
                            override fun onError(utteranceId: String?, errorCode: Int) { finished.complete(Unit) }
                        }
                        tts?.setOnUtteranceProgressListener(listener)
                        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                        finished.await()
                        if (!isActive) return@launch
                        delay(calcRestDuration(sentence, pauseRatio))
                    }
                }
                tts?.setOnUtteranceProgressListener(null)
                // TODO: highlight callback to ViewModel/UI ("answer", -1)
                Log.d("TtsService", "speakBySentence finished")
                releaseWakeLock()
                abandonAudioFocus()
                unregisterNoisyReceiver()
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    fun calcRestDuration(sentence: String, ratio: Float): Long {
        val baseDuration = (sentence.length * 50L).coerceAtLeast(800L)
        return (baseDuration * ratio).toLong()
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
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopTts()
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
                        stopTts()
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
                stopTts()
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

    // 하이라이트 인덱스 브로드캐스트 함수 (전체 제거)
    // private fun sendHighlightBroadcast(index: Int, mode: String) { ... }

    fun stopTts() {
        Log.d("TtsService", "stopTts called")
        speakJob?.cancel()
        tts?.stop()
        releaseWakeLock()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
} 