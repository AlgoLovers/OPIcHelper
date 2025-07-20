package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.delay

/**
 * 반복 듣기(암기 레벨) 테스트용 UseCase
 * - answerKo: 한글 답변 텍스트
 * - answerEn: 영문 답변 텍스트
 * - repeatCount: 반복 횟수 (기본 5회)
 * - onHighlight: 문장별 하이라이트 콜백
 *
 * 한글 문장 1회 → 1/2 쉬고 → 영문 문장 1회 → 1.0배 쉬고 → 영문 문장 2회 ... 5회까지 → 다음 한글 문장 ...
 */
class RepeatListeningUseCase(
    private val ttsPlayer: TtsPlayer,
    private val answerKo: String,
    private val answerEn: String,
    private val repeatCount: Int = 5,
    private val onHighlight: (Int?) -> Unit
) : MemorizeTestUseCase {
    override suspend fun execute() {
        val koSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        for (i in 0 until count) {
            // 1. 한글 문장 1회 TTS
            onHighlight(i)
            val koDuration = ttsPlayer.speakAndGetDuration(koSentences[i], isKorean = true)
            delay((koDuration * 0.5).toLong())
            // 2. 영문 문장 1~repeatCount회 TTS (속도 0.75, 쉬는 시간 1.0배)
            for (j in 1..repeatCount) {
                onHighlight(i)
                val enDuration = ttsPlayer.speakAndGetDuration(enSentences[i], isKorean = false, rate = 0.75f)
                delay((enDuration * 1.0).toLong())
            }
            onHighlight(null)
        }
    }
} 