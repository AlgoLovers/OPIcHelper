package com.na982.opichelper.domain.audio

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class TtsOrchestrator @Inject constructor(
    private val googleTtsPlayer: TtsPlayer,
    private val samsungTtsPlayer: TtsPlayer
) {
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val allPlayers = listOf(googleTtsPlayer, samsungTtsPlayer)
    private val koreanTtsPlayers = listOf(samsungTtsPlayer)
    private val currentKoreanTtsIndex = AtomicInteger(0)

    private suspend fun speakInternal(text: String): TtsSpeakResult {
        val isKorean = text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
        return if (isKorean) speakKorean(text) else speakEnglish(text)
    }

    suspend fun speak(text: String): TtsSpeakResult {
        _isSpeaking.value = true
        return try {
            speakInternal(text)
        } finally {
            _isSpeaking.value = false
        }
    }

    private suspend fun speakEnglish(text: String): TtsSpeakResult {
        return googleTtsPlayer.speak(text)
    }

    private suspend fun speakKorean(text: String): TtsSpeakResult {
        val startIndex = currentKoreanTtsIndex.get()
        for (i in startIndex until koreanTtsPlayers.size) {
            val player = koreanTtsPlayers[i]
            if (player.isAvailable()) {
                val result = player.speak(text)
                if (result is TtsSpeakResult.Success) {
                    currentKoreanTtsIndex.set(i)
                    return result
                }
                Log.w("TtsOrchestrator", "한글 TTS 실패: ${player.getServiceName()}, 다음 서비스 시도")
                currentKoreanTtsIndex.set(i + 1)
            } else {
                Log.w("TtsOrchestrator", "한글 TTS 서비스 사용 불가: ${player.getServiceName()}, 다음 서비스 시도")
                currentKoreanTtsIndex.set(i + 1)
            }
        }
        Log.e("TtsOrchestrator", "모든 한글 TTS 서비스 실패 — 인덱스 리셋")
        currentKoreanTtsIndex.set(0)
        return TtsSpeakResult.Error("모든 한글 TTS 서비스 실패")
    }

    fun stop() {
        try {
            _isSpeaking.value = false
            allPlayers.forEach { it.stop() }
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 중지 실패", e)
        }
    }

    fun releaseAllPlayers() {
        try {
            allPlayers.forEach { it.release() }
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 플레이어 해제 실패", e)
        }
    }

    fun isPlaying(): Boolean = allPlayers.any { it.isPlaying() }

    fun getCurrentKoreanTtsServiceName(): String {
        val index = currentKoreanTtsIndex.get()
        return if (index < koreanTtsPlayers.size) {
            koreanTtsPlayers[index].getServiceName()
        } else {
            "없음"
        }
    }

    fun getAvailableKoreanTtsServices(): List<String> {
        return koreanTtsPlayers.mapNotNull { player ->
            if (player.isAvailable()) player.getServiceName() else null
        }
    }

    fun getKoreanTtsServiceStatus(): List<Pair<String, Boolean>> {
        return koreanTtsPlayers.map { player ->
            player.getServiceName() to player.isAvailable()
        }
    }

    suspend fun speakWithHighlight(text: String, onHighlight: (index: Int?, sentence: String?) -> Unit) {
        val sentences = SentenceSplitter.split(text)
        _isSpeaking.value = true

        try {
            for ((idx, sentence) in sentences.withIndex()) {
                onHighlight(idx, sentence)
                val result = speakInternal(sentence)
                if (result is TtsSpeakResult.Error || result is TtsSpeakResult.Timeout) {
                    Log.e("TtsOrchestrator", "speakWithHighlight 문장 $idx 실패: $result")
                }
                delay(400L)
            }
            onHighlight(null, null)
        } catch (e: CancellationException) {
            onHighlight(null, null)
            throw e
        } finally {
            _isSpeaking.value = false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun speakAndWaitForCompletion(text: String, isKorean: Boolean, rate: Float): Long {
        _isSpeaking.value = true
        return try {
            val result = speakInternal(text)
            if (result is TtsSpeakResult.Success) result.durationMs else 0L
        } finally {
            _isSpeaking.value = false
        }
    }

    fun pause() {
        try {
            allPlayers.forEach { it.pause() }
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 일시 중지 실패", e)
        }
    }

    fun resume() {
        try {
            allPlayers.forEach { it.resume() }
        } catch (e: Exception) {
            Log.e("TtsOrchestrator", "TTS 재개 실패", e)
        }
    }
}
