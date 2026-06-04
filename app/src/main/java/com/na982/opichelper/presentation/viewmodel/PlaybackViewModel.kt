package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.manager.AppLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.na982.opichelper.domain.audio.HighlightInfo
import com.na982.opichelper.domain.audio.PipState
import com.na982.opichelper.domain.audio.PipStateAggregator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import com.na982.opichelper.domain.repository.PlaybackPreferences
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class PlaybackState(
    val hasEnglishWritingTestMergedFile: Boolean = false,
    val isEnglishWritingTestMergedFilePlaying: Boolean = false,
    val englishWritingTestMergedFileHighlightIndex: Int? = null,

    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val questionHighlight: HighlightInfo = HighlightInfo(),
    val answerHighlight: HighlightInfo = HighlightInfo(),
    val answerKoHighlight: HighlightInfo = HighlightInfo(),
    val recordingHighlight: HighlightInfo = HighlightInfo(),

    val hasProgress: Boolean = false
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val playMergedFileUseCase: PlayMergedFileUseCase,
    private val coordinator: MemorizationModeCoordinator,
    private val playbackPreferences: PlaybackPreferences,
    private val pipStateAggregator: PipStateAggregator,
    private val appLogger: AppLogger
) : ViewModel() {

    companion object {
        private const val ANSWER_PLAY_TIMEOUT_MS = 60_000L
    }

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    val pipState: StateFlow<PipState> = pipStateAggregator.pipState

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private suspend fun emitEvent(message: String) {
        _events.emit(message)
    }

    init {
        setupStateCombination()
        setupCoordinatorEventHandling()
    }

    private fun setupStateCombination() {
        viewModelScope.launch {
            combine(
                ttsPlaybackController.isPlaying,
                ttsPlaybackController.isQuestionPlaying,
                ttsPlaybackController.isAnswerPlaying,
                ttsPlaybackController.questionHighlight,
                ttsPlaybackController.answerHighlight,
                ttsPlaybackController.answerKoHighlight,
                ttsPlaybackController.recordingHighlight
            ) { values ->
                val playing = values[0] as Boolean
                _uiState.update { it.copy(
                    isPlaying = playing,
                    isQuestionPlaying = values[1] as Boolean,
                    isAnswerPlaying = values[2] as Boolean,
                    questionHighlight = values[3] as HighlightInfo,
                    answerHighlight = values[4] as HighlightInfo,
                    answerKoHighlight = values[5] as HighlightInfo,
                    recordingHighlight = values[6] as HighlightInfo
                ) }
            }.collect { }
        }

        viewModelScope.launch {
            combine(
                playMergedFileUseCase.hasFile,
                playMergedFileUseCase.isPlaying,
                playMergedFileUseCase.highlightIndex
            ) { hasFile, isPlaying, highlightIndex ->
                _uiState.update { it.copy(
                    hasEnglishWritingTestMergedFile = hasFile,
                    isEnglishWritingTestMergedFilePlaying = isPlaying,
                    englishWritingTestMergedFileHighlightIndex = highlightIndex
                ) }
            }.collect { }
        }
    }

    private fun setupCoordinatorEventHandling() {
        viewModelScope.launch {
            coordinator.events.collect { event ->
                when (event) {
                    is CoordinatorEvent.EnglishWritingCompleted -> {
                        stopEnglishWritingTestMergedFile()
                        checkEnglishWritingTestMergedFile()
                    }
                    is CoordinatorEvent.EnglishWritingStopped -> {
                        checkEnglishWritingTestMergedFile()
                    }
                    is CoordinatorEvent.RecordingStateChanged -> {}
                    is CoordinatorEvent.LevelChanged -> {}
                }
            }
        }
    }

    fun playEnglishWritingTestMergedFile() {
        playMergedFileUseCase.play()
    }

    fun stopEnglishWritingTestMergedFile() {
        playMergedFileUseCase.stop()
    }

    private fun checkEnglishWritingTestMergedFile() {
        playMergedFileUseCase.checkFile()
    }

    fun playQuestion(question: String) {
        pipStateAggregator.markQuestionPlayed()
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        pipStateAggregator.markAnswerPlayed()
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            val playCount = playbackPreferences.getAnswerPlayCount()
            for (i in 1..playCount) {
                ttsPlaybackController.playAnswer(answer)
                withTimeoutOrNull(ANSWER_PLAY_TIMEOUT_MS) {
                    ttsPlaybackController.isAnswerPlaying.first { !it }
                } ?: run {
                    appLogger.w("PlaybackViewModel", "답변 재생 완료 대기 타임아웃")
                    ttsPlaybackController.stopTts()
                }
            }
        }
    }

    fun stopTts() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            playMergedFileUseCase.stop()
            ttsPlaybackController.clearHighlight()
        }
    }

    fun cleanupAllTtsSync() {
        try {
            ttsPlaybackController.stopWithoutClearingHighlight()
            ttsPlaybackController.clearHighlight()
            playMergedFileUseCase.stop()
        } catch (e: Exception) {
            appLogger.e("PlaybackViewModel", "TTS 정리 중 오류", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pipStateAggregator.release()
        ttsPlaybackController.cleanupTts()
        playMergedFileUseCase.close()
    }

    // Delegated PiP methods
    fun onBackgroundMove() = pipStateAggregator.onBackgroundMove()
    fun onForegroundReturn() = pipStateAggregator.onForegroundReturn()
    fun setPipMode(isPip: Boolean) = pipStateAggregator.setPipMode(isPip)
    fun togglePlayPause() = pipStateAggregator.togglePlayPause()
    fun setActionListener(listener: PlaybackActionListener) = pipStateAggregator.setActionListener(listener)
    fun setHasNextItem(hasNext: Boolean) = pipStateAggregator.setHasNextItem(hasNext)
    fun repeatPlayback() = pipStateAggregator.repeatPlayback()
    fun playNextItem() = pipStateAggregator.playNextItem()
    fun stopPlayback() = pipStateAggregator.stopPlayback()
    fun shouldEnterPip(): Boolean = pipStateAggregator.shouldEnterPip()
    fun setFullMemorizationSentence(en: String?, ko: String?) = pipStateAggregator.setFullMemorizationSentence(en, ko)
    val lastMemorizationGroup get() = pipStateAggregator.lastMemorizationGroup
}
