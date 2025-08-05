package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.state.MemorizationProgressTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.na982.opichelper.domain.util.CoroutineUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반복 듣기 테스트 시작 UseCase
 * 책임: 반복 듣기 테스트 실행, 진행 상황 관리, TTS 제어
 */
@Singleton
class StartRepeatListeningUseCase @Inject constructor(
    private val ttsController: TtsController,
    private val progressTracker: MemorizationProgressTracker,
    private val recordingTimeManager: RecordingTimeManager,
    private val appStateManager: AppStateManager
) {
    private var currentJob: Job? = null
    
    /**
     * 반복 듣기 테스트 시작
     * 
     * @param data 반복 듣기 데이터
     * @param uiCallback UI 콜백 인터페이스
     * @param repeatCount 반복 횟수 (기본 5회)
     */
    suspend fun execute(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int = 5
    ) {
        Log.d("StartRepeatListeningUseCase", "반복 듣기 테스트 시작")
        
        // 이전 작업이 있다면 취소
        currentJob?.cancel()
        
        currentJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                executeRepeatListening(data, uiCallback, repeatCount)
            } catch (e: Exception) {
                Log.e("StartRepeatListeningUseCase", "반복듣기 실행 중 오류", e)
                // 오류 발생 시 상태 초기화
                uiCallback.onComplete()
                throw e
            }
        }
    }
    
    /**
     * 반복 듣기 중지
     */
    suspend fun stop() {
        Log.d("StartRepeatListeningUseCase", "반복듣기 중지 요청")
        currentJob?.cancel()
        currentJob = null
        // TTS도 중지
        ttsController.stopTts()
        Log.d("StartRepeatListeningUseCase", "반복듣기 중지 완료 - TTS 중지 및 코루틴 취소")
    }
    
    /**
     * 실제 반복듣기 실행 로직
     */
    private suspend fun executeRepeatListening(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int
    ) {
        val koSentences = data.koreanAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = data.englishAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        
        // 복원된 앱 상태에서 시작 인덱스 가져오기 (암기레벨별)
        val currentProgress = progressTracker.getScriptProgress(data.category, data.scriptIndex, "반복 듣기")
        
        val startIndex = if (currentProgress != null) {
            currentProgress.currentSentenceIndex
        } else {
            0
        }
        
        Log.d("StartRepeatListeningUseCase", "반복 듣기 시작: 총 $count 문장, 시작 인덱스: $startIndex")
        Log.d("StartRepeatListeningUseCase", "현재 스크립트 진행 상황: $currentProgress")
        Log.d("StartRepeatListeningUseCase", "검색한 카테고리: ${data.category}, 스크립트 인덱스: ${data.scriptIndex}")
        
        // TTS 상태 초기화 (한글 TTS 재생 문제 해결)
        Log.d("StartRepeatListeningUseCase", "TTS 상태 초기화 시작")
        ttsController.stopTts()
        // TTS 중지는 stopTts()에서 완료됨
        Log.d("StartRepeatListeningUseCase", "TTS 상태 초기화 완료")
        
        for (i in startIndex until count) {
            // 안전한 코루틴 취소 확인
            if (!CoroutineUtils.checkCancellation("StartRepeatListeningUseCase", "문장 처리")) {
                break
            }
            
            Log.d("StartRepeatListeningUseCase", "문장 ${i + 1} 처리 시작 (인덱스: $i)")
            
            // 진행 상황 업데이트 및 실시간 저장
            progressTracker.updateProgress(
                category = data.category,
                scriptIndex = data.scriptIndex,
                memorizeLevel = "반복 듣기",
                currentSentenceIndex = i,
                totalSentences = count,
                isMemorizeTestRunning = true
            )
            // 실시간으로 진행상황 저장
            progressTracker.persistChangedProgress()
            Log.d("StartRepeatListeningUseCase", "문장 $i 진행상황 실시간 저장 완료")
            
            // 1. 한글 문장 1회 TTS (카드를 한글로 뒤집고 하이라이트)
            uiCallback.onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            
            // 한글 문장 하이라이트 설정 (executeRepeatListening에서 직접 관리)
            appStateManager.updateHighlightState(
                questionHighlightIndex = -1,
                answerHighlightIndex = -1,
                answerKoHighlightIndex = i,  // 문장 인덱스 직접 설정
                recordingHighlightIndex = -1
            )
            
            // TtsController를 통한 한글 문장 재생 (하이라이트 없음)
            ttsController.playSentenceForRepeatListening(
                text = koSentences[i],
                isKorean = true
            )
            
            // 1순위: 저장된 TTS 시간 확인 (이전 영작테스트에서 저장된 시간)
            val savedTtsTime = recordingTimeManager.getRecordingTime(data.category, data.scriptIndex, i)
            
            // 2순위: 폴백 - 문장 길이 기반 예측 계산
            val adaptiveDelay = if (savedTtsTime != null && savedTtsTime > 0) {
                Log.d("StartRepeatListeningUseCase", "문장 $i 저장된 TTS 시간 사용: ${savedTtsTime}ms")
                savedTtsTime
            } else {
                val enSentence = enSentences[i]
                val enWordCount = enSentence.split("\\s+".toRegex()).size
                
                // 단어 수 기반 적응형 딜레이 계산
                val baseDelay = enWordCount * 500 // 기본 딜레이
                val lengthMultiplier = when {
                    enWordCount <= 5 -> 1.5f    // 짧은 문장: 1.5배
                    enWordCount <= 10 -> 1.2f   // 중간 문장: 1.2배
                    enWordCount <= 15 -> 1.0f   // 긴 문장: 1.0배
                    else -> 0.8f                // 매우 긴 문장: 0.8배
                }
                val calculatedDelay = (baseDelay * lengthMultiplier).toLong()
                
                Log.d("StartRepeatListeningUseCase", "문장 $i 예측 계산 사용: 영문 단어 수=$enWordCount, 기본 딜레이=${baseDelay}ms, 최종 딜레이=${calculatedDelay}ms")
                calculatedDelay
            }
            
            delay(adaptiveDelay)
            
            // 안전한 코루틴 취소 확인 (딜레이 후)
            if (!CoroutineUtils.checkCancellation("StartRepeatListeningUseCase", "딜레이 후")) {
                break
            }
            

            for (j in 1..repeatCount) {
                // 안전한 코루틴 취소 확인 (영문 반복)
                if (!CoroutineUtils.checkCancellation("StartRepeatListeningUseCase", "영문 반복")) {
                    break
                }
                
                Log.d("StartRepeatListeningUseCase", "문장 ${i + 1} 영문 TTS 반복 ${j}/${repeatCount}")
                
                uiCallback.onCardFlip(false) // 카드를 영문으로 뒤집기
                delay(100) // 카드 뒤집기 애니메이션 대기
                
                // 첫 번째 반복에서만 영문 하이라이트 설정
                if (j == 1) {
                    appStateManager.updateHighlightState(
                        questionHighlightIndex = -1,
                        answerHighlightIndex = i,  // 문장 인덱스 직접 설정
                        answerKoHighlightIndex = -1,
                        recordingHighlightIndex = -1
                    )
                    Log.d("StartRepeatListeningUseCase", "문장 ${i + 1} 영문 하이라이트 설정 (첫 번째 반복)")
                }
                
                // TtsController를 통한 영문 문장 재생 (하이라이트 없음)
                val enDuration = ttsController.playSentenceForRepeatListening(
                    text = enSentences[i],
                    isKorean = false
                )
                
                // 첫 번째 반복에서만 TTS 시간 저장 (영문 문장)
                if (j == 1) {
                    recordingTimeManager.saveRecordingTime(data.category, data.scriptIndex, i, enDuration)
                    Log.d("StartRepeatListeningUseCase", "문장 $i 영문 TTS 시간 저장: ${enDuration}ms")
                }
                
                // 안전한 코루틴 취소 확인 (TTS 재생 후)
                if (!CoroutineUtils.checkCancellation("StartRepeatListeningUseCase", "TTS 재생 후")) {
                    Log.d("StartRepeatListeningUseCase", "TTS 재생 후 코루틴이 취소됨 - Service 중단")
                    break
                }

                val restTime = (enDuration * 1.2).toLong()
                delay(restTime)
            }
            // 문장 반복 완료 후 하이라이트 제거하지 않음 (다음 문장으로 넘어갈 때까지 유지)
        }
        
        // 마지막에 카드를 원래 상태(영문)로 복원
        uiCallback.onCardFlip(false)
        uiCallback.onHighlight(-1)
        
        // 테스트 완료 - 현재 스크립트 진행 상황 삭제 (암기레벨별)
        progressTracker.clearScriptProgress(data.category, data.scriptIndex, "반복 듣기")
        
        // 완료 콜백 호출
        uiCallback.onComplete()
        
        Log.d("StartRepeatListeningUseCase", "반복 듣기 완료")
    }
} 