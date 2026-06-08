package com.na982.opichelper.data.audio

import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.repository.TtsPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

class TtsOrchestratorImpl(
    private val googleTtsPlayer: TtsPlayer,
    private val samsungTtsPlayer: TtsPlayer,
    private val ttsPreferences: TtsPreferences,
    private val logger: AppLogger
) : TtsOrchestrator {

    companion object {
        private const val INTER_SENTENCE_DELAY_MS = 400L
        private const val KOREAN_UNICODE_SYLLABLE_START = 0xAC00
        private const val KOREAN_UNICODE_SYLLABLE_END = 0xD7AF
        private const val KOREAN_UNICODE_JAMO_START = 0x3131
        private const val KOREAN_UNICODE_JAMO_END = 0x318E
    }
    private val activeSpeakCount = AtomicInteger(0)
    private val _isSpeaking = MutableStateFlow(false)

    private val allPlayers = listOf(googleTtsPlayer, samsungTtsPlayer)
    private val koreanTtsPlayers = listOf(samsungTtsPlayer)
    private val currentKoreanTtsIndex = AtomicInteger(0)

    private fun enterSpeaking() {
        if (activeSpeakCount.incrementAndGet() == 1) {
            _isSpeaking.update { true }
        }
    }

    private fun exitSpeaking() {
        if (activeSpeakCount.decrementAndGet() == 0) {
            _isSpeaking.update { false }
        }
    }

    private suspend fun speakInternal(text: String): TtsSpeakResult {
        val isKorean = text.any { it.code in KOREAN_UNICODE_SYLLABLE_START..KOREAN_UNICODE_SYLLABLE_END || it.code in KOREAN_UNICODE_JAMO_START..KOREAN_UNICODE_JAMO_END }
        return if (isKorean) speakKorean(text) else speakEnglish(text)
    }

    private suspend fun speakEnglish(text: String): TtsSpeakResult {
        googleTtsPlayer.setSpeechRate(ttsPreferences.getEnglishTtsRate())
        return googleTtsPlayer.speak(text)
    }

    private suspend fun speakKorean(text: String): TtsSpeakResult {
        val startIndex = currentKoreanTtsIndex.get()
        var anyAvailable = false
        for (i in startIndex until koreanTtsPlayers.size) {
            val player = koreanTtsPlayers[i]
            if (player.isAvailable()) {
                anyAvailable = true
                val result = player.speak(text)
                if (result is TtsSpeakResult.Success) {
                    currentKoreanTtsIndex.set(i)
                    return result
                }
                logger.w("TtsOrchestrator", "한글 TTS 실패: ${player.getServiceName()}, 다음 서비스 시도")
                currentKoreanTtsIndex.set(i + 1)
            } else {
                logger.w("TtsOrchestrator", "한글 TTS 서비스 사용 불가: ${player.getServiceName()}, 다음 서비스 시도")
                currentKoreanTtsIndex.set(i + 1)
            }
        }
        currentKoreanTtsIndex.set(0)
        return if (anyAvailable) {
            logger.e("TtsOrchestrator", "모든 한글 TTS 서비스 실패 — 인덱스 리셋")
            TtsSpeakResult.Error("모든 한글 TTS 서비스 실패")
        } else {
            TtsSpeakResult.Unavailable
        }
    }

    override fun stop() {
        try {
            allPlayers.forEach { it.stop() }
        } catch (e: Exception) {
            logger.e("TtsOrchestrator", "TTS 중지 실패", e)
        }
    }

    override fun releaseAllPlayers() {
        try {
            allPlayers.forEach { it.release() }
        } catch (e: Exception) {
            logger.e("TtsOrchestrator", "TTS 플레이어 해제 실패", e)
        }
    }

    override fun getCurrentKoreanTtsServiceName(): String {
        val index = currentKoreanTtsIndex.get()
        return if (index < koreanTtsPlayers.size) {
            koreanTtsPlayers[index].getServiceName()
        } else {
            "없음"
        }
    }

    override suspend fun speakWithHighlight(text: String, onHighlight: (index: Int?, sentence: String?) -> Unit): TtsSpeakResult {
        val sentences = SentenceSplitter.split(text)
        enterSpeaking()
        var lastResult: TtsSpeakResult = TtsSpeakResult.Success(0L)

        try {
            for ((idx, sentence) in sentences.withIndex()) {
                onHighlight(idx, sentence)
                val result = speakInternal(sentence)
                lastResult = result
                if (result is TtsSpeakResult.Unavailable) {
                    logger.e("TtsOrchestrator", "speakWithHighlight 문장 $idx TTS 사용 불가 — 중단")
                    break
                }
                if (result is TtsSpeakResult.Error || result is TtsSpeakResult.Timeout) {
                    logger.e("TtsOrchestrator", "speakWithHighlight 문장 $idx 실패: $result")
                    break
                }
                delay(INTER_SENTENCE_DELAY_MS)
            }
            onHighlight(null, null)
        } catch (e: CancellationException) {
            onHighlight(null, null)
            throw e
        } finally {
            exitSpeaking()
        }
        return lastResult
    }

    override suspend fun speakAndWaitForCompletion(text: String): TtsSpeakResult {
        enterSpeaking()
        return try {
            speakInternal(text)
        } finally {
            exitSpeaking()
        }
    }
}
