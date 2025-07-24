package com.na982.opichelper.data.audio

import android.content.Context
import java.util.*

/**
 * 삼성 TTS 플레이어 (한글 전용, 오프라인)
 * BaseTtsPlayer를 상속하여 공통 로직 재사용
 */
class SamsungTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.KOREAN,
    serviceName = "삼성 TTS",
    logTag = "SamsungTtsPlayer"
) {
    override fun getSpeechRate(): Float = 0.9f
    override fun getPitch(): Float = 1.05f
    
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
} 