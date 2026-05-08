package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반복 듣기 테스트용 UseCase
 * 책임: 반복 듣기 테스트 실행, 진행 상황 관리, TTS 제어
 */
@Singleton
class RepeatListeningUseCase @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val progressTracker: MemorizeTestProgressTracker,
    private val recordingTimeManager: RecordingTimeManager
) {
    /**
     * 반복 듣기 서비스 실행
     * 
     * @param data 반복 듣기 데이터
     * @param uiCallback UI 콜백 인터페이스
     * @param repeatCount 반복 횟수 (기본 5회)
     */
    suspend fun startRepeatListening(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int = 5
    ) {
        val koSentences = data.koreanAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = data.englishAnswer.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        
        // 복원된 앱 상태에서 시작 인덱스 가져오기 (암기레벨별)
        val currentProgress = progressTracker.getScriptProgress(data.category, data.scriptIndex, MemorizeLevel.REPEAT_LISTENING.displayName)
        
        val startIndex = if (currentProgress != null) {
            currentProgress.currentSentenceIndex
        } else {
            0
        }
        
        Log.d("RepeatListeningService", "반복 듣기 시작: 총 $count 문장, 시작 인덱스: $startIndex")
        Log.d("RepeatListeningService", "현재 스크립트 진행 상황: $currentProgress")
        Log.d("RepeatListeningService", "검색한 카테고리: ${data.category}, 스크립트 인덱스: ${data.scriptIndex}")
        
        for (i in startIndex until count) {
            // 코루틴이 취소되었는지 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("RepeatListeningService", "코루틴이 취소됨 - Service 중단")
                break
            }
            
            Log.d("RepeatListeningService", "문장 ${i + 1} 처리 시작 (인덱스: $i)")
            
            // 진행 상황 업데이트 및 실시간 저장
            progressTracker.updateProgress(
                category = data.category,
                scriptIndex = data.scriptIndex,
                memorizeLevel = MemorizeLevel.REPEAT_LISTENING.displayName,
                currentSentenceIndex = i,
                totalSentences = count,
                isMemorizeTestRunning = true
            )
            // 실시간으로 진행상황 저장
            progressTracker.persistChangedProgress()
            Log.d("RepeatListeningService", "문장 $i 진행상황 실시간 저장 완료")
            
            // 1. 한글 문장 1회 TTS (카드를 한글로 뒤집고 하이라이트)
            uiCallback.onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            uiCallback.onKoreanHighlight(i) // 한글 하이라이트
            
            // 한글 TTS 재생 완료까지 기다리기 (표준화된 방식 사용)
            ttsOrchestrator.speakAndWaitForCompletion(koSentences[i], isKorean = true, rate = 1.0f)
            
            // 영문 문장 길이에 비례한 딜레이 계산 (고급 버전)
            val enSentence = enSentences[i]
            val enWordCount = enSentence.split("\\s+".toRegex()).size
            
            // 방법 1: 단어 수 기반 적응형 딜레이
            val baseDelay = enWordCount * 500 // 기본 딜레이
            val lengthMultiplier = when {
                enWordCount <= 5 -> 1.5f    // 짧은 문장: 1.5배
                enWordCount <= 10 -> 1.2f   // 중간 문장: 1.2배
                enWordCount <= 15 -> 1.0f   // 긴 문장: 1.0배
                else -> 0.8f                // 매우 긴 문장: 0.8배
            }
            val adaptiveDelay = (baseDelay * lengthMultiplier).toLong()
            
            Log.d("RepeatListeningService", "문장 $i 딜레이 계산: 영문 단어 수=$enWordCount, 기본 딜레이=${baseDelay}ms, 최종 딜레이=${adaptiveDelay}ms")
            kotlinx.coroutines.delay(adaptiveDelay)
            
            // 코루틴이 취소되었는지 다시 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("RepeatListeningService", "코루틴이 취소됨 - Service 중단")
                break
            }
            
            // 2. 영문 문장 1~repeatCount회 TTS (카드를 영문으로 뒤집고 하이라이트)
            for (j in 1..repeatCount) {
                // 코루틴이 취소되었는지 확인
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                    Log.d("RepeatListeningService", "코루틴이 취소됨 - Service 중단")
                    break
                }
                
                Log.d("RepeatListeningService", "문장 ${i + 1} 영문 TTS 반복 ${j}/${repeatCount}")
                
                uiCallback.onCardFlip(false) // 카드를 영문으로 뒤집기
                delay(100) // 카드 뒤집기 애니메이션 대기
                uiCallback.onHighlight(i) // 영문 하이라이트
                
                // TTS 재생 완료까지 기다리기 (표준화된 방식 사용)
                val enDuration = ttsOrchestrator.speakAndWaitForCompletion(enSentences[i], isKorean = false, rate = 1.0f)
                
                // 첫 번째 반복에서만 TTS 시간 저장 (영문 문장)
                if (j == 1) {
                    recordingTimeManager.saveRecordingTime(data.category, data.scriptIndex, i, enDuration)
                    Log.d("RepeatListeningService", "문장 $i 영문 TTS 시간 저장: ${enDuration}ms")
                }
                
                // 코루틴이 취소되었는지 다시 확인 (TTS 재생 후)
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                    Log.d("RepeatListeningService", "TTS 재생 후 코루틴이 취소됨 - Service 중단")
                    break
                }
                
                // 충분한 쉬는 시간 (사용자가 혼자 말해볼 시간)
                val restTime = (enDuration * 1.2).toLong() // TTS 시간의 1.2배
                Log.d("RepeatListeningService", "문장 ${i + 1} 반복 ${j} 쉬는 시간: ${restTime}ms")
                delay(restTime)
            }
            uiCallback.onHighlight(null) // 하이라이트 제거
        }
        
        // 마지막에 카드를 원래 상태(영문)로 복원
        uiCallback.onCardFlip(false)
        uiCallback.onHighlight(null)
        
        // 테스트 완료 - 현재 스크립트 진행 상황 삭제 (암기레벨별)
        progressTracker.clearScriptProgress(data.category, data.scriptIndex, MemorizeLevel.REPEAT_LISTENING.displayName)
        
        // 완료 콜백 호출
        uiCallback.onComplete()
        
        Log.d("RepeatListeningService", "반복 듣기 완료")
    }
} 