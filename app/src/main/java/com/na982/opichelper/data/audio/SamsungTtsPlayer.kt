package com.na982.opichelper.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.*
import java.util.*

/**
 * 삼성 TTS 플레이어 (한글 전용, 오프라인)
 * 클린 아키텍처 원칙에 따라 단일 책임을 가짐
 */
class SamsungTtsPlayer(private val context: Context) : TtsPlayer {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isPlaying = false
    
    init {
        initializeTts()
    }
    
    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                isInitialized = result == TextToSpeech.LANG_AVAILABLE || 
                               result == TextToSpeech.LANG_COUNTRY_AVAILABLE
                Log.d("SamsungTtsPlayer", "🟢 삼성 TTS 초기화 완료: $isInitialized")
            } else {
                Log.e("SamsungTtsPlayer", "🟢 삼성 TTS 초기화 실패")
            }
        }
    }
    
    override fun isAvailable(): Boolean {
        Log.d("SamsungTtsPlayer", "🟢 삼성 TTS 사용 가능 여부: $isInitialized")
        return isInitialized
    }
    
    override fun getServiceName(): String {
        return "삼성 TTS"
    }
    
    override suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        return if (isAvailable()) {
            try {
                Log.d("SamsungTtsPlayer", "🟢 삼성 TTS 시작: $text")
                isPlaying = true
                
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.05f)
                
                val utteranceId = "samsung_tts_${System.currentTimeMillis()}"
                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        Log.d("SamsungTtsPlayer", "🟢 삼성 TTS 재생 완료")
                        isPlaying = false
                        onComplete?.invoke()
                    }
                    @Deprecated("Deprecated in Android API")
                    override fun onError(utteranceId: String?) {
                        Log.e("SamsungTtsPlayer", "🟢 삼성 TTS 재생 오류")
                        isPlaying = false
                        onComplete?.invoke()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e("SamsungTtsPlayer", "🟢 삼성 TTS 재생 오류: $errorCode")
                        isPlaying = false
                        onComplete?.invoke()
                    }
                })
                
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                true
            } catch (e: Exception) {
                Log.e("SamsungTtsPlayer", "🟢 삼성 TTS 오류", e)
                isPlaying = false
                onComplete?.invoke()
                false
            }
        } else {
            Log.e("SamsungTtsPlayer", "🟢 삼성 TTS 사용 불가")
            onComplete?.invoke()
            false
        }
    }
    
    override fun stop() {
        tts?.stop()
        isPlaying = false
        Log.d("SamsungTtsPlayer", "🟢 삼성 TTS 중지")
    }
    
    override fun isPlaying(): Boolean {
        return isPlaying || (tts?.isSpeaking == true)
    }
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {
        // 단일 TTS 플레이어는 하이라이트를 지원하지 않음
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
    
    fun destroy() {
        tts?.shutdown()
        tts = null
        isInitialized = false
        isPlaying = false
    }
} 