package com.na982.opichelper.domain.audio

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ButtonStateObserver {
    
    // 한글 TTS 플레이어들 (폴백 순서)
    private val koreanTtsPlayers = listOf(
        samsungTtsPlayer,   // 1순위: 삼성 TTS (무료)
        // NaverTtsPlayer(context),    // 2순위: 네이버 클로바 (유료 - 월 9만원)
        // KakaoTtsPlayer(context),    // 3순위: 카카오 음성 (유료 - 요금 불명)
    )
    
    private var currentKoreanTtsIndex = 0
    
    // ButtonStateObserver 구현
    private val _isQuestionPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val _isAnswerPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var questionPlaybackCompletedCallback: (() -> Unit)? = null
    private var answerPlaybackCompletedCallback: (() -> Unit)? = null
    
    override val isQuestionPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()
    override val isAnswerPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()
    
    override fun onQuestionPlaybackCompleted(callback: () -> Unit) {
        questionPlaybackCompletedCallback = callback
    }
    
    override fun onAnswerPlaybackCompleted(callback: () -> Unit) {
        answerPlaybackCompletedCallback = callback
    }
    
    init {
        // Android 버전 정보 로깅
        val androidVersion = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Android 14+ (최신 기기)"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "Android 13 (중간 기기)"
            else -> "Android 12 이하 (구형 기기)"
        }
        Log.d("TtsOrchestrator", "📱 Android 버전 감지: ${Build.VERSION.SDK_INT} ($androidVersion)")
        Log.d("TtsOrchestrator", "📱 기기 정보: ${Build.MANUFACTURER} ${Build.MODEL}")
    }
    
    /**
     * 텍스트 언어를 감지하여 적절한 TTS 플레이어로 재생
     * @param text 재생할 텍스트
     * @param onComplete 재생 완료 콜백
     * @return 재생 성공 여부
     */
    suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        val isKorean = text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        
        Log.d("TtsOrchestrator", "🔍 언어 감지: 텍스트='${text.take(20)}...', 한글여부=$isKorean")
        
        return if (isKorean) {
            Log.d("TtsOrchestrator", "🇰🇷 한글 텍스트 감지 - 한글 TTS로 전달")
            speakKorean(text, onComplete)
        } else {
            Log.d("TtsOrchestrator", "🇺🇸 영문 텍스트 감지 - 영문 TTS로 전달")
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
        Log.d("TtsOrchestrator", "TTS 중지")
        try {
            // 모든 TTS 플레이어 중지
            googleTtsPlayer.stop()
            for (player in koreanTtsPlayers) {
                player.stop()
            }
            
            // 상태 초기화
            _isQuestionPlaying.value = false
            _isAnswerPlaying.value = false
            
            Log.d("TtsOrchestrator", "TTS 중지 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 중지 실패", e)
        }
    }
    
    /**
     * TTS 일시정지
     */
    fun pauseTts() {
        Log.d("TtsOrchestrator", "TTS 일시정지")
        stop()
    }
    
    /**
     * 모든 TTS 중지
     */
    fun stopAllTts() {
        Log.d("TtsOrchestrator", "모든 TTS 중지")
        stop()
    }
    
    /**
     * 하이라이트 초기화
     */
    fun clearHighlight() {
        Log.d("TtsOrchestrator", "하이라이트 초기화")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 답변 하이라이트 인덱스 설정
     */
    fun setAnswerHighlightIndex(index: Int?) {
        Log.d("TtsOrchestrator", "답변 하이라이트 인덱스 설정: $index")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 답변 한글 하이라이트 인덱스 설정
     */
    fun setAnswerKoHighlightIndex(index: Int?) {
        Log.d("TtsOrchestrator", "답변 한글 하이라이트 인덱스 설정: $index")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 녹음 하이라이트 인덱스 설정
     */
    fun setRecordingHighlightIndex(index: Int?) {
        Log.d("TtsOrchestrator", "녹음 하이라이트 인덱스 설정: $index")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 모든 TTS 플레이어 해제 (앱 종료 시 사용)
     */
    fun releaseAllPlayers() {
        Log.d("TtsOrchestrator", "모든 TTS 플레이어 해제")
        try {
            googleTtsPlayer.release()
            samsungTtsPlayer.release()
            Log.d("TtsOrchestrator", "모든 TTS 플레이어 해제 완료")
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
        Log.d("TtsOrchestrator", "🎯 speakWithHighlight 호출됨: '${text.take(30)}...'")
        
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        Log.d("TtsOrchestrator", "📝 문장 분리 완료: ${sentences.size}개 문장")
        
        for ((idx, sentence) in sentences.withIndex()) {
            Log.d("TtsOrchestrator", "🔤 문장 ${idx + 1}/${sentences.size}: '${sentence.take(20)}...'")
            
            onHighlight(idx)
            val isKorean = sentence.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
            
            Log.d("TtsOrchestrator", "🔍 문장 ${idx + 1} 언어 감지: 한글여부=$isKorean")
            
            val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
            if (isKorean) {
                Log.d("TtsOrchestrator", "🇰🇷 문장 ${idx + 1} 한글 TTS로 재생")
                // 한글 TTS가 모두 실패하면 안내만 하고 넘어감
                val success = speakKorean(sentence) { finished.complete(Unit) }
                if (!success) finished.complete(Unit)
            } else {
                Log.d("TtsOrchestrator", "🇺🇸 문장 ${idx + 1} 영문 TTS로 재생")
                speakEnglish(sentence) { finished.complete(Unit) }
            }
            finished.await()
            kotlinx.coroutines.delay(400L)
        }
        onHighlight(null)
        Log.d("TtsOrchestrator", "✅ speakWithHighlight 완료")
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
    
    /**
     * TTS 재생 완료까지 기다린 후 재생 시간 반환
     */
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
            // TTS 재생 완료까지 대기
            completionDeferred.await()
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
        Log.d("TtsOrchestrator", "TTS 일시 중지")
        try {
            // 현재 활성화된 TTS 플레이어 일시 중지
            googleTtsPlayer.pause()
            samsungTtsPlayer.pause()
            Log.d("TtsOrchestrator", "TTS 일시 중지 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 일시 중지 실패", e)
        }
    }
    
    /**
     * TTS 재개
     */
    fun resume() {
        Log.d("TtsOrchestrator", "TTS 재개")
        try {
            // 현재 활성화된 TTS 플레이어 재개
            googleTtsPlayer.resume()
            samsungTtsPlayer.resume()
            Log.d("TtsOrchestrator", "TTS 재개 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 재개 실패", e)
        }
    }
    
    /**
     * 통합 TTS 재생 함수 - 모든 TTS 재생에서 사용
     * @param text 재생할 텍스트
     * @param isKorean 한글 여부 (null이면 자동 감지)
     * @param rate 재생 속도 (1.0f가 기본)
     * @param onHighlight 하이라이트 콜백 (null이면 하이라이트 없음)
     * @param waitForCompletion 완료까지 대기 여부
     * @return 재생 시간 (밀리초)
     */
    suspend fun speakUnified(
        text: String,
        isKorean: Boolean? = null,
        rate: Float = 1.0f,
        onHighlight: ((Int?) -> Unit)? = null,
        waitForCompletion: Boolean = true
    ): Long {
        Log.d("TtsOrchestrator", "🎯 speakUnified 호출: '${text.take(30)}...', isKorean=$isKorean, rate=$rate, hasHighlight=${onHighlight != null}")
        
        val startTime = System.currentTimeMillis()
        val detectedKorean = isKorean ?: text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        
        return if (onHighlight != null) {
            // 하이라이트가 있는 경우 - 문장별 분리 재생
            speakWithHighlightUnified(text, detectedKorean, rate, onHighlight)
        } else {
            // 하이라이트가 없는 경우 - 단일 텍스트 재생
            speakSingleTextUnified(text, detectedKorean, rate, waitForCompletion)
        }
    }
    
    /**
     * 하이라이트가 있는 통합 TTS 재생
     */
    private suspend fun speakWithHighlightUnified(
        text: String,
        isKorean: Boolean,
        rate: Float,
        onHighlight: (Int?) -> Unit
    ): Long {
        val startTime = System.currentTimeMillis()
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        
        Log.d("TtsOrchestrator", "📝 문장 분리 완료: ${sentences.size}개 문장")
        
        for ((idx, sentence) in sentences.withIndex()) {
            Log.d("TtsOrchestrator", "🔤 문장 ${idx + 1}/${sentences.size}: '${sentence.take(20)}...'")
            
            onHighlight(idx)
            
            val completionDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
            val success = if (isKorean) {
                Log.d("TtsOrchestrator", "🇰🇷 문장 ${idx + 1} 한글 TTS로 재생")
                speakKorean(sentence) { completionDeferred.complete(Unit) }
            } else {
                Log.d("TtsOrchestrator", "🇺🇸 문장 ${idx + 1} 영문 TTS로 재생")
                speakEnglish(sentence) { completionDeferred.complete(Unit) }
            }
            
            if (success) {
                completionDeferred.await()
            } else {
                completionDeferred.complete(Unit)
            }
            
            kotlinx.coroutines.delay(400L)
        }
        
        onHighlight(null)
        val endTime = System.currentTimeMillis()
        Log.d("TtsOrchestrator", "✅ speakWithHighlightUnified 완료")
        return endTime - startTime
    }
    
    /**
     * 단일 텍스트 통합 TTS 재생
     */
    private suspend fun speakSingleTextUnified(
        text: String,
        isKorean: Boolean,
        rate: Float,
        waitForCompletion: Boolean
    ): Long {
        val startTime = System.currentTimeMillis()
        
        return if (waitForCompletion) {
            val completionDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
            
            val success = if (isKorean) {
                Log.d("TtsOrchestrator", "🇰🇷 한글 TTS 재생: '${text.take(20)}...'")
                speakKorean(text) { completionDeferred.complete(Unit) }
            } else {
                Log.d("TtsOrchestrator", "🇺🇸 영문 TTS 재생: '${text.take(20)}...'")
                speakEnglish(text) { completionDeferred.complete(Unit) }
            }
            
            if (success) {
                completionDeferred.await()
                val endTime = System.currentTimeMillis()
                endTime - startTime
            } else {
                completionDeferred.complete(Unit)
                0L
            }
        } else {
            // 완료 대기 없이 즉시 반환
            val success = if (isKorean) {
                speakKorean(text, null)
            } else {
                speakEnglish(text, null)
            }
            
            if (success) {
                val endTime = System.currentTimeMillis()
                endTime - startTime
            } else {
                0L
            }
        }
    }
} 