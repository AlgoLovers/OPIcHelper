package com.na982.opichelper.domain.audio

import android.content.Context
import android.os.Build
import android.util.Log
import com.na982.opichelper.data.audio.BaseTtsPlayer
import com.na982.opichelper.domain.button.ButtonStateObserver
import com.na982.opichelper.domain.manager.TtsHealthMonitor
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TTS 오케스트레이터 (TTS 서비스 조율 및 폴백 관리)
 * 클린 아키텍처 원칙에 따라 여러 TTS 서비스들을 조율하고 폴백 처리
 * Android 버전별 TTS 성능 최적화 지원
 */
class TtsOrchestrator @Inject constructor(
    private val context: Context,
    private val googleTtsPlayer: TtsPlayer,
    private val samsungTtsPlayer: TtsPlayer,
    private val ttsHealthMonitor: TtsHealthMonitor,
    private val appStateManager: AppStateManager
) : ButtonStateObserver {
    
    // 한글 TTS 플레이어들 (폴백 순서)
    private val koreanTtsPlayers = listOf(
        samsungTtsPlayer,   // 1순위: 삼성 TTS (무료)
        // NaverTtsPlayer(context),    // 2순위: 네이버 클로바 (유료 - 월 9만원)
        // KakaoTtsPlayer(context),    // 3순위: 카카오 음성 (유료 - 요금 불명)
    )
    
    private var currentKoreanTtsIndex = 0
    
    // ButtonStateObserver 구현
    override fun onButtonStateChanged(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction, newState: com.na982.opichelper.domain.entity.ButtonState) {
        // TTS 관련 버튼 상태 변경 시 처리
        when (buttonFunction) {
            is com.na982.opichelper.domain.entity.ButtonFunction.QuestionPlay -> {
                Log.d("TtsOrchestrator", "질문 재생 버튼 상태 변경: $newState")
            }
            is com.na982.opichelper.domain.entity.ButtonFunction.AnswerPlay -> {
                Log.d("TtsOrchestrator", "답변 재생 버튼 상태 변경: $newState")
            }
            else -> {
                // 다른 버튼들은 무시
            }
        }
    }
    
    override fun onAllButtonsReset() {
        Log.d("TtsOrchestrator", "모든 버튼 초기화")
        // TTS 중지
        stop()
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
     * TTS 초기화 (모니터링 시작 포함)
     */
    fun initialize() {
        Log.d("TtsOrchestrator", "TTS 초기화 시작")
        try {
            // TTS 모니터링 시작 및 복구 콜백 설정
            ttsHealthMonitor.onTtsRecoveryNeeded = { 
                CoroutineScope(Dispatchers.Main).launch { recoverTts() }
            }
            ttsHealthMonitor.startMonitoring()
            Log.d("TtsOrchestrator", "TTS 초기화 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 초기화 실패", e)
        }
    }
    
    /**
     * TTS 슬립 상태 복구
     */
    private suspend fun recoverTts() {
        try {
            Log.d("TtsOrchestrator", "TTS 슬립 상태 복구 시작")
            
            // 1. 현재 TTS 중지
            stop()
            
            // 2. 잠시 대기 (TTS 서비스 안정화)
            kotlinx.coroutines.delay(500)
            
            // 3. TTS 재초기화
            initialize()
            
            // 4. 마지막 재생 요청 복구
            val currentState = appStateManager.state.value
            val currentQaItem = currentState.currentQaItem
            
            if (currentQaItem != null) {
                if (currentState.isQuestionPlaying) {
                    // 질문 재생 복구
                    if (currentQaItem.questionEn.isNotEmpty()) {
                        Log.d("TtsOrchestrator", "질문 재생 복구: ${currentQaItem.questionEn}")
                        speak(currentQaItem.questionEn, null)
                    }
                } else if (currentState.isAnswerPlaying) {
                    // 답변 재생 복구 - 첫 번째 답변 문장 사용
                    val firstAnswerSentence = currentQaItem.answerEnSentences.firstOrNull()
                    if (firstAnswerSentence != null) {
                        Log.d("TtsOrchestrator", "답변 재생 복구: $firstAnswerSentence")
                        speak(firstAnswerSentence, null)
                    }
                }
            }
            
            // 5. TTS 활동 시간 업데이트
            ttsHealthMonitor.updateTtsActivity()
            
            Log.d("TtsOrchestrator", "TTS 슬립 상태 복구 완료")
            
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 슬립 상태 복구 실패", e)
        }
    }
    
    /**
     * 텍스트 언어를 감지하여 적절한 TTS 플레이어로 재생
     * @param text 재생할 텍스트
     * @param onComplete 재생 완료 콜백
     * @return 재생 성공 여부
     */
    suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        // TTS 활동 시간 업데이트 (모니터링용)
        ttsHealthMonitor.updateTtsActivity()
        
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
        
        // 재시도 로직 포함
        var retryCount = 0
        val maxRetries = 3
        
        while (retryCount < maxRetries) {
            try {
                if (googleTtsPlayer.isAvailable()) {
                    Log.d("TtsOrchestrator", "🇺🇸 Google TTS 사용 가능, 재생 시도")
                    val success = googleTtsPlayer.speak(text, onComplete)
                    if (success) {
                        Log.d("TtsOrchestrator", "🇺🇸 영문 TTS 성공: Google TTS")
                        return true
                    } else {
                        Log.w("TtsOrchestrator", "🇺🇸 Google TTS 재생 실패, 재시도 ${retryCount + 1}/$maxRetries")
                        retryCount++
                        if (retryCount < maxRetries) {
                            kotlinx.coroutines.delay(200) // 200ms 대기 후 재시도
                        }
                    }
                } else {
                    Log.w("TtsOrchestrator", "🇺🇸 Google TTS 사용 불가, 재시도 ${retryCount + 1}/$maxRetries")
                    retryCount++
                    if (retryCount < maxRetries) {
                        kotlinx.coroutines.delay(200) // 200ms 대기 후 재시도
                    }
                }
            } catch (e: Exception) {
                Log.e("TtsOrchestrator", "🇺🇸 Google TTS 초기화/재생 중 오류", e)
                retryCount++
                if (retryCount < maxRetries) {
                    kotlinx.coroutines.delay(200) // 200ms 대기 후 재시도
                }
            }
        }
        
        // 최대 재시도 횟수 초과
        Log.e("TtsOrchestrator", "🇺🇸 Google TTS 최대 재시도 횟수 초과")
        onComplete?.invoke()
        return false
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
            
            // TTS 서비스 사용 가능 여부 확인 (재시도 로직 포함)
            var retryCount = 0
            val maxRetries = 3
            
            while (retryCount < maxRetries) {
                try {
                    if (player.isAvailable()) {
                        Log.d("TtsOrchestrator", "🇰🇷 ${player.getServiceName()} 사용 가능, 재생 시도")
                        val success = player.speak(text, onComplete)
                        if (success) {
                            currentKoreanTtsIndex = i // 성공한 서비스로 업데이트
                            Log.d("TtsOrchestrator", "🇰🇷 한글 TTS 성공: ${player.getServiceName()}")
                            return true
                        } else {
                            Log.w("TtsOrchestrator", "🇰🇷 ${player.getServiceName()} 재생 실패, 재시도 ${retryCount + 1}/$maxRetries")
                            retryCount++
                            if (retryCount < maxRetries) {
                                kotlinx.coroutines.delay(200) // 200ms 대기 후 재시도
                            }
                        }
                    } else {
                        Log.w("TtsOrchestrator", "🇰🇷 ${player.getServiceName()} 사용 불가, 재시도 ${retryCount + 1}/$maxRetries")
                        retryCount++
                        if (retryCount < maxRetries) {
                            kotlinx.coroutines.delay(200) // 200ms 대기 후 재시도
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TtsOrchestrator", "🇰🇷 ${player.getServiceName()} 초기화/재생 중 오류", e)
                    retryCount++
                    if (retryCount < maxRetries) {
                        kotlinx.coroutines.delay(200) // 200ms 대기 후 재시도
                    }
                }
            }
            
            // 최대 재시도 횟수 초과 시 다음 서비스로 넘어감
            Log.e("TtsOrchestrator", "🇰🇷 ${player.getServiceName()} 최대 재시도 횟수 초과, 다음 서비스 시도")
            currentKoreanTtsIndex = i + 1
        }
        
        // 모든 서비스 실패 시 currentKoreanTtsIndex를 0으로 리셋
        currentKoreanTtsIndex = 0
        Log.e("TtsOrchestrator", "🇰🇷 모든 한글 TTS 서비스 실패, 인덱스 리셋")
        onComplete?.invoke()
        return false
    }
    
    /**
     * TTS 재생 전 상태 초기화
     */
    suspend fun resetTtsState() {
        Log.d("TtsOrchestrator", "TTS 상태 초기화")
        try {
            // 모든 TTS 플레이어 중지
            googleTtsPlayer.stop()
            for (player in koreanTtsPlayers) {
                player.stop()
            }
            
            // 한글 TTS 인덱스 리셋
            currentKoreanTtsIndex = 0
            
            // TTS 모니터링 시작
            ttsHealthMonitor.startMonitoring()
            
            Log.d("TtsOrchestrator", "TTS 상태 초기화 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 상태 초기화 실패", e)
        }
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
            
            // TTS 모니터링 중지
            ttsHealthMonitor.stopMonitoring()
            
            // 상태 초기화 (ButtonStateObserver로 이동했으므로 여기서는 제거)
            
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
    fun setAnswerHighlightIndex(index: Int) {
        Log.d("TtsOrchestrator", "답변 하이라이트 인덱스 설정: $index")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 답변 한글 하이라이트 인덱스 설정
     */
    fun setAnswerKoHighlightIndex(index: Int) {
        Log.d("TtsOrchestrator", "답변 한글 하이라이트 인덱스 설정: $index")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 녹음 하이라이트 인덱스 설정
     */
    fun setRecordingHighlightIndex(index: Int) {
        Log.d("TtsOrchestrator", "녹음 하이라이트 인덱스 설정: $index")
        // 하이라이트 관련 상태는 AppStateManager에서 관리
    }
    
    /**
     * 모든 TTS 플레이어 해제 (앱 종료 시 사용)
     */
    fun releaseAllPlayers() {
        Log.d("TtsOrchestrator", "모든 TTS 플레이어 해제")
        try {
            // TTS 모니터링 중지
            ttsHealthMonitor.stopMonitoring()
            
            googleTtsPlayer.release()
            samsungTtsPlayer.release()
            Log.d("TtsOrchestrator", "모든 TTS 플레이어 해제 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 플레이어 해제 실패", e)
        }
    }
    
    /**
     * 모든 TTS 플레이어 재초기화 (release 후 재사용 시)
     */
    fun reinitializeAllPlayers() {
        Log.d("TtsOrchestrator", "모든 TTS 플레이어 재초기화")
        try {
            // Google TTS 재초기화
            if (googleTtsPlayer is BaseTtsPlayer) {
                (googleTtsPlayer as BaseTtsPlayer).reinitializeTts()
            }
            
            // Samsung TTS 재초기화
            if (samsungTtsPlayer is BaseTtsPlayer) {
                (samsungTtsPlayer as BaseTtsPlayer).reinitializeTts()
            }
            
            Log.d("TtsOrchestrator", "모든 TTS 플레이어 재초기화 완료")
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 플레이어 재초기화 실패", e)
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
    suspend fun getAvailableKoreanTtsServices(): List<String> {
        val availableServices = mutableListOf<String>()
        for (player in koreanTtsPlayers) {
            if (player.isAvailable()) {
                availableServices.add(player.getServiceName())
            }
        }
        return availableServices
    }
    
    /**
     * 한글 TTS 서비스 상태 정보 반환
     */
    suspend fun getKoreanTtsServiceStatus(): List<Pair<String, Boolean>> {
        val statusList = mutableListOf<Pair<String, Boolean>>()
        for (player in koreanTtsPlayers) {
            statusList.add(player.getServiceName() to player.isAvailable())
        }
        return statusList
    }
    
    /**
     * 문장별 하이라이트와 함께 TTS 재생
     */
    suspend fun speakWithHighlight(text: String, onHighlight: (Int) -> Unit): Long {
        // TTS 활동 시간 업데이트 (모니터링용)
        ttsHealthMonitor.updateTtsActivity()
        
        Log.d("TtsOrchestrator", "🎯 speakWithHighlight 호출됨: '${text.take(30)}...'")
        
        val startTime = System.currentTimeMillis()
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        Log.d("TtsOrchestrator", "📝 문장 분리 완료: ${sentences.size}개 문장")
        
        // 하이라이트 초기화 (질문/답변 재생에서는 필요)
        onHighlight(-1)
        
        for ((idx, sentence) in sentences.withIndex()) {
            Log.d("TtsOrchestrator", "🔤 문장 ${idx + 1}/${sentences.size}: '${sentence.take(20)}...'")
            
            // 현재 문장 하이라이트 설정
            onHighlight(idx)
            Log.d("TtsOrchestrator", "✨ 하이라이트 설정: 문장 $idx")
            
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
        
        // 모든 재생 완료 후 하이라이트 해제 (질문/답변 재생에서는 필요)
        onHighlight(-1)
        val endTime = System.currentTimeMillis()
        Log.d("TtsOrchestrator", "✅ speakWithHighlight 완료")
        return endTime - startTime
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
     * 통합 TTS 재생 (하이라이트 지원)
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
        onHighlight: ((Int) -> Unit)? = null,
        waitForCompletion: Boolean = true
    ): Long {
        // TTS 활동 시간 업데이트 (모니터링용)
        ttsHealthMonitor.updateTtsActivity()
        
        Log.d("TtsOrchestrator", "🎯 speakUnified 호출: '${text.take(30)}...', isKorean=$isKorean, rate=$rate, hasHighlight=${onHighlight != null}")

        System.currentTimeMillis()
        val detectedKorean = isKorean ?: text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        
        return if (onHighlight != null) {
            // 하이라이트가 있는 경우 - 문장별 분리 재생
            speakWithHighlight(text, onHighlight)
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
        onHighlight: (Int) -> Unit
    ): Long {
        val startTime = System.currentTimeMillis()
        
        // 단일 문장으로 처리 (RepeatListeningUseCase에서 이미 분리된 문장을 전달)
        Log.d("TtsOrchestrator", "🔤 단일 문장 처리: '${text.take(30)}...'")
        
        // EnglishWritingTestRepositoryImpl에서 이미 올바른 하이라이트를 설정했으므로
        // 여기서는 초기화하지 않고 그대로 유지
        
        val completionDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
        val success = if (isKorean) {
            Log.d("TtsOrchestrator", "🇰🇷 한글 TTS로 재생")
            speakKorean(text) { completionDeferred.complete(Unit) }
        } else {
            Log.d("TtsOrchestrator", "🇺🇸 영문 TTS로 재생")
            speakEnglish(text) { completionDeferred.complete(Unit) }
        }
        
        if (success) {
            completionDeferred.await()
        } else {
            completionDeferred.complete(Unit)
        }
        
        // 모든 재생 완료 후 하이라이트 해제
        onHighlight(-1)
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