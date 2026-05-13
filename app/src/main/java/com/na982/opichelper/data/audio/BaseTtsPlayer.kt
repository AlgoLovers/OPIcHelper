package com.na982.opichelper.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TTS 플레이어들의 공통 로직을 담은 베이스 클래스
 * Android TextToSpeech 기반 TTS 플레이어들의 공통 기능 제공
 */
abstract class BaseTtsPlayer(
    protected val context: Context,
    private val locale: Locale,
    private val serviceName: String,
    protected val logTag: String
) : TtsPlayer {
    
    protected var tts: TextToSpeech? = null
    protected var isInitialized = false
    @Volatile
    protected var _isPlaying = false
    
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
    
    override fun isAvailable(): Boolean {
        return isInitialized
    }
    
    override fun getServiceName(): String {
        return serviceName
    }
    
    override suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        return if (isAvailable()) {
            try {
                // stop() 이후 TTS 엔진이 완전히 정지될 때까지 대기
                var waitCount = 0
                while (tts?.isSpeaking == true && waitCount < 20) {
                    kotlinx.coroutines.delay(50)
                    waitCount++
                }

                // 기본 설정 적용
                tts?.setSpeechRate(getSpeechRate())
                tts?.setPitch(getPitch())

                val utteranceId = "${logTag.lowercase()}_${System.currentTimeMillis()}"
                val completionDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
                val completed = AtomicBoolean(false)
                var started = false

                fun safeComplete() {
                    if (completed.compareAndSet(false, true)) {
                        completionDeferred.complete(Unit)
                    }
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        started = true
                        _isPlaying = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isPlaying = false
                        onComplete?.invoke()
                        safeComplete()
                    }
                    @Deprecated("Deprecated in Android API")
                    override fun onError(utteranceId: String?) {
                        Log.e(logTag, "$serviceName 재생 오류")
                        _isPlaying = false
                        onComplete?.invoke()
                        safeComplete()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(logTag, "$serviceName 재생 오류: $errorCode")
                        _isPlaying = false
                        onComplete?.invoke()
                        safeComplete()
                    }
                })

                val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    Log.e(logTag, "$serviceName speak() 실패 (ERROR 반환)")
                    _isPlaying = false
                    safeComplete()
                    onComplete?.invoke()
                    return false
                }

                // onStart 콜백 대기 (최대 2초) — 실제 재생 시작 확인
                val startDeadline = System.currentTimeMillis() + 2000
                while (!started && System.currentTimeMillis() < startDeadline) {
                    kotlinx.coroutines.delay(50)
                }
                if (!started) {
                    Log.e(logTag, "$serviceName 재생 시작 타임아웃 — TTS 엔진 응답 없음")
                    _isPlaying = false
                    safeComplete()
                    onComplete?.invoke()
                    return false
                }

                // 재생 완료까지 대기 (최대 30초 안전 타임아웃)
                try {
                    kotlinx.coroutines.withTimeout(30000L) {
                        completionDeferred.await()
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.e(logTag, "$serviceName 재생 완료 타임아웃")
                    _isPlaying = false
                    onComplete?.invoke()
                }

                true
            } catch (e: Exception) {
                Log.e(logTag, "$serviceName 오류", e)
                _isPlaying = false
                onComplete?.invoke()
                false
            }
        } else {
            Log.e(logTag, "$serviceName 사용 불가")
            onComplete?.invoke()
            false
        }
    }
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {
        // 기본 구현: 하이라이트 없이 재생
        speak(text, null)
    }
    
    override suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long {
        val start = System.currentTimeMillis()
        val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
        val done = AtomicBoolean(false)

        speak(text) {
            if (done.compareAndSet(false, true)) {
                finished.complete(Unit)
            }
        }

        finished.await()
        return System.currentTimeMillis() - start
    }
    
    override fun stop() {
        try {
            tts?.stop()
            _isPlaying = false
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 중지 실패", e)
        }
    }
    
    override fun pause() {
        try {
            // Android TTS는 pause 기능이 없으므로 stop으로 대체
            tts?.stop()
            _isPlaying = false
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 일시 중지 실패", e)
        }
    }

    override fun resume() {
        try {
            // Android TTS는 resume 기능이 없으므로
            Log.w(logTag, "$serviceName 재개 불가 - Android TTS 제한")
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 재개 실패", e)
        }
    }
    
    override fun isPlaying(): Boolean {
        return _isPlaying || (tts?.isSpeaking == true)
    }
    
    override fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 해제 중 오류", e)
        }
        tts = null
        isInitialized = false
        _isPlaying = false
    }
    
    /**
     * 음성 속도 설정 (하위 클래스에서 오버라이드 가능)
     */
    protected open fun getSpeechRate(): Float = 0.8f
    
    /**
     * 음성 피치 설정 (하위 클래스에서 오버라이드 가능)
     */
    protected open fun getPitch(): Float = 1.0f
} 