package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * 삼성 TTS 플레이어 (한글 전용, 오프라인)
 * BaseTtsPlayer를 상속하여 공통 로직 재사용
 * Android 버전별 TTS 성능 최적화 적용
 */
class SamsungTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.KOREAN,
    serviceName = "삼성 TTS",
    logTag = "SamsungTtsPlayer"
) {
    // TTS 속도를 1.0f로 고정 (한글)
    override fun getSpeechRate(): Float {
        return 1.0f
    }
    
    override fun getPitch(): Float {
        return 1.0f
    }
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int) -> Unit) {
        // 단일 TTS 플레이어는 하이라이트를 지원하지 않음
        speak(text, null)
    }
    
    override suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long {
        val start = System.currentTimeMillis()
        val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
        
        // TTS 속도를 1.0f로 고정 (한글)
        val fixedRate = 1.0f
        
        // 임시로 속도 설정
        val originalRate = getSpeechRate()
        tts?.setSpeechRate(fixedRate)
        
        speak(text) {
            // 원래 속도로 복원
            tts?.setSpeechRate(originalRate)
            finished.complete(Unit)
        }
        
        finished.await()
        return System.currentTimeMillis() - start
    }

    fun destroy() {
        release()
    }
    
    /**
     * TTS 플레이어 완전 해제 (앱 종료 시 사용)
     */
    override fun release() {
        try {
            Log.d(logTag, "Samsung TTS 플레이어 완전 해제 시작")
            super.release()
            Log.d(logTag, "Samsung TTS 플레이어 완전 해제 완료")
        } catch (e: Exception) {
            Log.e(logTag, "Samsung TTS 플레이어 해제 중 오류", e)
        }
    }
} 