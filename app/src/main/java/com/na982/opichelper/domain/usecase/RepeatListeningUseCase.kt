package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.delay

/**
 * 반복 듣기(암기 레벨) 테스트용 UseCase
 * - answerKo: 한글 답변 텍스트
 * - answerEn: 영문 답변 텍스트
 * - repeatCount: 반복 횟수 (기본 5회)
 * - onHighlight: 문장별 하이라이트 콜백
 * - onCardFlip: 카드 뒤집기 콜백 (true: 한글, false: 영문)
 *
 * 한글 문장 1회 → 1/2 쉬고 → 영문 문장 1회 → 1.0배 쉬고 → 영문 문장 2회 ... 5회까지 → 다음 한글 문장 ...
 */
class RepeatListeningUseCase(
    private val answerKo: String,
    private val answerEn: String,
    private val ttsPlayer: TtsPlayer,
    private val onHighlight: (Int?) -> Unit,
    private val onCardFlip: (Boolean) -> Unit, // true: 한글, false: 영문
    private val repeatCount: Int = 5
) : MemorizeTestUseCase {
    override suspend fun execute() {
        val koSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        
        for (i in 0 until count) {
            // 1. 한글 문장 1회 TTS (카드를 한글로 뒤집고 하이라이트)
            onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            onHighlight(i) // 한글 하이라이트
            val koDuration = ttsPlayer.speakAndGetDuration(koSentences[i], isKorean = true, rate = 0.8f)
            delay((koDuration * 0.5).toLong())
            
            // 2. 영문 문장 1~repeatCount회 TTS (카드를 영문으로 뒤집고 하이라이트)
            for (j in 1..repeatCount) {
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
    }
} 