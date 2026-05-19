package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.FullMemorizationState
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class FullMemorizationUiState(
    val hasRecordingFile: Boolean = false,
    val highlightIndex: Int? = null
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
            checkRecordingStatus()

            val category = qaDataManager.getCurrentCategory() ?: ""
            val scriptIndex = qaDataManager.getCurrentIndex()

            // 하이라이트 인덱스 수집
            viewModelScope.launch {
                fullMemorizationUseCase.highlightIndex.collect { index ->
                    _uiState.value = _uiState.value.copy(highlightIndex = index)
                }
            }

            // UseCase 상태 → 코디네이터 모드 동기화
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
                            _uiState.value = _uiState.value.copy(hasRecordingFile = fsState.hasRecording)
                            coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
                        }
                    }
                }
            }

            // 코디네이터 이벤트 수집
            viewModelScope.launch {
                coordinator.events.collect { event ->
                    if (event is CoordinatorEvent.RecordingStateChanged) {
                        updateRecordingStatus()
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
                updateRecordingStatus()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "통암기 녹음 종료 실패", e)
                emitEvent("녹음을 종료할 수 없습니다")
            }
        }
    }

    fun playRecording() {
        viewModelScope.launch {
            try {
                if (fullMemorizationUseCase.hasRecording()) {
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
                updateRecordingStatus()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "통암기 재생 중지 실패", e)
                emitEvent("재생 중지에 실패했습니다")
                coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
            }
        }
    }

    suspend fun hasRecording(): Boolean {
        return _uiState.value.hasRecordingFile
    }

    fun updateRecordingStatus() {
        viewModelScope.launch {
            try {
                val hasRecording = fullMemorizationUseCase.hasRecording()
                _uiState.value = _uiState.value.copy(hasRecordingFile = hasRecording)
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "녹음 파일 상태 확인 실패", e)
            }
        }
    }

    private suspend fun checkRecordingStatus() {
        try {
            val hasRecording = fullMemorizationUseCase.hasRecording()
            _uiState.value = _uiState.value.copy(hasRecordingFile = hasRecording)
        } catch (e: Exception) {
            Log.e("FullMemorizationVM", "녹음 파일 상태 확인 실패", e)
        }
    }

    fun deleteRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.clearRecording()
                updateRecordingStatus()
            } catch (e: Exception) {
                Log.e("FullMemorizationVM", "녹음 파일 삭제 실패", e)
            }
        }
    }

    override fun onStop() {
        viewModelScope.launch {
            fullMemorizationUseCase.cancelPlayback()
        }
    }

    override fun onCleared() {
        super.onCleared()
        fullMemorizationUseCase.close()
    }
}
