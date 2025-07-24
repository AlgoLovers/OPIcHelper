package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log

/**
 * 영작 테스트용 UseCase
 * - answerKo: 한글 답변 텍스트
 * - answerEn: 영문 답변 텍스트
 * - onKoreanHighlight: 한글 문장 하이라이트 콜백
 * - onEnglishHighlight: 영문 문장 하이라이트 콜백
 * - onRecordingHighlight: 녹음 하이라이트 콜백
 * - onCardFlip: 카드 뒤집기 콜백 (true: 한글, false: 영문)
 * - progressTracker: 암기 테스트 진행 상황 추적
 * - category: 카테고리
 * - scriptIndex: 스크립트 인덱스
 *
 * 한글 문장 1회 → 1/2 쉬고 → 영문 문장 1회 → 종료
 */
class EnglishWritingTestUseCase(
    private val answerKo: String,
    private val answerEn: String,
    private val ttsPlayer: TtsPlayer,
    private val onKoreanHighlight: (Int?) -> Unit,
    private val onEnglishHighlight: (Int?) -> Unit,
    private val onRecordingHighlight: (Int?) -> Unit,
    private val onCardFlip: (Boolean) -> Unit, // true: 한글, false: 영문
    private val progressTracker: MemorizeTestProgressTracker,
    private val category: String,
    private val scriptIndex: Int
) : MemorizeTestUseCase {
    override suspend fun execute() {
        val koSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        
        // 복원된 앱 상태에서 시작 인덱스 가져오기
        val currentProgress = progressTracker.getScriptProgress(category, scriptIndex)
        
        val startIndex = if (currentProgress?.isMemorizeTestRunning == true) {
            currentProgress.currentSentenceIndex
        } else {
            0
        }
        
        Log.d("EnglishWritingTestUseCase", "영작 테스트 시작: 총 $count 문장, 시작 인덱스: $startIndex")
        Log.d("EnglishWritingTestUseCase", "현재 스크립트 진행 상황: $currentProgress")
        Log.d("EnglishWritingTestUseCase", "검색한 카테고리: $category, 스크립트 인덱스: $scriptIndex")
        
        for (idx in startIndex until count) {
            // 코루틴이 취소되었는지 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("EnglishWritingTestUseCase", "코루틴이 취소됨 - UseCase 중단")
                break
            }
            
            Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 처리 시작 (인덱스: $idx)")
            
            // 진행 상황 업데이트
            progressTracker.updateCurrentSentenceIndex(category, scriptIndex, idx)
            
            // 1. 한글 문장 1회 TTS (카드를 한글로 뒤집고 하이라이트)
            onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            onKoreanHighlight(idx) // 한글 하이라이트
            val koDuration = ttsPlayer.speakAndGetDuration(koSentences[idx], isKorean = true, rate = 0.8f)
            delay((koDuration * 0.5).toLong())

            // 코루틴이 취소되었는지 다시 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("EnglishWritingTestUseCase", "코루틴이 취소됨 - UseCase 중단")
                break
            }

            // 2. 영문 문장 1회 TTS (카드를 영문으로 뒤집고 하이라이트)
            onCardFlip(false) // 카드를 영문으로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            onEnglishHighlight(idx) // 영문 하이라이트
            val enDuration = ttsPlayer.speakAndGetDuration(enSentences[idx], isKorean = false, rate = 0.75f)
            delay((enDuration * 1.0).toLong())

            onEnglishHighlight(null) // 영문 하이라이트 제거
        }
        
        // 마지막에 카드를 원래 상태(영문)로 복원
        onCardFlip(false)
        onKoreanHighlight(null)
        onEnglishHighlight(null)
        
        // 테스트 완료 - 현재 스크립트 진행 상황 삭제
        progressTracker.clearScriptProgress(category, scriptIndex)
        
        Log.d("EnglishWritingTestUseCase", "영작 테스트 완료")
    }
} 