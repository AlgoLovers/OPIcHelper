package com.na982.opichelper.presentation.ui.component

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import java.util.*
import kotlinx.coroutines.*
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred

/**
 * TtsPlayer: 모듈형 TTS 유틸리티. Context와 lifecycle을 안전하게 관리하며,
 * 재생/정지/속도 등 확장 가능. 질문/답변 등 다양한 곳에서 재사용 가능.
 */
class TtsPlayer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speakJob: Job? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US // 영어 기본
            isReady = true
        }
    }

    fun speak(text: String, rate: Float = 0.8f) {
        if (isReady) {
            tts?.setSpeechRate(rate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun speakBySentence(text: String, repeatCount: Int = 5, pauseRatio: Float = 1.5f, rate: Float = 0.8f) {
        if (!isReady) return
        speakJob?.cancel()
        speakJob = CoroutineScope(Dispatchers.Main).launch {
            val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            for (sentence in sentences) {
                repeat(repeatCount) { i ->
                    if (!isActive) return@launch
                    val utteranceId = "utt_${System.currentTimeMillis()}_${i}"
                    val tts = tts ?: return@launch
                    tts.setSpeechRate(rate)
                    val finished = CompletableDeferred<Unit>()
                    @Suppress("DEPRECATION")
                    val listener = object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) { finished.complete(Unit) }
                        override fun onError(utteranceId: String?) { finished.complete(Unit) }
                    }
                    tts.setOnUtteranceProgressListener(listener)
                    tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    finished.await() // 실제 재생이 끝날 때까지 대기
                    if (!isActive) return@launch
                    // 재생이 끝난 후 1.5배 쉬기
                    val baseDuration = (sentence.length * 50L).coerceAtLeast(800L)
                    delay((baseDuration * pauseRatio).toLong())
                }
            }
            tts?.setOnUtteranceProgressListener(null)
        }
    }

    fun stop() {
        speakJob?.cancel()
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

/**
 * rememberTtsPlayer: Compose에서 안전하게 TtsPlayer를 관리하는 헬퍼
 */
@Composable
fun rememberTtsPlayer(context: Context): TtsPlayer {
    val ttsPlayer = remember { TtsPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsPlayer.shutdown()
        }
    }
    return ttsPlayer
} 