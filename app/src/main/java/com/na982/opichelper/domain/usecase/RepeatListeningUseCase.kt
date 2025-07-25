package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 반복 듣기 테스트용 Service
 * 책임: 반복 듣기 테스트 실행, 진행 상황 관리, TTS 제어
 */
@Singleton
class RepeatListeningService @Inject constructor(
    private val ttsPlayer: TtsPlayer,
    private val progressTracker: MemorizeTestProgressTracker
) {
    /**
     * 반복 듣기 테스트 실행
     * - answerKo: 한글 답변 텍스트
     * - answerEn: 영문 답변 텍스트
     * - repeatCount: 반복 횟수 (기본 5회)
     * - onHighlight: 영문 하이라이트 콜백
     * - onKoreanHighlight: 한글 하이라이트 콜백
     * - onCardFlip: 카드 뒤집기 콜백 (true: 한글, false: 영문)
     * - category: 카테고리
     * - scriptIndex: 스크립트 인덱스
     *
     * 한글 문장 1회 → 1/2 쉬고 → 영문 문장 1회 → 1.0배 쉬고 → 영문 문장 2회 ... 5회까지 → 다음 한글 문장 ...
     */
    suspend fun executeRepeatListeningTest(
        answerKo: String,
        answerEn: String,
        onHighlight: (Int?) -> Unit,
        onKoreanHighlight: (Int?) -> Unit,
        onCardFlip: (Boolean) -> Unit, // true: 한글, false: 영문
        category: String,
        scriptIndex: Int,
        repeatCount: Int = 5
    ) {
        val koSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        
        // 복원된 앱 상태에서 시작 인덱스 가져오기
        val currentProgress = progressTracker.getScriptProgress(category, scriptIndex)
        
        val startIndex = if (currentProgress != null && currentProgress.memorizeLevel == "반복 듣기") {
            currentProgress.currentSentenceIndex
        } else {
            0
        }
        
        Log.d("RepeatListeningService", "반복 듣기 시작: 총 $count 문장, 시작 인덱스: $startIndex")
        Log.d("RepeatListeningService", "현재 스크립트 진행 상황: $currentProgress")
        Log.d("RepeatListeningService", "검색한 카테고리: $category, 스크립트 인덱스: $scriptIndex")
        
        for (i in startIndex until count) {
            // 코루틴이 취소되었는지 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("RepeatListeningService", "코루틴이 취소됨 - Service 중단")
                break
            }
            
            Log.d("RepeatListeningService", "문장 ${i + 1} 처리 시작 (인덱스: $i)")
            
            // 진행 상황 업데이트 및 실시간 저장
            progressTracker.updateProgress(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = "반복 듣기",
                currentSentenceIndex = i,
                totalSentences = count,
                isMemorizeTestRunning = true
            )
            // 실시간으로 진행상황 저장
            progressTracker.persistChangedProgress()
            Log.d("RepeatListeningService", "문장 $i 진행상황 실시간 저장 완료")
            
            // 1. 한글 문장 1회 TTS (카드를 한글로 뒤집고 하이라이트)
            onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            onKoreanHighlight(i) // 한글 하이라이트
            val koDuration = ttsPlayer.speakAndGetDuration(koSentences[i], isKorean = true, rate = 0.8f)
            delay((koDuration * 0.5).toLong())
            
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
                
                onCardFlip(false) // 카드를 영문으로 뒤집기
                delay(100) // 카드 뒤집기 애니메이션 대기
                onHighlight(i) // 영문 하이라이트
                val enDuration = ttsPlayer.speakAndGetDuration(enSentences[i], isKorean = false, rate = 0.75f)
                delay((enDuration * 1.0).toLong())
            }
            onHighlight(null) // 하이라이트 제거
        }
        
        // 마지막에 카드를 원래 상태(영문)로 복원
        onCardFlip(false)
        onHighlight(null)
        
        // 테스트 완료 - 현재 스크립트 진행 상황 삭제
        progressTracker.clearScriptProgress(category, scriptIndex)
        
        Log.d("RepeatListeningService", "반복 듣기 완료")
    }
} 