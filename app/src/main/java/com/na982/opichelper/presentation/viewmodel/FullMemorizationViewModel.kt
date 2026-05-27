package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.CurrentMode
import com.na982.opichelper.domain.usecase.FullMemorizationState
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class FullMemorizationUiState(
    val hasRecordingFile: Boolean = false,
    val highlightIndex: Int? = null,
    val currentSentenceEn: String? = null,
    val currentSentenceKo: String? = null
)

@HiltViewModel
class FullMemorizationViewModel @Inject constructor(
    private val fullMemorizationUseCase: FullMemorizationUseCase,
    private val qaDataManager: QaDataManager,
    coordinator: MemorizationModeCoordinator
) : BaseMemorizationViewModel<FullMemorizationUiState>(
    coordinator = coordinator,
    ttsPlaybackController = null,
    progressTracker = null
) {

    override val _uiState = MutableStateFlow(FullMemorizationUiState())
    override fun resetUiState() = FullMemorizationUiState()
    override fun initialMode() = CurrentMode.FULL_MEMORIZATION

    override suspend fun startMode() {
        try {
            refreshRecordingStatus()

            val category = qaDataManager.getCurrentCategory() ?: ""
            val scriptIndex = qaDataManager.getCurrentIndex()

            viewModelScope.launch {
                fullMemorizationUseCase.highlightIndex.collect { index ->
                    val sentenceEn = index?.let { getSentenceFromAnswer(it, isKorean = false) }
                    val sentenceKo = index?.let { getSentenceFromAnswer(it, isKorean = true) }
                    _uiState.update { it.copy(
                        highlightIndex = index,
                        currentSentenceEn = sentenceEn,
                        currentSentenceKo = sentenceKo
                    ) }
                }
            }

            viewModelScope.launch {
                fullMemorizationUseCase.state.collect { fsState ->
                    when (fsState) {
                        is FullMemorizationState.Idle -> {
                            coordinator.updateMode(CurrentMode.FULL_MEMORIZATION)
                        }
                        is FullMemorizationState.QuestionPlaying -> {
                            coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING)
                        }
                        is FullMemorizationState.Recording -> {
                            coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_RECORDING)
                        }
                        is FullMemorizationState.Playing -> {
                            coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_PLAYING)
                        }
                        is FullMemorizationState.WithFile -> {
                            _uiState.update { it.copy(hasRecordingFile = fsState.hasRecording) }
                            coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                        }
                    }
                }
            }

            viewModelScope.launch {
                coordinator.events.collect { event ->
                    if (event is CoordinatorEvent.RecordingStateChanged) {
                        refreshRecordingStatus()
                    }
                }
            }

            fullMemorizationUseCase.startFullMemorization(
                category = category,
                scriptIndex = scriptIndex
            )
        } catch (e: Exception) {
            Log.e("FullMemorizationVM", "통암기 모드 시작 실패", e)
            emitEvent("통암기를 시작할 수 없습니다")
            coordinator.releaseMode()
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.stopRecording()
                refreshRecordingStatus()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "통암기 녹음 종료 실패", e)
                emitEvent("녹음을 종료할 수 없습니다")
            }
        }
    }

    fun playRecording() {
        viewModelScope.launch {
            try {
                if (_uiState.value.hasRecordingFile) {
                    coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_PLAYING)
                    fullMemorizationUseCase.playRecordingWithHighlight()
                }
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "통암기 녹음 재생 실패", e)
                emitEvent("녹음 재생에 실패했습니다")
                coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
            }
        }
    }

    fun stopPlaying() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.cancelPlayback()
                coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                refreshRecordingStatus()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "통암기 재생 중지 실패", e)
                emitEvent("재생 중지에 실패했습니다")
                coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
            }
        }
    }

    fun refreshRecordingStatus() {
        viewModelScope.launch {
            try {
                val hasRecording = fullMemorizationUseCase.hasRecording()
                _uiState.update { it.copy(hasRecordingFile = hasRecording) }
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "녹음 파일 상태 확인 실패", e)
            }
        }
    }

    fun deleteRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.clearRecording()
                refreshRecordingStatus()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "녹음 파일 삭제 실패", e)
            }
        }
    }

    override fun onStop() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.cancelPlayback()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "cancelPlayback 실패", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fullMemorizationUseCase.close()
    }

    private fun getSentenceFromAnswer(index: Int, isKorean: Boolean): String? {
        val currentItem = qaDataManager.currentQaItem.value ?: return null
        val text = if (isKorean) {
            qaDataManager.getCurrentAnswerKo(currentItem)
        } else {
            qaDataManager.getCurrentAnswer(currentItem)
        }
        return SentenceSplitter.split(text).getOrNull(index)
    }
}
