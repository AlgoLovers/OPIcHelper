package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.usecase.*
import com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.ExecuteRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val executeRepeatListeningUseCase: ExecuteRepeatListeningUseCase,
    private val executeEnglishWritingTestUseCase: ExecuteEnglishWritingTestUseCase,
    private val fullMemorizationUseCase: FullMemorizationUseCase,
    private val progressTracker: MemorizeTestProgressTracker,
    private val userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
) : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        fullMemorizationUseCase.cancelPlayback()
    }

    private val _uiState = MutableStateFlow(MemorizationUiState())
    val uiState: StateFlow<MemorizationUiState> = _uiState.asStateFlow()

    private var currentUseCaseJob: Job? = null
    private var eventCollectJob: Job? = null

    private fun startMode(mode: CurrentMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode, isRunning = true)
    }

    private fun stopMode() {
        _uiState.value = _uiState.value.copy(isRunning = false, currentMode = CurrentMode.NONE)
    }

    fun onMemorizeTestButtonClick(selectedLevel: String) {
        viewModelScope.launch {
            try {
                val level = MemorizeLevel.fromDisplayName(selectedLevel)

                when (level) {
                    MemorizeLevel.REPEAT_LISTENING -> {
                        if (_uiState.value.isRepeatListeningRunning) {
                            stopRepeatListening()
                        } else {
                            startMode(CurrentMode.REPEAT_LISTENING)
                            ttsPlaybackController.stopTts()
                            ttsPlaybackController.clearHighlight()
                            startRepeatListening()
                        }
                    }
                    MemorizeLevel.ENGLISH_WRITING -> {
                        if (_uiState.value.isEnglishWritingTestRunning) {
                            stopEnglishWritingTest()
                        } else {
                            startMode(CurrentMode.ENGLISH_WRITING)
                            ttsPlaybackController.stopTts()
                            ttsPlaybackController.clearHighlight()
                            _uiState.value = _uiState.value.copy(stopEnglishWritingTestMergedFilePlaying = true)
                            startEnglishWritingTest()
                        }
                    }
                    MemorizeLevel.FULL_MEMORIZATION -> {
                        startMode(CurrentMode.FULL_MEMORIZATION)
                        startFullMemorizationMode()
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기 테스트 시작 실패", e)
                stopMode()
            }
        }
    }

    private fun startRepeatListening() {
        viewModelScope.launch {
            try {
                currentUseCaseJob?.cancel()
                eventCollectJob?.cancel()
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    eventCollectJob = viewModelScope.launch {
                        executeRepeatListeningUseCase.events.collect { event ->
                            handleRepeatListeningEvent(event)
                        }
                    }
                    currentUseCaseJob = launch {
                        val repeatListeningData = RepeatListeningData(
                            category = currentItem.category,
                            scriptIndex = qaDataManager.getCurrentIndex(),
                            koreanAnswer = qaDataManager.getCurrentAnswerKo(currentItem),
                            englishAnswer = qaDataManager.getCurrentAnswer(currentItem)
                        )
                        executeRepeatListeningUseCase.execute(
                            data = repeatListeningData,
                            repeatCount = userPreferencesRepository.getRepeatListeningCount()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복 듣기 시작 실패", e)
                stopMode()
            }
        }
    }

    private fun handleRepeatListeningEvent(event: MemorizeTestEvent) {
        if (currentUseCaseJob?.isActive != true) return
        when (event) {
            is MemorizeTestEvent.CardFlip -> {
                _uiState.value = _uiState.value.copy(isRepeatListeningCardFlipped = event.isKorean)
            }
            is MemorizeTestEvent.Highlight -> {
                if (event.index != null) {
                    ttsPlaybackController.setAnswerHighlightIndex(event.index)
                } else {
                    ttsPlaybackController.clearHighlight()
                }
            }
            is MemorizeTestEvent.KoreanHighlight -> {
                if (event.index != null) {
                    ttsPlaybackController.setAnswerKoHighlightIndex(event.index)
                } else {
                    ttsPlaybackController.clearHighlight()
                }
            }
            is MemorizeTestEvent.Completed -> {
                if (userPreferencesRepository.isAutoAdvance()) {
                    handleAutoAdvance()
                } else {
                    stopMode()
                }
            }
            else -> {}
        }
    }

    private fun handleAutoAdvance() {
        viewModelScope.launch {
            val hasMore = qaDataManager.hasNextQaItem()
            if (hasMore) {
                qaDataManager.nextQaItem()
                stopMode()
                startMode(CurrentMode.REPEAT_LISTENING)
                ttsPlaybackController.stopTts()
                ttsPlaybackController.clearHighlight()
                startRepeatListening()
            } else {
                stopMode()
            }
        }
    }

    private fun startEnglishWritingTest() {
        val currentItem = qaDataManager.currentQaItem.value
        if (currentItem != null) {
            val scriptIndex = qaDataManager.getCurrentIndex()

            eventCollectJob?.cancel()
            eventCollectJob = viewModelScope.launch {
                executeEnglishWritingTestUseCase.events.collect { event ->
                    handleEnglishWritingTestEvent(event)
                }
            }

            currentUseCaseJob = viewModelScope.launch {
                try {
                    executeEnglishWritingTestUseCase.execute(
                        answerKo = qaDataManager.getCurrentAnswerKo(currentItem),
                        answerEn = qaDataManager.getCurrentAnswer(currentItem),
                        category = currentItem.category,
                        scriptIndex = scriptIndex
                    )
                } catch (e: Exception) {
                    Log.e("MemorizationViewModel", "영작 테스트 실행 중 오류", e)
                    stopMode()
                }
            }
        }
    }

    private fun handleEnglishWritingTestEvent(event: MemorizeTestEvent) {
        if (currentUseCaseJob?.isActive != true) return
        when (event) {
            is MemorizeTestEvent.CardFlip -> {
                _uiState.value = _uiState.value.copy(
                    isEnglishWritingTestCardFlipped = event.isKorean
                )
            }
            is MemorizeTestEvent.KoreanHighlight -> {
                if (event.index != null) {
                    ttsPlaybackController.setAnswerKoHighlightIndex(event.index)
                } else {
                    ttsPlaybackController.clearHighlight()
                }
            }
            is MemorizeTestEvent.RecordingHighlight -> {
                if (event.index != null) {
                    ttsPlaybackController.setRecordingHighlightIndex(event.index)
                } else {
                    ttsPlaybackController.clearHighlight()
                }
            }
            is MemorizeTestEvent.RecordingStateChange -> {
                if (!event.isRecording) {
                    viewModelScope.launch {
                        updateFullMemorizationRecordingStatus()
                    }
                }
            }
            is MemorizeTestEvent.MergedFileCreated -> {
                _uiState.value = _uiState.value.copy(englishWritingTestCompleted = true)
                stopMode()
            }
            else -> {}
        }
    }

    fun startFullMemorizationMode() {
        viewModelScope.launch {
            try {
                currentUseCaseJob?.cancel()
                checkFullMemorizationRecordingStatus()

                val category = qaDataManager.getCurrentCategory() ?: ""
                val scriptIndex = qaDataManager.getCurrentIndex()

                fullMemorizationUseCase.startFullMemorization(
                    category = category,
                    scriptIndex = scriptIndex
                )
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 모드 시작 실패", e)
                stopMode()
            }
        }
    }

    fun stopFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.stopRecording()
                updateFullMemorizationRecordingStatus()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 종료 실패", e)
            }
        }
    }

    fun playFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                if (fullMemorizationUseCase.hasRecording()) {
                    startMode(CurrentMode.FULL_MEMORIZATION_PLAYING)
                    fullMemorizationUseCase.playRecordingWithHighlight()
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 재생 실패", e)
                startMode(CurrentMode.FULL_MEMORIZATION)
            }
        }
    }

    fun stopFullMemorizationPlaying() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.cancelPlayback()
                startMode(CurrentMode.FULL_MEMORIZATION)
                updateFullMemorizationRecordingStatus()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 재생 중지 실패", e)
                startMode(CurrentMode.FULL_MEMORIZATION)
            }
        }
    }

    suspend fun hasFullMemorizationRecording(): Boolean {
        return _uiState.value.hasFullMemorizationRecording
    }

    fun updateFullMemorizationRecordingStatus() {
        viewModelScope.launch {
            try {
                val hasRecording = fullMemorizationUseCase.hasRecording()
                val currentMode = _uiState.value.currentMode
                if (currentMode in setOf(
                        CurrentMode.FULL_MEMORIZATION,
                        CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
                        CurrentMode.FULL_MEMORIZATION_RECORDING,
                        CurrentMode.FULL_MEMORIZATION_PLAYING,
                        CurrentMode.FULL_MEMORIZATION_WITH_FILE
                    )) {
                    _uiState.value = _uiState.value.copy(
                        currentMode = if (hasRecording) CurrentMode.FULL_MEMORIZATION_WITH_FILE else CurrentMode.FULL_MEMORIZATION
                    )
                }
                _uiState.value = _uiState.value.copy(hasFullMemorizationRecordingFile = hasRecording)
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 파일 상태 확인 실패", e)
            }
        }
    }

    private suspend fun checkFullMemorizationRecordingStatus() {
        try {
            val hasRecording = fullMemorizationUseCase.hasRecording()
            _uiState.value = _uiState.value.copy(
                currentMode = if (hasRecording) CurrentMode.FULL_MEMORIZATION_WITH_FILE else CurrentMode.FULL_MEMORIZATION,
                hasFullMemorizationRecordingFile = hasRecording
            )
        } catch (e: Exception) {
            Log.e("MemorizationViewModel", "통암기 녹음 파일 상태 확인 실패", e)
        }
    }

    fun resetStateOnAppRestart() {
        _uiState.value = _uiState.value.copy(currentMode = CurrentMode.NONE, isRunning = false)
    }

    fun deleteFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.clearRecording()
                updateFullMemorizationRecordingStatus()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 파일 삭제 실패", e)
            }
        }
    }

    fun stopMemorization() {
        when {
            _uiState.value.isRepeatListeningRunning -> stopRepeatListening()
            _uiState.value.isEnglishWritingTestRunning -> stopEnglishWritingTest()
            _uiState.value.isFullMemorizationMode -> {
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                stopMode()
                viewModelScope.launch {
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                }
            }
            else -> {
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                stopMode()
                viewModelScope.launch {
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                }
            }
        }
    }

    fun stopRepeatListening() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        stopMode()

        _uiState.value = _uiState.value.copy(isRepeatListeningCardFlipped = false)

        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }

        viewModelScope.launch {
            try {
                progressTracker.persistChangedProgress()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복듣기 진행상황 저장 실패", e)
            }
        }
    }

    fun resetEnglishWritingTestCompleted() {
        _uiState.value = _uiState.value.copy(englishWritingTestCompleted = false)
    }

    fun resetStopEnglishWritingTestMergedFilePlaying() {
        _uiState.value = _uiState.value.copy(stopEnglishWritingTestMergedFilePlaying = false)
    }

    fun stopEnglishWritingTest() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        stopMode()

        _uiState.value = _uiState.value.copy(isEnglishWritingTestCardFlipped = false)

        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }

        viewModelScope.launch {
            try {
                progressTracker.persistChangedProgress()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "영작테스트 진행상황 저장 실패", e)
            }
        }
    }

    fun onMemorizeLevelChanged() {
        viewModelScope.launch {
            try {
                progressTracker.persistChangedProgress()

                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                eventCollectJob?.cancel()
                eventCollectJob = null
                stopMode()

                _uiState.value = _uiState.value.copy(
                    englishWritingTestCompleted = false,
                    stopEnglishWritingTestMergedFilePlaying = false
                )

                ttsPlaybackController.stopTts()
                ttsPlaybackController.clearHighlight()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기레벨 변경 처리 실패", e)
            }
        }
    }

    fun setQuestionCardFlipped(isFlipped: Boolean) {
        _uiState.value = _uiState.value.copy(isQuestionCardFlipped = isFlipped)
    }

    init {
        viewModelScope.launch {
            fullMemorizationUseCase.highlightIndex.collect { index ->
                _uiState.value = _uiState.value.copy(fullMemorizationHighlightIndex = index)
            }
        }

        viewModelScope.launch {
            var isFirst = true
            qaDataManager.currentQaItem.collect { currentItem ->
                if (currentItem != null) {
                    if (isFirst) {
                        isFirst = false
                        return@collect
                    }
                    updateFullMemorizationRecordingStatus()
                }
            }
        }

        viewModelScope.launch {
            fullMemorizationUseCase.state.collect { fsState ->
                if (_uiState.value.currentMode !in setOf(
                        CurrentMode.FULL_MEMORIZATION,
                        CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
                        CurrentMode.FULL_MEMORIZATION_RECORDING,
                        CurrentMode.FULL_MEMORIZATION_PLAYING,
                        CurrentMode.FULL_MEMORIZATION_WITH_FILE
                    )) return@collect

                when (fsState) {
                    is FullMemorizationState.Idle ->
                        startMode(CurrentMode.FULL_MEMORIZATION)
                    is FullMemorizationState.QuestionPlaying ->
                        startMode(CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING)
                    is FullMemorizationState.Recording ->
                        startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                    is FullMemorizationState.Playing ->
                        startMode(CurrentMode.FULL_MEMORIZATION_PLAYING)
                    is FullMemorizationState.WithFile -> {
                        _uiState.value = _uiState.value.copy(hasFullMemorizationRecordingFile = fsState.hasRecording)
                        startMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                    }
                }
            }
        }
    }
}
