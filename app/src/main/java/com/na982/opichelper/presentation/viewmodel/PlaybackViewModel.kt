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
import com.na982.opichelper.domain.audio.PlaybackActionListener
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
    val recordingHighlight: HighlightInfo = HighlightInfo()
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val playMergedFileUseCase: PlayMergedFileUseCase,
    private val coordinator: MemorizationModeCoordinator,
    private val playbackPreferences: PlaybackPreferences,
    private val _pipStateAggregator: PipStateAggregator,
    private val appLogger: AppLogger
) : ViewModel() {

    companion object {
        private const val ANSWER_PLAY_TIMEOUT_MS = 60_000L
        private const val ANSWER_PLAY_START_TIMEOUT_MS = 3_000L
    }

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    val pipState: StateFlow<PipState> = _pipStateAggregator.pipState

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
        _pipStateAggregator.markQuestionPlayed()
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        _pipStateAggregator.markAnswerPlayed()
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            val playCount = playbackPreferences.getAnswerPlayCount()
            for (i in 1..playCount) {
                ttsPlaybackController.playAnswer(answer)
                // playAnswer()는 재생 job을 비동기로 launch만 하고 즉시 반환한다.
                // 곧바로 isAnswerPlaying이 false가 되기를 기다리면, 아직 false인 현재값에
                // 즉시 통과되어 루프가 대기 없이 돌고 각 반복이 직전 재생을 취소한다
                // (= 설정한 playCount와 무관하게 1회만 재생됨). 재생이 실제로 시작(true)될
                // 때까지 먼저 기다린 뒤, 완료(false)를 기다려야 N회 반복이 동작한다.
                val started = withTimeoutOrNull(ANSWER_PLAY_START_TIMEOUT_MS) {
                    ttsPlaybackController.isAnswerPlaying.first { it }
                } != null
                if (!started) {
                    appLogger.w("PlaybackViewModel", "답변 재생 시작 대기 타임아웃 — 중단")
                    break
                }
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
        // pipStateAggregator는 @Singleton이므로 여기서 release하지 않는다. 두 스코프의
        // PlaybackViewModel이 공유하는 인스턴스라, 한쪽 VM이 clear될 때 스코프를 취소하면
        // 다른 쪽의 PiP 집계가 깨진다. 앱 수명 동안 유지된다.
        ttsPlaybackController.reset()
        playMergedFileUseCase.reset()
    }

    val pipStateAggregator: PipStateAggregator get() = _pipStateAggregator
}
