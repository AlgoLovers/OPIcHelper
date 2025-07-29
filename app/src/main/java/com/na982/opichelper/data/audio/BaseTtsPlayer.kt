package com.na982.opichelper.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.*
import java.util.*

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
                Log.d(logTag, "$serviceName 초기화 완료: $isInitialized")
            } else {
                Log.e(logTag, "$serviceName 초기화 실패")
            }
        }
    }
    
    override fun isAvailable(): Boolean {
        Log.d(logTag, "$serviceName 사용 가능 여부: $isInitialized")
        return isInitialized
    }
    
    override fun getServiceName(): String {
        return serviceName
    }
    
    override suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        return if (isAvailable()) {
            try {
                Log.d(logTag, "$serviceName 시작: $text")
                _isPlaying = true
                
                // 기본 설정 적용
                tts?.setSpeechRate(getSpeechRate())
                tts?.setPitch(getPitch())
                
                val utteranceId = "${logTag.lowercase()}_${System.currentTimeMillis()}"
                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        Log.d(logTag, "$serviceName 재생 완료")
                        _isPlaying = false
                        onComplete?.invoke()
                    }
                    @Deprecated("Deprecated in Android API")
                    override fun onError(utteranceId: String?) {
                        Log.e(logTag, "$serviceName 재생 오류")
                        _isPlaying = false
                        onComplete?.invoke()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(logTag, "$serviceName 재생 오류: $errorCode")
                        _isPlaying = false
                        onComplete?.invoke()
                    }
                })
                
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
        
        speak(text) {
            finished.complete(Unit)
        }
        
        finished.await()
        return System.currentTimeMillis() - start
    }
    
    override fun stop() {
        Log.d(logTag, "$serviceName 중지")
        try {
            tts?.stop()
            _isPlaying = false
            Log.d(logTag, "$serviceName 중지 완료")
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 중지 실패", e)
        }
    }
    
    override fun pause() {
        Log.d(logTag, "$serviceName 일시 중지")
        try {
            // Android TTS는 pause 기능이 없으므로 stop으로 대체
            // 실제로는 현재 재생 위치를 저장하고 나중에 재개할 수 있지만
            // 현재 구현에서는 단순히 중지
            tts?.stop()
            _isPlaying = false
            Log.d(logTag, "$serviceName 일시 중지 완료")
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 일시 중지 실패", e)
        }
    }
    
    override fun resume() {
        Log.d(logTag, "$serviceName 재개")
        try {
            // Android TTS는 resume 기능이 없으므로
            // 현재 구현에서는 재개할 수 없음
            // 사용자에게 알림
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