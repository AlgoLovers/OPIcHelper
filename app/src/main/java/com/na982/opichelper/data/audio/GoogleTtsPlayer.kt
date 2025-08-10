package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import android.util.Log
import java.util.Locale

/**
 * Google TTS 플레이어 (영문 전용)
 * BaseTtsPlayer를 상속하여 공통 로직 재사용
 * Android 버전별 TTS 성능 최적화 적용
 */
class GoogleTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.US,
    serviceName = "Google TTS",
    logTag = "GoogleTtsPlayer"
) {
    // TTS 속도를 0.7f로 고정 (영문)
    override fun getSpeechRate(): Float {
        return 0.7f
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
        
        // TTS 속도를 0.7f로 고정 (영문)
        val fixedRate = 0.7f
        
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
            Log.d(logTag, "Google TTS 플레이어 완전 해제 시작")
            super.release()
            Log.d(logTag, "Google TTS 플레이어 완전 해제 완료")
        } catch (e: Exception) {
            Log.e(logTag, "Google TTS 플레이어 해제 중 오류", e)
        }
    }
} 