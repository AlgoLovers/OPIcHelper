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
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.ModeGroup
import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import com.na982.opichelper.domain.repository.TtsServiceController
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

    val isAnswerCardFlipped: Boolean = false,

    val hasProgress: Boolean = false
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val playMergedFileUseCase: PlayMergedFileUseCase,
    private val ttsOrchestrator: TtsOrchestrator,
    private val playbackPreferences: PlaybackPreferences,
    private val coordinator: MemorizationModeCoordinator,
    private val ttsServiceController: TtsServiceController,
    private val appLogger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    private val _pipState = MutableStateFlow(PipState())
    val pipState: StateFlow<PipState> = _pipState.asStateFlow()

    private val _fullMemorizationSentenceEn = MutableStateFlow<String?>(null)
    private val _fullMemorizationSentenceKo = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val events: SharedFlow<String> = _events.asSharedFlow()

    @Volatile
    private var lastPlayingTimestamp: Long = 0L

    @Volatile
    private var wasStoppedByUser: Boolean = false

    @Volatile
    private var _hasNextItem: Boolean = false
    @Volatile
    private var _actionListener: PlaybackActionListener? = null
    @Volatile
    internal var lastMemorizationGroup: ModeGroup? = null

    enum class LastPlayedType { QUESTION, ANSWER, NONE }
    @Volatile
    private var lastPlayedType: LastPlayedType = LastPlayedType.NONE

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
                if (playing) lastPlayingTimestamp = System.currentTimeMillis()
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

        viewModelScope.launch {
            combine(
                ttsPlaybackController.questionHighlight,
                ttsPlaybackController.answerHighlight,
                ttsPlaybackController.answerKoHighlight,
                ttsPlaybackController.isPlaying,
                ttsPlaybackController.isPaused,
                _fullMemorizationSentenceEn,
                _fullMemorizationSentenceKo,
                coordinator.isRunning,
                coordinator.currentMode,
                playMergedFileUseCase.isPlaying
            ) { values ->
                val questionSentence = (values[0] as HighlightInfo).sentence
                val answerSentence = (values[1] as HighlightInfo).sentence
                val answerKoSentence = (values[2] as HighlightInfo).sentence
                val isPlaying = values[3] as Boolean
                val isPaused = values[4] as Boolean
                val fmSentenceEn = values[5] as String?
                val fmSentenceKo = values[6] as String?
                val isMemorizationRunning = values[7] as Boolean
                val currentMode = values[8] as CurrentMode
                val isMergedFilePlaying = values[9] as Boolean
                val sentenceEn = fmSentenceEn ?: answerSentence ?: questionSentence
                val sentenceKo = fmSentenceKo ?: answerKoSentence
                val active = isPlaying || isMemorizationRunning || isMergedFilePlaying
                if (active) lastPlayingTimestamp = System.currentTimeMillis()
                if (isMemorizationRunning && currentMode != CurrentMode.NONE) {
                    lastMemorizationGroup = currentMode.group
                }
                _pipState.update { prev ->
                    val wasPlaying = prev.isPlaying
                    val completed = wasPlaying && !active && !wasStoppedByUser
                    if (completed) wasStoppedByUser = false
                    prev.copy(
                        currentSentenceEn = if (sentenceEn != null) sentenceEn else if (active) prev.currentSentenceEn else null,
                        currentSentenceKo = if (sentenceKo != null) sentenceKo else if (active) prev.currentSentenceKo else null,
                        isPlaying = active,
                        isPaused = if (active) isPaused else false,
                        isPausable = !isMergedFilePlaying,
                        hasCompleted = if (active) false else (completed || prev.hasCompleted),
                        hasNextItem = _hasNextItem
                    )
                }
                updateNotificationSentence(sentenceEn, sentenceKo)
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
                    is CoordinatorEvent.RecordingStateChanged -> {
                        // FullMemorizationViewModel에서 직접 처리
                    }
                    is CoordinatorEvent.LevelChanged -> {
                        // BaseMemorizationViewModel.onLevelChanged()에서 처리
                    }
                }
            }
        }
    }

    fun setAnswerCardFlipped(isFlipped: Boolean) {
        _uiState.update { it.copy(isAnswerCardFlipped = isFlipped) }
    }

    fun playEnglishWritingTestMergedFile() {
        playMergedFileUseCase.play()
    }

    fun stopEnglishWritingTestMergedFile() {
        playMergedFileUseCase.stop()
    }

    fun checkEnglishWritingTestMergedFile() {
        playMergedFileUseCase.checkFile()
    }

    fun playQuestion(question: String) {
        wasStoppedByUser = false
        lastPlayedType = LastPlayedType.QUESTION
        lastMemorizationGroup = null
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        wasStoppedByUser = false
        lastPlayedType = LastPlayedType.ANSWER
        lastMemorizationGroup = null
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            val playCount = playbackPreferences.getAnswerPlayCount()
            for (i in 1..playCount) {
                ttsPlaybackController.playAnswer(answer)
                withTimeoutOrNull(60_000L) {
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
        _actionListener = null
        ttsPlaybackController.cleanupTts()
        playMergedFileUseCase.stop()
        playMergedFileUseCase.release()
    }

    fun onBackgroundMove() {
        if (_uiState.value.isPlaying || coordinator.isRunning.value || playMergedFileUseCase.isPlaying.value) {
            ttsServiceController.startForegroundService()
        }
    }

    fun onForegroundReturn() {
        ttsServiceController.stopForegroundService()
    }

    fun setPipMode(isPip: Boolean) {
        if (!isPip) wasStoppedByUser = false
        _pipState.update { it.copy(
            isPipMode = isPip,
            hasCompleted = if (!isPip) false else it.hasCompleted
        ) }
    }

    fun togglePlayPause() {
        if (ttsPlaybackController.isPaused.value) {
            wasStoppedByUser = false
            ttsPlaybackController.clearPausedState()
        } else if (_uiState.value.isPlaying || coordinator.isRunning.value) {
            wasStoppedByUser = true
            _pipState.update { it.copy(hasCompleted = false) }
            ttsPlaybackController.stopAndMarkPaused()
        }
    }

    fun setActionListener(listener: PlaybackActionListener) {
        _actionListener = listener
    }

    fun setHasNextItem(hasNext: Boolean) { _hasNextItem = hasNext }

    fun repeatPlayback() {
        wasStoppedByUser = false
        _pipState.update { it.copy(hasCompleted = false) }
        if (lastMemorizationGroup != null) {
            _actionListener?.onRepeatMemorization()
            return
        }
        when (lastPlayedType) {
            LastPlayedType.QUESTION -> _actionListener?.onRepeatQuestion()
            LastPlayedType.ANSWER -> _actionListener?.onRepeatAnswer()
            LastPlayedType.NONE -> {}
        }
    }

    fun playNextItem() {
        wasStoppedByUser = false
        _pipState.update { it.copy(hasCompleted = false) }
        if (lastMemorizationGroup != null) {
            _actionListener?.onNextAndRestart()
            return
        }
        _actionListener?.onNext()
    }

    fun stopPlayback() {
        wasStoppedByUser = true
        _pipState.update { it.copy(hasCompleted = false) }
        if (coordinator.isRunning.value) {
            _actionListener?.onStopMemorization()
        } else {
            ttsPlaybackController.stopTts()
        }
    }

    fun shouldEnterPip(): Boolean {
        val isPlaying = _uiState.value.isPlaying
        val isCoordinatorRunning = coordinator.isRunning.value
        val isMergedPlaying = playMergedFileUseCase.isPlaying.value
        val isTtsSpeaking = ttsPlaybackController.isPlaying.value || ttsOrchestrator.isSpeaking.value
        val recentlyPlayed = System.currentTimeMillis() - lastPlayingTimestamp < 30_000L
        return isPlaying || isCoordinatorRunning || isMergedPlaying || isTtsSpeaking || recentlyPlayed
    }

    fun isCoordinatorRunning(): Boolean = coordinator.isRunning.value

    fun isMergedFilePlaying(): Boolean = playMergedFileUseCase.isPlaying.value

    fun setFullMemorizationSentence(en: String?, ko: String?) {
        _fullMemorizationSentenceEn.value = en
        _fullMemorizationSentenceKo.value = ko
    }

    private fun updateNotificationSentence(sentenceEn: String?, sentenceKo: String?) {
        if (_uiState.value.isPlaying || coordinator.isRunning.value) {
            ttsServiceController.updateNotificationSentence(sentenceEn, sentenceKo)
        }
    }
}
