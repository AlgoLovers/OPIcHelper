package com.na982.opichelper.domain.audio

import com.na982.opichelper.domain.manager.AppLogger
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
    private val highlightStateHolder: HighlightStateHolder,
    private val logger: AppLogger
) : java.io.Closeable {
    companion object {
        private const val MERGED_AUDIO_DELAY_MS = 500L
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var currentPlayJob: Job? = null

    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()

    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    val questionHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.questionHighlight
    val answerHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.answerHighlight
    val answerKoHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.answerKoHighlight
    val recordingHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.recordingHighlight

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
                _isQuestionPlaying.value = true
                updateIsPlaying()
                ttsOrchestrator.speakWithHighlight(question) { index, sentence ->
                    highlightStateHolder.setQuestionHighlight(index, sentence)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("TtsPlaybackController", "질문 TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isQuestionPlaying.value = false
                    _isPaused.value = false
                    updateIsPlaying()
                    highlightStateHolder.setQuestionHighlight(null)
                }
            }
        }
    }

    fun playAnswer(answer: String) {
        stopAndReset(clearHighlight = false)
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                _isAnswerPlaying.value = true
                updateIsPlaying()
                ttsOrchestrator.speakWithHighlight(answer) { index, sentence ->
                    highlightStateHolder.setAnswerHighlight(index, sentence)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("TtsPlaybackController", "답변 TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isAnswerPlaying.value = false
                    _isPaused.value = false
                    updateIsPlaying()
                    highlightStateHolder.setAnswerHighlight(null)
                }
            }
        }
    }

    fun playMergedAudio(question: String, answer: String) {
        stopAndReset(clearHighlight = false)
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                _isQuestionPlaying.value = true
                updateIsPlaying()
                ttsOrchestrator.speakWithHighlight(question) { index, sentence ->
                    highlightStateHolder.setQuestionHighlight(index, sentence)
                }
                _isAnswerPlaying.value = true
                _isQuestionPlaying.value = false
                highlightStateHolder.setQuestionHighlight(null)

                kotlinx.coroutines.delay(MERGED_AUDIO_DELAY_MS)

                updateIsPlaying()
                ttsOrchestrator.speakWithHighlight(answer) { index, sentence ->
                    highlightStateHolder.setAnswerHighlight(index, sentence)
                }
                _isAnswerPlaying.value = false
                highlightStateHolder.setAnswerHighlight(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("TtsPlaybackController", "합쳐진 오디오 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isQuestionPlaying.value = false
                    _isAnswerPlaying.value = false
                    _isPaused.value = false
                    updateIsPlaying()
                    highlightStateHolder.setQuestionHighlight(null)
                    highlightStateHolder.setAnswerHighlight(null)
                }
            }
        }
    }

    fun stopTts() = stopAndReset(clearHighlight = true)

    private fun updateIsPlaying() {
        _isPlaying.value = _isQuestionPlaying.value || _isAnswerPlaying.value
    }

    private fun resetPlayState() {
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = false
        _isPaused.value = false
        updateIsPlaying()
    }

    fun stopAndMarkPaused() {
        currentPlayJob?.cancel()
        currentPlayJob = null
        ttsOrchestrator.stop()
        _isPaused.value = true
        resetPlayState()
        _isPaused.value = true
    }

    fun clearPausedState() {
        _isPaused.value = false
        highlightStateHolder.clearHighlight()
    }

    fun cleanupTts() {
        try {
            stopTts()
            ttsOrchestrator.releaseAllPlayers()
        } catch (e: Exception) {
            logger.e("TtsPlaybackController", "TTS 완전 정리 실패", e)
        }
    }

    override fun close() {
        cleanupTts()
        coroutineScope.cancel()
    }

    fun stopWithoutClearingHighlight() = stopAndReset(clearHighlight = false)

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
