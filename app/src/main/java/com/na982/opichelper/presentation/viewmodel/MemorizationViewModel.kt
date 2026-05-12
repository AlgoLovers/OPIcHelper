package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
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
    private val progressTracker: MemorizeTestProgressTracker
) : ViewModel(), RepeatListeningUiCallback {

    override fun onCleared() {
        super.onCleared()
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        fullMemorizationUseCase.cancelPlayback()
    }

    private val _memorizeLevels = MutableStateFlow(MemorizeLevel.allDisplayNames)
    val memorizeLevels: StateFlow<List<String>> = _memorizeLevels.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentMode = MutableStateFlow(CurrentMode.NONE)
    val currentMode: StateFlow<CurrentMode> = _currentMode.asStateFlow()

    private val _englishWritingTestCompleted = MutableStateFlow(false)
    val englishWritingTestCompleted: StateFlow<Boolean> = _englishWritingTestCompleted.asStateFlow()

    private val _stopEnglishWritingTestMergedFilePlaying = MutableStateFlow(false)
    val stopEnglishWritingTestMergedFilePlaying: StateFlow<Boolean> = _stopEnglishWritingTestMergedFilePlaying.asStateFlow()

    private val _uiState = MutableStateFlow(MemorizationUiState())
    val uiState: StateFlow<MemorizationUiState> = _uiState.asStateFlow()

    val fullMemorizationHighlightIndex: StateFlow<Int?> = fullMemorizationUseCase.highlightIndex

    private var currentUseCaseJob: Job? = null

    private fun updateUiState() {
        val mode = _currentMode.value
        val running = _isRunning.value
        _uiState.value = MemorizationUiState(
            isRunning = running,
            currentMode = mode,

            isRepeatListeningCardFlipped = mode == CurrentMode.REPEAT_LISTENING && running,
            isRepeatListeningRunning = mode == CurrentMode.REPEAT_LISTENING && running,
            isRepeatListeningMode = mode == CurrentMode.REPEAT_LISTENING,
            repeatListeningCurrentRepeat = 0,
            repeatListeningTotalRepeats = 5,

            isEnglishWritingTestCardFlipped = mode == CurrentMode.ENGLISH_WRITING && running,
            isEnglishWritingTestRunning = mode == CurrentMode.ENGLISH_WRITING && running,
            isEnglishWritingTestMode = mode in setOf(
                CurrentMode.ENGLISH_WRITING,
                CurrentMode.ENGLISH_WRITING_RECORDING,
                CurrentMode.ENGLISH_WRITING_PLAYING,
                CurrentMode.ENGLISH_WRITING_WITH_FILE
            ),
            isEnglishWritingTestRecording = mode == CurrentMode.ENGLISH_WRITING_RECORDING,
            isEnglishWritingTestPlaying = mode == CurrentMode.ENGLISH_WRITING_PLAYING,
            hasEnglishWritingTestRecording = mode == CurrentMode.ENGLISH_WRITING_WITH_FILE,

            isFullMemorizationMode = mode in setOf(
                CurrentMode.FULL_MEMORIZATION,
                CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
                CurrentMode.FULL_MEMORIZATION_RECORDING,
                CurrentMode.FULL_MEMORIZATION_PLAYING,
                CurrentMode.FULL_MEMORIZATION_WITH_FILE
            ),
            isFullMemorizationQuestionPlaying = mode == CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
            isFullMemorizationRecording = mode == CurrentMode.FULL_MEMORIZATION_RECORDING,
            isFullMemorizationPlaying = mode == CurrentMode.FULL_MEMORIZATION_PLAYING,
            hasFullMemorizationRecording = mode in setOf(
                CurrentMode.FULL_MEMORIZATION_WITH_FILE,
                CurrentMode.FULL_MEMORIZATION_PLAYING
            ),
            isFullMemorizationRecordingPlaying = mode == CurrentMode.FULL_MEMORIZATION_PLAYING,

            isMemorizeTestRunning = running,

            englishWritingTestCompleted = _englishWritingTestCompleted.value,
            stopEnglishWritingTestMergedFilePlaying = _stopEnglishWritingTestMergedFilePlaying.value
        )
    }

    private fun startMode(mode: CurrentMode) {
        _currentMode.value = mode
        _isRunning.value = true
        updateUiState()
    }

    private fun stopMode() {
        _isRunning.value = false
        _currentMode.value = CurrentMode.NONE
        updateUiState()
    }

    private fun isModeRunning(mode: CurrentMode): Boolean {
        return _isRunning.value && _currentMode.value == mode
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
                            _stopEnglishWritingTestMergedFilePlaying.value = true
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

    fun startFullMemorizationMode() {
        viewModelScope.launch {
            try {
                currentUseCaseJob?.cancel()
                checkFullMemorizationRecordingStatus()

                val category = qaDataManager.getCurrentCategory() ?: ""
                val scriptIndex = qaDataManager.getCurrentIndex()

                fullMemorizationUseCase.startFullMemorization(
                    category = category,
                    scriptIndex = scriptIndex,
                    onRecordingStateChange = { isRecording ->
                        if (isRecording) {
                            startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                        } else {
                            startMode(CurrentMode.FULL_MEMORIZATION)
                        }
                    },
                    onPlayingStateChange = { isPlaying ->
                        if (isPlaying) {
                            startMode(CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING)
                        } else {
                            startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                        }
                    }
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
                    fullMemorizationUseCase.playRecordingWithHighlight { isPlaying ->
                        if (!isPlaying) {
                            startMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                        }
                    }
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
                _currentMode.value = if (hasRecording) CurrentMode.FULL_MEMORIZATION_WITH_FILE else CurrentMode.FULL_MEMORIZATION
                updateUiState()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 파일 상태 확인 실패", e)
            }
        }
    }

    private suspend fun checkFullMemorizationRecordingStatus() {
        try {
            val hasRecording = fullMemorizationUseCase.hasRecording()
            _currentMode.value = if (hasRecording) CurrentMode.FULL_MEMORIZATION_WITH_FILE else CurrentMode.FULL_MEMORIZATION
            updateUiState()
        } catch (e: Exception) {
            Log.e("MemorizationViewModel", "통암기 녹음 파일 상태 확인 실패", e)
        }
    }

    fun resetStateOnAppRestart() {
        _currentMode.value = CurrentMode.NONE
        _isRunning.value = false
        updateUiState()
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

    private fun startRepeatListening() {
        viewModelScope.launch {
            try {
                currentUseCaseJob?.cancel()
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    currentUseCaseJob = launch {
                        val repeatListeningData = RepeatListeningData(
                            category = currentItem.category,
                            scriptIndex = qaDataManager.getCurrentIndex(),
                            koreanAnswer = qaDataManager.getCurrentAnswerKo(currentItem),
                            englishAnswer = qaDataManager.getCurrentAnswer(currentItem)
                        )
                        executeRepeatListeningUseCase.execute(
                            data = repeatListeningData,
                            uiCallback = this@MemorizationViewModel
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복 듣기 시작 실패", e)
                stopMode()
            }
        }
    }

    private fun startEnglishWritingTest() {
        val currentItem = qaDataManager.currentQaItem.value
        if (currentItem != null) {
            val scriptIndex = qaDataManager.getCurrentIndex()

            currentUseCaseJob = viewModelScope.launch {
                try {
                    executeEnglishWritingTestUseCase.execute(
                        answerKo = qaDataManager.getCurrentAnswerKo(currentItem),
                        answerEn = qaDataManager.getCurrentAnswer(currentItem),
                        category = currentItem.category,
                        scriptIndex = scriptIndex,
                        onCardFlip = { isKorean ->
                            _uiState.value = _uiState.value.copy(
                                isEnglishWritingTestCardFlipped = isKorean
                            )
                        },
                        onKoreanHighlight = { index ->
                            if (index != null) {
                                ttsPlaybackController.setAnswerKoHighlightIndex(index)
                            } else {
                                ttsPlaybackController.clearHighlight()
                            }
                        },
                        onRecordingHighlight = { index ->
                            if (index != null) {
                                ttsPlaybackController.setRecordingHighlightIndex(index)
                            } else {
                                ttsPlaybackController.clearHighlight()
                            }
                        },
                        onRecordingStateChange = { isRecording ->
                            if (!isRecording) {
                                viewModelScope.launch {
                                    updateFullMemorizationRecordingStatus()
                                }
                            }
                        },
                        onMergedFileCreated = {
                            _englishWritingTestCompleted.value = true
                            stopMode()
                            _uiState.value = _uiState.value.copy(
                                isEnglishWritingTestRunning = false,
                                isEnglishWritingTestMode = false
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MemorizationViewModel", "영작 테스트 실행 중 오류", e)
                    stopMode()
                }
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
        stopMode()

        _uiState.value = _uiState.value.copy(isRepeatListeningCardFlipped = false)

        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }

        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex(), MemorizeLevel.REPEAT_LISTENING.displayName)
                    if (currentProgress != null) {
                        progressTracker.persistChangedProgress()
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복듣기 진행상황 저장 실패", e)
            }
        }
    }

    fun resetEnglishWritingTestCompleted() {
        _englishWritingTestCompleted.value = false
    }

    fun resetStopEnglishWritingTestMergedFilePlaying() {
        _stopEnglishWritingTestMergedFilePlaying.value = false
    }

    fun stopEnglishWritingTest() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        stopMode()

        _uiState.value = _uiState.value.copy(isEnglishWritingTestCardFlipped = false)

        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }

        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex(), MemorizeLevel.ENGLISH_WRITING.displayName)
                    if (currentProgress != null) {
                        progressTracker.persistChangedProgress()
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "영작테스트 진행상황 저장 실패", e)
            }
        }
    }

    fun onMemorizeLevelChanged() {
        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val currentLevel = when {
                        _uiState.value.isRepeatListeningMode -> MemorizeLevel.REPEAT_LISTENING.displayName
                        _uiState.value.isEnglishWritingTestMode -> MemorizeLevel.ENGLISH_WRITING.displayName
                        _uiState.value.isFullMemorizationMode -> MemorizeLevel.FULL_MEMORIZATION.displayName
                        else -> MemorizeLevel.REPEAT_LISTENING.displayName
                    }

                    val currentProgress = progressTracker.getScriptProgress(
                        currentItem.category,
                        qaDataManager.getCurrentIndex(),
                        currentLevel
                    )

                    if (currentProgress != null) {
                        progressTracker.persistChangedProgress()
                    }
                }

                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                stopMode()

                _englishWritingTestCompleted.value = false
                _stopEnglishWritingTestMergedFilePlaying.value = false

                ttsPlaybackController.stopTts()
                ttsPlaybackController.clearHighlight()
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기레벨 변경 처리 실패", e)
            }
        }
    }

    init {
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
    }

    override fun onCardFlip(isKorean: Boolean) {
        if (currentUseCaseJob?.isActive != true) return
        _uiState.value = _uiState.value.copy(isRepeatListeningCardFlipped = isKorean)
    }

    override fun onHighlight(index: Int?) {
        if (currentUseCaseJob?.isActive != true) return
        if (index != null) {
            ttsPlaybackController.setAnswerHighlightIndex(index)
        } else {
            ttsPlaybackController.clearHighlight()
        }
    }

    override fun onKoreanHighlight(index: Int?) {
        if (currentUseCaseJob?.isActive != true) return
        if (index != null) {
            ttsPlaybackController.setAnswerKoHighlightIndex(index)
        } else {
            ttsPlaybackController.clearHighlight()
        }
    }

    override fun onComplete() {
        if (currentUseCaseJob?.isActive != true) return
        stopMode()
    }
}
