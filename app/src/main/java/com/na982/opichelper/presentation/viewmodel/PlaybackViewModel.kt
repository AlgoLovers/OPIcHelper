package com.na982.opichelper.presentation.viewmodel

import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.na982.opichelper.domain.audio.HighlightInfo
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import com.na982.opichelper.service.TtsForegroundService
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application

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
    private val ttsOrchestrator: com.na982.opichelper.domain.audio.TtsOrchestrator,
    private val userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository,
    private val coordinator: MemorizationModeCoordinator,
    private val application: Application
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
                _uiState.value = _uiState.value.copy(
                    isPlaying = playing,
                    isQuestionPlaying = values[1] as Boolean,
                    isAnswerPlaying = values[2] as Boolean,
                    questionHighlight = values[3] as HighlightInfo,
                    answerHighlight = values[4] as HighlightInfo,
                    answerKoHighlight = values[5] as HighlightInfo,
                    recordingHighlight = values[6] as HighlightInfo
                )
            }.collect { }
        }

        viewModelScope.launch {
            combine(
                playMergedFileUseCase.hasFile,
                playMergedFileUseCase.isPlaying,
                playMergedFileUseCase.highlightIndex
            ) { hasFile, isPlaying, highlightIndex ->
                _uiState.value = _uiState.value.copy(
                    hasEnglishWritingTestMergedFile = hasFile,
                    isEnglishWritingTestMergedFilePlaying = isPlaying,
                    englishWritingTestMergedFileHighlightIndex = highlightIndex
                )
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
                val isMergedFilePlaying = values[8] as Boolean
                val sentenceEn = fmSentenceEn ?: answerSentence ?: questionSentence
                val sentenceKo = fmSentenceKo ?: answerKoSentence
                val active = isPlaying || isMemorizationRunning || isMergedFilePlaying
                if (active) lastPlayingTimestamp = System.currentTimeMillis()
                _pipState.value = _pipState.value.copy(
                    currentSentenceEn = if (sentenceEn != null) sentenceEn else if (active) _pipState.value.currentSentenceEn else null,
                    currentSentenceKo = if (sentenceKo != null) sentenceKo else if (active) _pipState.value.currentSentenceKo else null,
                    isPlaying = active,
                    isPaused = if (active) isPaused else false,
                    isPausable = !isMergedFilePlaying
                )
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
        _uiState.value = _uiState.value.copy(isAnswerCardFlipped = isFlipped)
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
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            val playCount = userPreferencesRepository.getAnswerPlayCount()
            for (i in 1..playCount) {
                ttsPlaybackController.playAnswer(answer)
                withTimeoutOrNull(60_000L) {
                    ttsPlaybackController.isAnswerPlaying.first { !it }
                } ?: run {
                    Log.w("PlaybackViewModel", "답변 재생 완료 대기 타임아웃")
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
            Log.e("PlaybackViewModel", "TTS 정리 중 오류", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsPlaybackController.cleanupTts()
        playMergedFileUseCase.stop()
        playMergedFileUseCase.release()
    }

    @Suppress("NewApi")
    fun onBackgroundMove() {
        if (_uiState.value.isPlaying || coordinator.isRunning.value || playMergedFileUseCase.isPlaying.value) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                application.startForegroundService(TtsForegroundService.startIntent(application))
            } else {
                application.startService(TtsForegroundService.startIntent(application))
            }
        }
    }

    @Suppress("NewApi")
    fun onForegroundReturn() {
        application.stopService(TtsForegroundService.stopIntent(application))
    }

    fun setPipMode(isPip: Boolean) {
        _pipState.value = _pipState.value.copy(isPipMode = isPip)
    }

    fun togglePlayPause() {
        if (ttsPlaybackController.isPaused.value) {
            ttsPlaybackController.resumeTts()
        } else if (_uiState.value.isPlaying || coordinator.isRunning.value) {
            ttsPlaybackController.pauseTts()
        }
    }

    private var _stopMemorizationCallback: (() -> Unit)? = null

    fun setStopMemorizationCallback(callback: () -> Unit) {
        _stopMemorizationCallback = callback
    }

    fun stopPlayback() {
        if (coordinator.isRunning.value) {
            _stopMemorizationCallback?.invoke()
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

    @Suppress("NewApi")
    private fun updateNotificationSentence(sentenceEn: String?, sentenceKo: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && (_uiState.value.isPlaying || coordinator.isRunning.value)
        ) {
            application.startService(
                TtsForegroundService.updateSentenceIntent(application, sentenceEn, sentenceKo)
            )
        }
    }
}
