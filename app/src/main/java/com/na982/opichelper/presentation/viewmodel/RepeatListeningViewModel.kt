package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.usecase.ExecuteRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.entity.RepeatListeningData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class RepeatListeningUiState(
    val isCardFlipped: Boolean = false
)

@HiltViewModel
class RepeatListeningViewModel @Inject constructor(
    private val executeRepeatListeningUseCase: ExecuteRepeatListeningUseCase,
    private val ttsPlaybackController: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val progressTracker: MemorizeTestProgressTracker,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val coordinator: MemorizationModeCoordinator
) : ViewModel() {

    val modeCoordinator: MemorizationModeCoordinator get() = coordinator

    private val _uiState = MutableStateFlow(RepeatListeningUiState())
    val uiState: StateFlow<RepeatListeningUiState> = _uiState.asStateFlow()

    private var currentUseCaseJob: Job? = null
    private var eventCollectJob: Job? = null

    fun start() {
        viewModelScope.launch {
            try {
                if (coordinator.requestMode(CurrentMode.REPEAT_LISTENING)) {
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                    startRepeatListening()
                }
            } catch (e: Exception) {
                Log.e("RepeatListeningVM", "반복 듣기 시작 실패", e)
                stop()
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
                            handleEvent(event)
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
                    coordinator.registerJob(currentUseCaseJob!!)
                    coordinator.registerEventJob(eventCollectJob!!)
                }
            } catch (e: Exception) {
                Log.e("RepeatListeningVM", "반복 듣기 시작 실패", e)
                stop()
            }
        }
    }

    private fun handleEvent(event: MemorizeTestEvent) {
        if (currentUseCaseJob?.isActive != true) return
        when (event) {
            is MemorizeTestEvent.CardFlip -> {
                _uiState.value = _uiState.value.copy(isCardFlipped = event.isKorean)
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
                    stop()
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
                _uiState.value = _uiState.value.copy(isCardFlipped = false)
                coordinator.releaseMode()
                if (!coordinator.requestMode(CurrentMode.REPEAT_LISTENING)) {
                    return@launch
                }
                ttsPlaybackController.stopTts()
                ttsPlaybackController.clearHighlight()
                startRepeatListening()
            } else {
                stop()
            }
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
                Log.e("RepeatListeningVM", "진행상황 저장 실패", e)
            }
        }
    }

    fun onLevelChanged() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
        coordinator.releaseMode()
        _uiState.value = RepeatListeningUiState()
        ttsPlaybackController.stopTts()
        ttsPlaybackController.clearHighlight()
    }
}
