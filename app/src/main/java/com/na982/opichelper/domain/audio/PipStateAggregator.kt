package com.na982.opichelper.domain.audio

import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import com.na982.opichelper.domain.repository.TtsServiceController
import com.na982.opichelper.domain.usecase.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.ModeGroup
import com.na982.opichelper.presentation.viewmodel.PlaybackActionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class PipStateAggregator @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val coordinator: MemorizationModeCoordinator,
    private val playMergedFileUseCase: PlayMergedFileUseCase,
    private val ttsServiceController: TtsServiceController
) {
    companion object {
        private const val PIP_RECENTLY_PLAYED_THRESHOLD_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _pipState = MutableStateFlow(PipState())
    val pipState: StateFlow<PipState> = _pipState.asStateFlow()

    private val _fullMemorizationSentenceEn = MutableStateFlow<String?>(null)
    private val _fullMemorizationSentenceKo = MutableStateFlow<String?>(null)

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

    init {
        setupPipStateCombination()
    }

    private fun setupPipStateCombination() {
        scope.launch {
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
        } else if (_pipState.value.isPlaying || coordinator.isRunning.value) {
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
        val isPlaying = _pipState.value.isPlaying
        val isCoordinatorRunning = coordinator.isRunning.value
        val isMergedPlaying = playMergedFileUseCase.isPlaying.value
        val recentlyPlayed = System.currentTimeMillis() - lastPlayingTimestamp < PIP_RECENTLY_PLAYED_THRESHOLD_MS
        return isPlaying || isCoordinatorRunning || isMergedPlaying || recentlyPlayed
    }

    fun setFullMemorizationSentence(en: String?, ko: String?) {
        _fullMemorizationSentenceEn.update { en }
        _fullMemorizationSentenceKo.update { ko }
    }

    fun markQuestionPlayed() {
        wasStoppedByUser = false
        lastPlayedType = LastPlayedType.QUESTION
        lastMemorizationGroup = null
    }

    fun markAnswerPlayed() {
        wasStoppedByUser = false
        lastPlayedType = LastPlayedType.ANSWER
        lastMemorizationGroup = null
    }

    fun onBackgroundMove() {
        if (_pipState.value.isPlaying || coordinator.isRunning.value || playMergedFileUseCase.isPlaying.value) {
            ttsServiceController.startForegroundService()
        }
    }

    fun onForegroundReturn() {
        ttsServiceController.stopForegroundService()
    }

    fun release() {
        _actionListener = null
    }

    private fun updateNotificationSentence(sentenceEn: String?, sentenceKo: String?) {
        if (_pipState.value.isPlaying || coordinator.isRunning.value) {
            ttsServiceController.updateNotificationSentence(sentenceEn, sentenceKo)
        }
    }
}
