package com.na982.opichelper.domain.audio

import android.content.Context
import android.util.Log
import javax.inject.Inject

/**
 * TTS 오케스트레이터 (TTS 서비스 조율 및 폴백 관리)
 * 클린 아키텍처 원칙에 따라 여러 TTS 서비스들을 조율하고 폴백 처리
 */
class TtsOrchestrator @Inject constructor(
    private val context: Context,
    private val googleTtsPlayer: TtsPlayer,
    private val samsungTtsPlayer: TtsPlayer
) {
    
    // 한글 TTS 플레이어들 (폴백 순서)
    private val koreanTtsPlayers = listOf(
        samsungTtsPlayer,   // 1순위: 삼성 TTS (무료)
        // NaverTtsPlayer(context),    // 2순위: 네이버 클로바 (유료 - 월 9만원)
        // KakaoTtsPlayer(context),    // 3순위: 카카오 음성 (유료 - 요금 불명)
    )
    
    private var currentKoreanTtsIndex = 0
    
    /**
     * 텍스트 언어를 감지하여 적절한 TTS 플레이어로 재생
     * @param text 재생할 텍스트
     * @param onComplete 재생 완료 콜백
     * @return 재생 성공 여부
     */
    suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        val isKorean = text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        
        return if (isKorean) {
            speakKorean(text, onComplete)
        } else {
            speakEnglish(text, onComplete)
        }
    }
    
    /**
     * 영문 TTS 재생 (Google TTS)
     */
    private suspend fun speakEnglish(text: String, onComplete: (() -> Unit)?): Boolean {
        Log.d("TtsOrchestrator", "🇺🇸 영문 TTS 재생: $text")
        return googleTtsPlayer.speak(text, onComplete)
    }
    
    /**
     * 한글 TTS 재생 (폴백 시스템)
     */
    private suspend fun speakKorean(text: String, onComplete: (() -> Unit)?): Boolean {
        Log.d("TtsOrchestrator", "🇰🇷 한글 TTS 재생 시작 (현재 서비스: ${getCurrentKoreanTtsServiceName()})")
        
        // 현재 서비스부터 순차적으로 시도
        for (i in currentKoreanTtsIndex until koreanTtsPlayers.size) {
            val player = koreanTtsPlayers[i]
            Log.d("TtsOrchestrator", "🇰🇷 시도 중: ${player.getServiceName()} (인덱스: $i)")
            
            if (player.isAvailable()) {
                Log.d("TtsOrchestrator", "🇰🇷 ${player.getServiceName()} 사용 가능, 재생 시도")
                val success = player.speak(text, onComplete)
                if (success) {
                    currentKoreanTtsIndex = i // 성공한 서비스로 업데이트
                    Log.d("TtsOrchestrator", "🇰🇷 한글 TTS 성공: ${player.getServiceName()}")
                    return true
                } else {
                    Log.w("TtsOrchestrator", "🇰🇷 한글 TTS 실패: ${player.getServiceName()}, 다음 서비스 시도")
                    currentKoreanTtsIndex = i + 1
                }
            } else {
                Log.w("TtsOrchestrator", "🇰🇷 한글 TTS 서비스 사용 불가: ${player.getServiceName()}, 다음 서비스 시도")
                currentKoreanTtsIndex = i + 1
            }
        }
        
        // 모든 서비스 실패
        Log.e("TtsOrchestrator", "🇰🇷 모든 한글 TTS 서비스 실패")
        onComplete?.invoke()
        return false
    }
    
    /**
     * TTS 재생 중지
     */
    fun stop() {
        googleTtsPlayer.stop()
        for (player in koreanTtsPlayers) {
            player.stop()
        }
        Log.d("TtsOrchestrator", "모든 TTS 중지")
    }
    
    /**
     * 현재 재생 중인지 확인
     */
    fun isPlaying(): Boolean {
        return googleTtsPlayer.isPlaying() || koreanTtsPlayers.any { player -> player.isPlaying() }
    }
    
    /**
     * 현재 사용 중인 한글 TTS 서비스 이름 반환
     */
    fun getCurrentKoreanTtsServiceName(): String {
        return if (currentKoreanTtsIndex < koreanTtsPlayers.size) {
            koreanTtsPlayers[currentKoreanTtsIndex].getServiceName()
        } else {
            "없음"
        }
    }
    
    /**
     * 사용 가능한 한글 TTS 서비스 목록 반환
     */
    fun getAvailableKoreanTtsServices(): List<String> {
        return koreanTtsPlayers.mapNotNull { player -> 
            if (player.isAvailable()) player.getServiceName() else null 
        }
    }
    
    /**
     * 한글 TTS 서비스 상태 정보 반환
     */
    fun getKoreanTtsServiceStatus(): List<Pair<String, Boolean>> {
        return koreanTtsPlayers.map { player -> 
            player.getServiceName() to player.isAvailable() 
        }
    }
    
    /**
     * 문장별 하이라이트와 함께 TTS 재생
     */
    suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        
        for ((idx, sentence) in sentences.withIndex()) {
            onHighlight(idx)
            val isKorean = sentence.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
            val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
            if (isKorean) {
                // 한글 TTS가 모두 실패하면 안내만 하고 넘어감
                val success = speakKorean(sentence) { finished.complete(Unit) }
                if (!success) finished.complete(Unit)
            } else {
                speakEnglish(sentence) { finished.complete(Unit) }
            }
            finished.await()
            kotlinx.coroutines.delay(400L)
        }
        onHighlight(null)
    }
    
    /**
     * TTS 재생 후 재생 시간 반환
     */
    suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long {
        val startTime = System.currentTimeMillis()
        val isKoreanText = text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        
        val success = if (isKoreanText) {
            speakKorean(text, null)
        } else {
            speakEnglish(text, null)
        }
        
        val endTime = System.currentTimeMillis()
        return if (success) (endTime - startTime) else 0L
    }
} 