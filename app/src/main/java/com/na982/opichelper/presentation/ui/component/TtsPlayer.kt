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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TtsPlayer: 모듈형 TTS 유틸리티. Context와 lifecycle을 안전하게 관리하며,
 * 재생/정지/속도 등 확장 가능. 질문/답변 등 다양한 곳에서 재사용 가능.
 */
class TtsPlayer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var speakJob: Job? = null
    private val _currentQuestionSentenceIndex = MutableStateFlow<Int?>(null)
    val currentQuestionSentenceIndex: StateFlow<Int?> = _currentQuestionSentenceIndex.asStateFlow()
    private val _currentAnswerSentenceIndex = MutableStateFlow<Int?>(null)
    val currentAnswerSentenceIndex: StateFlow<Int?> = _currentAnswerSentenceIndex.asStateFlow()

    enum class TtsMode { NONE, QUESTION, ANSWER, ANSWER_BY_SENTENCE }
    private var currentMode: TtsMode = TtsMode.NONE

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US // 영어 기본
            isReady = true
        }
    }

    private fun speakSentences(
        text: String,
        rate: Float,
        highlightFlow: MutableStateFlow<Int?>,
        restDuration: (String) -> Long,
        mode: TtsMode
    ) {
        if (!isReady) return
        speakJob?.cancel()
        currentMode = mode
        speakJob = CoroutineScope(Dispatchers.Main).launch {
            val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            for ((sentenceIdx, sentence) in sentences.withIndex()) {
                if (!isActive) return@launch
                highlightFlow.value = sentenceIdx
                val utteranceId = "utt_${mode.name}_${System.currentTimeMillis()}_${sentenceIdx}"
                val tts = tts ?: return@launch
                tts.setSpeechRate(rate)
                val finished = CompletableDeferred<Unit>()
                val listener = object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { finished.complete(Unit) }
                    override fun onError(utteranceId: String?) { finished.complete(Unit) }
                }
                tts.setOnUtteranceProgressListener(listener)
                tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                finished.await()
                if (!isActive) return@launch
                delay(restDuration(sentence))
            }
            tts?.setOnUtteranceProgressListener(null)
            highlightFlow.value = null
            currentMode = TtsMode.NONE
        }
    }

    fun speakQuestion(text: String, rate: Float = 0.8f) {
        // 답변 하이라이트 초기화
        _currentAnswerSentenceIndex.value = null
        speakSentences(
            text = text,
            rate = rate,
            highlightFlow = _currentQuestionSentenceIndex,
            restDuration = { 400L },
            mode = TtsMode.QUESTION
        )
    }

    fun speakAnswer(text: String, rate: Float = 0.8f) {
        // 질문 하이라이트 초기화
        _currentQuestionSentenceIndex.value = null
        speakSentences(
            text = text,
            rate = rate,
            highlightFlow = _currentAnswerSentenceIndex,
            restDuration = { 400L },
            mode = TtsMode.ANSWER
        )
    }

    fun speakBySentence(text: String, repeatCount: Int = 5, pauseRatio: Float = 1.5f, rate: Float = 0.8f) {
        if (!isReady) return
        speakJob?.cancel()
        currentMode = TtsMode.ANSWER_BY_SENTENCE
        // 질문 하이라이트 초기화
        _currentQuestionSentenceIndex.value = null
        speakJob = CoroutineScope(Dispatchers.Main).launch {
            val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            for ((sentenceIdx, sentence) in sentences.withIndex()) {
                repeat(repeatCount) { i ->
                    if (!isActive) return@launch
                    _currentAnswerSentenceIndex.value = sentenceIdx
                    val utteranceId = "utt_abs_${System.currentTimeMillis()}_${i}"
                    val tts = tts ?: return@launch
                    tts.setSpeechRate(rate)
                    val finished = CompletableDeferred<Unit>()
                    val listener = object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) { finished.complete(Unit) }
                        override fun onError(utteranceId: String?) { finished.complete(Unit) }
                    }
                    tts.setOnUtteranceProgressListener(listener)
                    tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    finished.await()
                    if (!isActive) return@launch
                    delay(calcRestDuration(sentence, pauseRatio))
                }
            }
            tts?.setOnUtteranceProgressListener(null)
            _currentAnswerSentenceIndex.value = null
            currentMode = TtsMode.NONE
        }
    }

    fun stop() {
        speakJob?.cancel()
        tts?.stop()
        _currentQuestionSentenceIndex.value = null
        _currentAnswerSentenceIndex.value = null
        currentMode = TtsMode.NONE
    }

    fun shutdown() {
        tts?.shutdown()
    }

    // 쉬는시간 계산 함수 (문장 길이 기반, 비율 적용)
    fun calcRestDuration(sentence: String, ratio: Float): Long {
        val baseDuration = (sentence.length * 50L).coerceAtLeast(800L)
        return (baseDuration * ratio).toLong()
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