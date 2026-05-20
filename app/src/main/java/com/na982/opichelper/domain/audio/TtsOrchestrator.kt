package com.na982.opichelper.domain.audio

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * TTS 오케스트레이터 (TTS 서비스 조율 및 폴백 관리)
 * 클린 아키텍처 원칙에 따라 여러 TTS 서비스들을 조율하고 폴백 처리
 * Android 버전별 TTS 성능 최적화 지원
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
    
    private val currentKoreanTtsIndex = AtomicInteger(0)
    
    init {
        // Android 버전 정보는 필요시 Build.VERSION.SDK_INT로 직접 확인
    }
    
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
        return googleTtsPlayer.speak(text, onComplete)
    }
    
    /**
     * 한글 TTS 재생 (폴백 시스템)
     */
    private suspend fun speakKorean(text: String, onComplete: (() -> Unit)?): Boolean {
        val startIndex = currentKoreanTtsIndex.get()
        for (i in startIndex until koreanTtsPlayers.size) {
            val player = koreanTtsPlayers[i]

            if (player.isAvailable()) {
                val success = player.speak(text, onComplete)
                if (success) {
                    currentKoreanTtsIndex.set(i)
                    return true
                } else {
                    Log.w("TtsOrchestrator", "한글 TTS 실패: ${player.getServiceName()}, 다음 서비스 시도")
                    currentKoreanTtsIndex.set(i + 1)
                }
            } else {
                Log.w("TtsOrchestrator", "한글 TTS 서비스 사용 불가: ${player.getServiceName()}, 다음 서비스 시도")
                currentKoreanTtsIndex.set(i + 1)
            }
        }

        Log.e("TtsOrchestrator", "모든 한글 TTS 서비스 실패 — 인덱스 리셋")
        currentKoreanTtsIndex.set(0)
        onComplete?.invoke()
        return false
    }
    
    /**
     * TTS 재생 중지
     */
    fun stop() {
        try {
            googleTtsPlayer.stop()
            for (player in koreanTtsPlayers) {
                player.stop()
            }
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 중지 실패", e)
        }
    }
    
    /**
     * 모든 TTS 플레이어 해제 (앱 종료 시 사용)
     */
    fun releaseAllPlayers() {
        try {
            googleTtsPlayer.release()
            samsungTtsPlayer.release()
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 플레이어 해제 실패", e)
        }
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
        val index = currentKoreanTtsIndex.get()
        return if (index < koreanTtsPlayers.size) {
            koreanTtsPlayers[index].getServiceName()
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
        val sentences = SentenceSplitter.split(text)

        try {
            for ((idx, sentence) in sentences.withIndex()) {
                onHighlight(idx)
                val isKorean = sentence.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }

                val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
                if (isKorean) {
                    val success = speakKorean(sentence) { finished.complete(Unit) }
                    if (!success) finished.complete(Unit)
                } else {
                    val success = speakEnglish(sentence) { finished.complete(Unit) }
                    if (!success) finished.complete(Unit)
                }
                try {
                    kotlinx.coroutines.withTimeout(15000L) {
                        finished.await()
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.e("TtsOrchestrator", "speakWithHighlight 문장 $idx 타임아웃")
                }
                kotlinx.coroutines.delay(400L)
            }
            onHighlight(null)
        } catch (e: kotlinx.coroutines.CancellationException) {
            stop()
            onHighlight(null)
            throw e
        }
    }
    
    /**
     * TTS 재생 후 재생 시간 반환
     */
    @Suppress("UNUSED_PARAMETER")
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
    
    /**
     * TTS 재생 완료까지 기다린 후 재생 시간 반환
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun speakAndWaitForCompletion(text: String, isKorean: Boolean, rate: Float): Long {
        val startTime = System.currentTimeMillis()
        val isKoreanText = text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }

        val completionDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()

        val success = if (isKoreanText) {
            speakKorean(text) { completionDeferred.complete(Unit) }
        } else {
            speakEnglish(text) { completionDeferred.complete(Unit) }
        }

        if (success) {
            try {
                kotlinx.coroutines.withTimeout(30000L) {
                    completionDeferred.await()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("TtsOrchestrator", "speakAndWaitForCompletion 타임아웃")
            }
            val endTime = System.currentTimeMillis()
            return endTime - startTime
        } else {
            completionDeferred.complete(Unit)
            return 0L
        }
    }
    
    /**
     * TTS 일시 중지
     */
    fun pause() {
        try {
            googleTtsPlayer.pause()
            samsungTtsPlayer.pause()
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 일시 중지 실패", e)
        }
    }
    
    /**
     * TTS 재개
     */
    fun resume() {
        try {
            googleTtsPlayer.resume()
            samsungTtsPlayer.resume()
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 재개 실패", e)
        }
    }
} 