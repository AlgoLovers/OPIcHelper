package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.repository.QaContentReader
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.entity.CurrentMode
import com.na982.opichelper.domain.usecase.FullMemorizationState
import com.na982.opichelper.domain.usecase.FullMemorizationUseCase
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.audio.TtsPlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import com.na982.opichelper.domain.manager.AppLogger
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
    qaContentReader: QaContentReader,
    coordinator: MemorizationModeCoordinator,
    ttsPlaybackController: TtsPlaybackController,
    progressTracker: MemorizeTestProgressTracker,
    appLogger: AppLogger
) : BaseMemorizationViewModel<FullMemorizationUiState>(
    coordinator = coordinator,
    ttsPlaybackController = ttsPlaybackController,
    progressTracker = progressTracker,
    appLogger = appLogger,
    qaContentReader = qaContentReader
) {

    override val _uiState = MutableStateFlow(FullMemorizationUiState())
    override fun resetUiState() = FullMemorizationUiState()
    override fun initialMode() = CurrentMode.FULL_MEMORIZATION

    override suspend fun startMode() {
        try {
            refreshRecordingStatus()

            val category = qaContentReader.getCurrentCategory() ?: ""
            val scriptIndex = qaContentReader.getCurrentIndex()

            val modeContext = coroutineContext
            // 부모는 현재 실행 중인 mode 코루틴의 Job이어야 한다. viewModelScope가
            // Main.immediate라 startMode()는 modeJob 대입 전에 동기 실행되므로,
            // 변수 modeJob을 읽으면 직전 stop()이 남긴 stale null을 잡아 collectJob이
            // 부모 없는 루트 Job이 된다 → 아래 컬렉터들이 modeJob.cancel()로 취소되지
            // 않고 누수·중복 갱신된다. coroutineContext[Job]으로 실제 부모를 잡는다.
            val collectJob = Job(modeContext[Job])

            viewModelScope.launch(modeContext + collectJob) {
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

            viewModelScope.launch(modeContext + collectJob) {
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

            viewModelScope.launch(modeContext + collectJob) {
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            appLogger.e("FullMemorizationVM", "통암기 모드 시작 실패", e)
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
                appLogger.e("FullMemorizationVM", "통암기 녹음 종료 실패", e)
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
                appLogger.e("FullMemorizationVM", "통암기 녹음 재생 실패", e)
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
                appLogger.e("FullMemorizationVM", "통암기 재생 중지 실패", e)
                emitEvent("재생 중지에 실패했습니다")
                coordinator.updateMode(CurrentMode.FULL_MEMORIZATION_WITH_FILE)
            }
        }
    }

    private fun refreshRecordingStatus() {
        viewModelScope.launch {
            try {
                val hasRecording = fullMemorizationUseCase.hasRecording()
                _uiState.update { it.copy(hasRecordingFile = hasRecording) }
            } catch (e: Exception) {
                appLogger.e("FullMemorizationVM", "녹음 파일 상태 확인 실패", e)
            }
        }
    }

    override fun onStop() {
        viewModelScope.launch {
            try {
                fullMemorizationUseCase.cancelPlayback()
            } catch (e: Exception) {
                appLogger.e("FullMemorizationVM", "cancelPlayback 실패", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fullMemorizationUseCase.reset()
    }
}
