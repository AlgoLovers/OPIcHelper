package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class EnglishWritingTestUiState(
    val isCardFlipped: Boolean = false,
    val completed: Boolean = false,
    val stopMergedFilePlaying: Boolean = false
)

@HiltViewModel
class EnglishWritingTestViewModel @Inject constructor(
    private val executeEnglishWritingTestUseCase: ExecuteEnglishWritingTestUseCase,
    private val ttsPlaybackController: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val progressTracker: MemorizeTestProgressTracker,
    private val coordinator: MemorizationModeCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnglishWritingTestUiState())
    val uiState: StateFlow<EnglishWritingTestUiState> = _uiState.asStateFlow()

    private var currentUseCaseJob: Job? = null
    private var eventCollectJob: Job? = null

    var onRecordingStateChanged: (() -> Unit)? = null

    fun start() {
        viewModelScope.launch {
            try {
                if (coordinator.requestMode(CurrentMode.ENGLISH_WRITING)) {
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                    _uiState.value = _uiState.value.copy(stopMergedFilePlaying = true)
                    startEnglishWritingTest()
                }
            } catch (e: Exception) {
                Log.e("EnglishWritingTestVM", "영작 테스트 시작 실패", e)
                stop()
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
                    handleEvent(event)
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
                    Log.e("EnglishWritingTestVM", "영작 테스트 실행 중 오류", e)
                    stop()
                }
            }
            coordinator.registerJob(currentUseCaseJob!!)
            coordinator.registerEventJob(eventCollectJob!!)
        }
    }

    private fun handleEvent(event: MemorizeTestEvent) {
        if (currentUseCaseJob?.isActive != true) return
        when (event) {
            is MemorizeTestEvent.CardFlip -> {
                _uiState.value = _uiState.value.copy(isCardFlipped = event.isKorean)
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
                    onRecordingStateChanged?.invoke()
                }
            }
            is MemorizeTestEvent.MergedFileCreated -> {
                _uiState.value = _uiState.value.copy(completed = true)
                stop()
            }
            else -> {}
        }
    }

    fun stop() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        coordinator.releaseMode()

        _uiState.value = _uiState.value.copy(isCardFlipped = false)

        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }

        viewModelScope.launch {
            try {
                progressTracker.persistChangedProgress()
            } catch (e: Exception) {
                Log.e("EnglishWritingTestVM", "진행상황 저장 실패", e)
            }
        }
    }

    fun resetCompleted() {
        _uiState.value = _uiState.value.copy(completed = false)
    }

    fun resetStopMergedFilePlaying() {
        _uiState.value = _uiState.value.copy(stopMergedFilePlaying = false)
    }

    fun onLevelChanged() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        coordinator.releaseMode()
        _uiState.value = EnglishWritingTestUiState()
        ttsPlaybackController.stopTts()
        ttsPlaybackController.clearHighlight()
    }

    override fun onCleared() {
        super.onCleared()
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        onRecordingStateChanged = null
    }
}
