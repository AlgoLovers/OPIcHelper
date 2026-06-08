package com.na982.opichelper.domain.audio

import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.TtsServiceController
import com.na982.opichelper.domain.entity.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.entity.ModeGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val ttsServiceController: TtsServiceController,
    private val repeatListeningRepository: RepeatListeningRepository
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
    private var _lastMemorizationGroup: ModeGroup? = null
    val lastMemorizationGroup: ModeGroup? get() = _lastMemorizationGroup

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
                playMergedFileUseCase.isPlaying,
                repeatListeningRepository.repeatProgress
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
                val repeatProgress = values[10] as RepeatListeningProgress?
                val sentenceEn = fmSentenceEn ?: answerSentence ?: questionSentence
                val sentenceKo = fmSentenceKo ?: answerKoSentence
                val active = isPlaying || isMemorizationRunning || isMergedFilePlaying
                if (active) lastPlayingTimestamp = System.currentTimeMillis()
                if (isMemorizationRunning && currentMode != CurrentMode.NONE) {
                    _lastMemorizationGroup = currentMode.group
                }
                val isRepeatListeningMode = currentMode == CurrentMode.REPEAT_LISTENING && isMemorizationRunning
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
                        hasNextItem = _hasNextItem,
                        sentenceIndex = repeatProgress?.sentenceIndex ?: 0,
                        totalSentences = repeatProgress?.totalSentences ?: 0,
                        currentRepetition = repeatProgress?.currentRepetition ?: 0,
                        totalRepetitions = repeatProgress?.totalRepetitions ?: 0,
                        isRepeatListeningMode = isRepeatListeningMode
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
        val listener = _actionListener
        if (_lastMemorizationGroup != null) {
            listener?.onRepeatMemorization()
            return
        }
        when (lastPlayedType) {
            LastPlayedType.QUESTION -> listener?.onRepeatQuestion()
            LastPlayedType.ANSWER -> listener?.onRepeatAnswer()
            LastPlayedType.NONE -> {}
        }
    }

    fun playNextItem() {
        wasStoppedByUser = false
        _pipState.update { it.copy(hasCompleted = false) }
        val listener = _actionListener
        if (_lastMemorizationGroup != null) {
            listener?.onNextAndRestart()
            return
        }
        listener?.onNext()
    }

    fun stopPlayback() {
        wasStoppedByUser = true
        _pipState.update { it.copy(hasCompleted = false) }
        val listener = _actionListener
        if (coordinator.isRunning.value) {
            listener?.onStopMemorization()
        } else {
            ttsPlaybackController.stopTts()
        }
    }

    fun repeatCurrentSentence() {
        val listener = _actionListener
        listener?.onRepeatCurrentSentence()
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
        _lastMemorizationGroup = null
    }

    fun markAnswerPlayed() {
        wasStoppedByUser = false
        lastPlayedType = LastPlayedType.ANSWER
        _lastMemorizationGroup = null
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
        scope.cancel()
    }

    private fun updateNotificationSentence(sentenceEn: String?, sentenceKo: String?) {
        if (_pipState.value.isPlaying || coordinator.isRunning.value) {
            ttsServiceController.updateNotificationSentence(sentenceEn, sentenceKo)
        }
    }
}
