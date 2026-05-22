package com.na982.opichelper.domain.audio

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPlaybackController @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val highlightStateHolder: HighlightStateHolder
) : java.io.Closeable {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var currentPlayJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()

    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()

    // Highlight state delegated to HighlightStateHolder
    val questionHighlightIndex: StateFlow<Int?> get() = highlightStateHolder.questionHighlightIndex
    val answerHighlightIndex: StateFlow<Int?> get() = highlightStateHolder.answerHighlightIndex
    val answerKoHighlightIndex: StateFlow<Int?> get() = highlightStateHolder.answerKoHighlightIndex
    val recordingHighlightIndex: StateFlow<Int?> get() = highlightStateHolder.recordingHighlightIndex
    val currentQuestionSentence: StateFlow<String?> get() = highlightStateHolder.currentQuestionSentence
    val currentAnswerSentence: StateFlow<String?> get() = highlightStateHolder.currentAnswerSentence
    val currentAnswerKoSentence: StateFlow<String?> get() = highlightStateHolder.currentAnswerKoSentence

    private fun stopAndReset(clearHighlight: Boolean = true) {
        currentPlayJob?.cancel()
        currentPlayJob = null
        ttsOrchestrator.stop()
        resetPlayState()
        if (clearHighlight) highlightStateHolder.clearHighlight()
    }

    fun playQuestion(question: String) {
        stopAndReset(clearHighlight = false)
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                _isPlaying.value = true
                _isQuestionPlaying.value = true

                val sentences = SentenceSplitter.split(question)
                ttsOrchestrator.speakWithHighlight(question) { index ->
                    highlightStateHolder.setQuestionHighlight(index ?: -1, index?.let { sentences.getOrNull(it) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "질문 TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isPlaying.value = false
                    _isPaused.value = false
                    _isQuestionPlaying.value = false
                    highlightStateHolder.setQuestionHighlight(-1)
                }
            }
        }
    }

    fun playAnswer(answer: String) {
        stopAndReset(clearHighlight = false)
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                _isPlaying.value = true
                _isAnswerPlaying.value = true

                val sentences = SentenceSplitter.split(answer)
                ttsOrchestrator.speakWithHighlight(answer) { index ->
                    highlightStateHolder.setAnswerHighlight(index ?: -1, index?.let { sentences.getOrNull(it) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "답변 TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isPlaying.value = false
                    _isPaused.value = false
                    _isAnswerPlaying.value = false
                    highlightStateHolder.setAnswerHighlight(-1)
                }
            }
        }
    }

    fun playMergedAudio(question: String, answer: String) {
        stopAndReset(clearHighlight = false)
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                _isPlaying.value = true

                _isQuestionPlaying.value = true
                val questionSentences = SentenceSplitter.split(question)
                ttsOrchestrator.speakWithHighlight(question) { index ->
                    highlightStateHolder.setQuestionHighlight(index ?: -1, index?.let { questionSentences.getOrNull(it) })
                }
                _isQuestionPlaying.value = false
                highlightStateHolder.setQuestionHighlight(-1)

                kotlinx.coroutines.delay(500)

                _isAnswerPlaying.value = true
                val answerSentences = SentenceSplitter.split(answer)
                ttsOrchestrator.speakWithHighlight(answer) { index ->
                    highlightStateHolder.setAnswerHighlight(index ?: -1, index?.let { answerSentences.getOrNull(it) })
                }
                _isAnswerPlaying.value = false
                highlightStateHolder.setAnswerHighlight(-1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "합쳐진 오디오 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isPlaying.value = false
                    _isPaused.value = false
                    _isQuestionPlaying.value = false
                    _isAnswerPlaying.value = false
                    highlightStateHolder.setQuestionHighlight(-1)
                    highlightStateHolder.setAnswerHighlight(-1)
                }
            }
        }
    }

    fun stopTts() = stopAndReset(clearHighlight = true)

    private fun resetPlayState() {
        _isPlaying.value = false
        _isPaused.value = false
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = false
    }

    fun pauseTts() {
        try {
            ttsOrchestrator.pause()
            _isPaused.value = true
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 일시 중지 실패", e)
        }
    }

    fun resumeTts() {
        try {
            ttsOrchestrator.resume()
            _isPaused.value = false
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 재개 실패", e)
        }
    }

    fun cleanupTts() {
        try {
            stopTts()
            ttsOrchestrator.releaseAllPlayers()
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 완전 정리 실패", e)
        }
    }

    override fun close() {
        cleanupTts()
        coroutineScope.cancel()
    }

    fun forceStopTts() = stopAndReset(clearHighlight = false)

    fun getCurrentKoreanTtsServiceName(): String {
        return ttsOrchestrator.getCurrentKoreanTtsServiceName()
    }

    fun setQuestionHighlightIndex(index: Int) {
        highlightStateHolder.setQuestionHighlight(index)
    }

    fun setAnswerHighlightIndex(index: Int, sentence: String? = null) {
        highlightStateHolder.setAnswerHighlight(index, sentence)
    }

    fun setAnswerKoHighlightIndex(index: Int, sentence: String? = null) {
        highlightStateHolder.setAnswerKoHighlight(index, sentence)
    }

    fun setRecordingHighlightIndex(index: Int) {
        highlightStateHolder.setRecordingHighlight(index)
    }

    fun clearHighlight() {
        highlightStateHolder.clearHighlight()
    }
}
