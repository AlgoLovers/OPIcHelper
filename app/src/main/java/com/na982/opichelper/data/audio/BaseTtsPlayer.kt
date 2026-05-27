package com.na982.opichelper.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.TtsSpeakResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseTtsPlayer(
    protected val context: Context,
    private val locale: Locale,
    private val serviceName: String,
    protected val logTag: String
) : TtsPlayer {

    @Volatile
    protected var tts: TextToSpeech? = null
    @Volatile
    protected var isInitialized = false
    private val _isPlaying = AtomicBoolean(false)
    @Volatile
    protected var userSpeechRate: Float? = null

    private val speakMutex = Mutex()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(locale)
                isInitialized = result == TextToSpeech.LANG_AVAILABLE ||
                               result == TextToSpeech.LANG_COUNTRY_AVAILABLE
            } else {
                Log.e(logTag, "$serviceName 초기화 실패")
            }
        }
    }

    override fun isAvailable(): Boolean = isInitialized

    override fun setSpeechRate(rate: Float) {
        userSpeechRate = rate
    }

    override fun getServiceName(): String = serviceName

    override suspend fun speak(text: String): TtsSpeakResult {
        if (!isAvailable()) {
            Log.e(logTag, "$serviceName 사용 불가")
            return TtsSpeakResult.Unavailable
        }

        return speakMutex.withLock {
            val completed = AtomicBoolean(false)
            try {
                tts?.stop()
                var waitCount = 0
                while (tts?.isSpeaking == true && waitCount < 40) {
                    kotlinx.coroutines.delay(50)
                    waitCount++
                }
                if (waitCount > 0) {
                    kotlinx.coroutines.delay(150)
                }

                tts?.setSpeechRate(getSpeechRate())
                tts?.setPitch(getPitch())

                val utteranceId = "${logTag.lowercase()}_${System.currentTimeMillis()}"
                val startTime = System.currentTimeMillis()
                val completionDeferred = kotlinx.coroutines.CompletableDeferred<TtsSpeakResult>()
                var started = false

                fun safeComplete(result: TtsSpeakResult) {
                    if (completed.compareAndSet(false, true)) {
                        completionDeferred.complete(result)
                    }
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        started = true
                        _isPlaying.set(true)
                    }
                    override fun onDone(utteranceId: String?) {
                        _isPlaying.set(false)
                        val duration = System.currentTimeMillis() - startTime
                        safeComplete(TtsSpeakResult.Success(duration))
                    }
                    @Deprecated("Deprecated in Android API")
                    override fun onError(utteranceId: String?) {
                        Log.e(logTag, "$serviceName 재생 오류")
                        _isPlaying.set(false)
                        safeComplete(TtsSpeakResult.Error("재생 오류"))
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(logTag, "$serviceName 재생 오류: $errorCode")
                        _isPlaying.set(false)
                        safeComplete(TtsSpeakResult.Error("오류 코드: $errorCode"))
                    }
                })

                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    Log.e(logTag, "$serviceName speak() 실패 (ERROR 반환)")
                    _isPlaying.set(false)
                    return@withLock TtsSpeakResult.Error("speak() 반환 ERROR")
                }

                val startDeadline = System.currentTimeMillis() + 2000
                while (!started && System.currentTimeMillis() < startDeadline) {
                    kotlinx.coroutines.delay(50)
                }
                if (!started) {
                    Log.e(logTag, "$serviceName 재생 시작 타임아웃 — TTS 엔진 응답 없음")
                    _isPlaying.set(false)
                    return@withLock TtsSpeakResult.Error("재생 시작 타임아웃")
                }

                try {
                    kotlinx.coroutines.withTimeout(30000L) {
                        completionDeferred.await()
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.e(logTag, "$serviceName 재생 완료 타임아웃")
                    _isPlaying.set(false)
                    TtsSpeakResult.Timeout
                }
            } catch (e: CancellationException) {
                _isPlaying.set(false)
                tts?.stop()
                throw e
            } catch (e: Exception) {
                Log.e(logTag, "$serviceName 오류", e)
                _isPlaying.set(false)
                TtsSpeakResult.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    override fun stop() {
        try {
            tts?.stop()
            _isPlaying.set(false)
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 중지 실패", e)
        }
    }

    override fun pause() {
        try {
            tts?.stop()
            _isPlaying.set(false)
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 일시 중지 실패", e)
        }
    }

    override fun resume() {
        Log.w(logTag, "$serviceName 재개 불가 - Android TTS 제한")
    }

    override fun isPlaying(): Boolean = _isPlaying.get() || (tts?.isSpeaking == true)

    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 해제 중 오류", e)
        }
        tts = null
        isInitialized = false
        _isPlaying.set(false)
    }

    protected open fun getSpeechRate(): Float = userSpeechRate ?: 0.8f
    protected open fun getPitch(): Float = 1.0f
}
