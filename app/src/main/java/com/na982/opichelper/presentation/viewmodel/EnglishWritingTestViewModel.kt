package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.ExecuteEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class EnglishWritingTestUiState(
    val isCardFlipped: Boolean = false
)

@HiltViewModel
class EnglishWritingTestViewModel @Inject constructor(
    private val executeEnglishWritingTestUseCase: ExecuteEnglishWritingTestUseCase,
    private val ttsCtrl: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val progress: MemorizeTestProgressTracker,
    coordinator: MemorizationModeCoordinator
) : BaseMemorizationViewModel<EnglishWritingTestUiState>(
    coordinator = coordinator,
    ttsPlaybackController = ttsCtrl,
    progressTracker = progress
) {

    override val _uiState = MutableStateFlow(EnglishWritingTestUiState())
    override fun resetUiState() = EnglishWritingTestUiState()

    override fun onStop() {
        _uiState.value = _uiState.value.copy(isCardFlipped = false)
        viewModelScope.launch { coordinator.emitEvent(CoordinatorEvent.EnglishWritingStopped) }
    }

    fun start() {
        viewModelScope.launch {
            try {
                if (coordinator.requestMode(CurrentMode.ENGLISH_WRITING)) {
                    ttsCtrl.stopTts()
                    ttsCtrl.clearHighlight()
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
                    ttsCtrl.setAnswerKoHighlightIndex(event.index)
                } else {
                    ttsCtrl.clearHighlight()
                }
            }
            is MemorizeTestEvent.RecordingHighlight -> {
                if (event.index != null) {
                    ttsCtrl.setRecordingHighlightIndex(event.index)
                } else {
                    ttsCtrl.clearHighlight()
                }
            }
            is MemorizeTestEvent.RecordingStateChange -> {
                if (!event.isRecording) {
                    viewModelScope.launch { coordinator.emitEvent(CoordinatorEvent.RecordingStateChanged) }
                }
            }
            is MemorizeTestEvent.MergedFileCreated -> {
                viewModelScope.launch { coordinator.emitEvent(CoordinatorEvent.EnglishWritingCompleted) }
                stop()
            }
            else -> {}
        }
    }
}
