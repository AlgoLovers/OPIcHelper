package com.na982.opichelper.data.audio

import android.content.Context
import java.util.*

/**
 * Google TTS 플레이어 (영문 전용)
 * BaseTtsPlayer를 상속하여 공통 로직 재사용
 */
class GoogleTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.US,
    serviceName = "Google TTS",
    logTag = "GoogleTtsPlayer"
) {
    // 기본 설정 사용 (speechRate: 0.8f, pitch: 1.0f)
    
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
        release()
    }
} 