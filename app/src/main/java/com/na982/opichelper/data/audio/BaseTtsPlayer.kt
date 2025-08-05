package com.na982.opichelper.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred

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
    private var initializationDeferred: CompletableDeferred<Boolean>? = null
    
    init {
        initializeTts()
    }
    
    private fun initializeTts() {
        val startTime = System.currentTimeMillis()
        Log.d(logTag, "$serviceName 초기화 시작")
        
        // 이전 초기화가 진행 중이면 취소
        initializationDeferred?.cancel()
        initializationDeferred = CompletableDeferred()
        
        tts = TextToSpeech(context) { status ->
            val initTime = System.currentTimeMillis() - startTime
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(locale)
                isInitialized = result == TextToSpeech.LANG_AVAILABLE || 
                               result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                Log.d(logTag, "$serviceName 초기화 완료: $isInitialized (${initTime}ms)")
                initializationDeferred?.complete(isInitialized)
            } else {
                Log.e(logTag, "$serviceName 초기화 실패 (${initTime}ms)")
                isInitialized = false
                initializationDeferred?.complete(false)
            }
        }
    }
    
    override suspend fun isAvailable(): Boolean {
        Log.d(logTag, "$serviceName 사용 가능 여부: $isInitialized (tts=${tts != null})")
        
        // TTS 객체가 null이거나 초기화되지 않았으면 재초기화 시도
        if (tts == null || !isInitialized) {
            Log.d(logTag, "$serviceName 재초기화 필요 - 초기화 시작")
            initializeTts()
            
            // 초기화 완료까지 대기
            try {
                val initResult = initializationDeferred?.await() ?: false
                Log.d(logTag, "$serviceName 재초기화 결과: $initResult")
                return initResult
            } catch (e: Exception) {
                Log.e(logTag, "$serviceName 재초기화 대기 중 오류", e)
                return false
            }
        }
        
        // TTS 객체가 여전히 null이면 false 반환
        if (tts == null) {
            Log.w(logTag, "$serviceName TTS 객체가 null")
            return false
        }
        
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
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int) -> Unit) {
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
            Log.d(logTag, "$serviceName 해제 시작")
            
            // 1. TTS 중지
            tts?.stop()
            Log.d(logTag, "$serviceName 중지 완료")
            
            // 2. TTS 완전 종료
            tts?.shutdown()
            Log.d(logTag, "$serviceName 종료 완료")
            
            // 3. 상태 초기화
            tts = null
            isInitialized = false
            _isPlaying = false
            
            Log.d(logTag, "$serviceName 해제 완료")
        } catch (e: Exception) {
            Log.e(logTag, "$serviceName 해제 중 오류", e)
            // 오류가 발생해도 상태는 초기화
            tts = null
            isInitialized = false
            _isPlaying = false
        }
    }
    
    /**
     * 음성 속도 설정 (하위 클래스에서 오버라이드 가능)
     */
    protected open fun getSpeechRate(): Float = 0.8f
    
    /**
     * 음성 피치 설정 (하위 클래스에서 오버라이드 가능)
     */
    protected open fun getPitch(): Float = 1.0f
    
    /**
     * TTS 재초기화 (release 후 재사용 시)
     */
    fun reinitializeTts() {
        Log.d(logTag, "$serviceName 재초기화 시작")
        
        // 기존 객체가 있으면 해제
        if (tts != null) {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.e(logTag, "$serviceName 기존 객체 해제 중 오류", e)
            }
        }
        
        // 상태 초기화
        tts = null
        isInitialized = false
        _isPlaying = false
        
        // 새로운 TTS 객체 생성
        initializeTts()
        
        Log.d(logTag, "$serviceName 재초기화 완료")
    }
} 