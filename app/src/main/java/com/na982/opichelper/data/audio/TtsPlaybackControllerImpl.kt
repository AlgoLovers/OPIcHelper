package com.na982.opichelper.data.audio

import com.na982.opichelper.domain.audio.HighlightInfo
import com.na982.opichelper.domain.audio.HighlightStateHolder
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.manager.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class TtsPlaybackControllerImpl(
    private val ttsOrchestrator: TtsOrchestrator,
    private val highlightStateHolder: HighlightStateHolder,
    private val appLogger: AppLogger
) : TtsPlaybackController, java.io.Closeable {
    companion object {
        private const val MERGED_AUDIO_DELAY_MS = 500L
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var currentPlayJob: Job? = null

    private val _isQuestionPlaying = MutableStateFlow(false)
    override val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()

    private val _isAnswerPlaying = MutableStateFlow(false)
    override val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()

    override val isPlaying: StateFlow<Boolean> = combine(_isQuestionPlaying, _isAnswerPlaying) { q, a -> q || a }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    private val _isPaused = MutableStateFlow(false)
    override val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    override val questionHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.questionHighlight
    override val answerHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.answerHighlight
    override val answerKoHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.answerKoHighlight
    override val recordingHighlight: StateFlow<HighlightInfo> get() = highlightStateHolder.recordingHighlight

    private fun stopAndReset(clearHighlight: Boolean = true) {
        currentPlayJob?.cancel()
        currentPlayJob = null
        ttsOrchestrator.stop()
        resetPlayState()
        if (clearHighlight) highlightStateHolder.clearHighlight()
    }

    override fun playQuestion(question: String) {
        stopAndReset(clearHighlight = false)
        playInternal(question, _isQuestionPlaying, highlightStateHolder::setQuestionHighlight, "질문")
    }

    override fun playAnswer(answer: String) {
        stopAndReset(clearHighlight = false)
        playInternal(answer, _isAnswerPlaying, highlightStateHolder::setAnswerHighlight, "답변")
    }

    private fun playInternal(
        text: String,
        playingFlag: MutableStateFlow<Boolean>,
        highlightSetter: (Int?, String?) -> Unit,
        label: String
    ) {
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                playingFlag.update { true }
                ttsOrchestrator.speakWithHighlight(text) { index, sentence ->
                    highlightSetter(index, sentence)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appLogger.e("TtsPlaybackController", "${label} TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    playingFlag.update { false }
                    _isPaused.update { false }
                    highlightSetter(null, null)
                }
            }
        }
    }

    override fun stopTts() = stopAndReset(clearHighlight = true)

    private fun resetPlayState() {
        _isQuestionPlaying.update { false }
        _isAnswerPlaying.update { false }
        _isPaused.update { false }
    }

    override fun stopAndMarkPaused() {
        currentPlayJob?.cancel()
        currentPlayJob = null
        ttsOrchestrator.stop()
        _isQuestionPlaying.update { false }
        _isAnswerPlaying.update { false }
        _isPaused.update { true }
    }

    override fun clearPausedState() {
        _isPaused.update { false }
        highlightStateHolder.clearHighlight()
    }

    override fun cleanupTts() {
        try {
            stopTts()
            ttsOrchestrator.releaseAllPlayers()
        } catch (e: Exception) {
            appLogger.e("TtsPlaybackController", "TTS 완전 정리 실패", e)
        }
    }

    override fun reset() {
        stopTts()
    }

    override fun close() {
        cleanupTts()
        coroutineScope.cancel()
    }

    override fun stopWithoutClearingHighlight() = stopAndReset(clearHighlight = false)

    override fun setAnswerHighlightIndex(index: Int, sentence: String?) {
        highlightStateHolder.setAnswerHighlight(index, sentence)
    }

    override fun setAnswerKoHighlightIndex(index: Int, sentence: String?) {
        highlightStateHolder.setAnswerKoHighlight(index, sentence)
    }

    override fun setRecordingHighlightIndex(index: Int) {
        highlightStateHolder.setRecordingHighlight(index)
    }

    override fun clearHighlight() {
        highlightStateHolder.clearHighlight()
    }
}
