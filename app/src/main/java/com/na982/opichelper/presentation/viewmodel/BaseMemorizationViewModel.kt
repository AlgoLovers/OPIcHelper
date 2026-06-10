package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.repository.QaContentReader
import com.na982.opichelper.domain.entity.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.na982.opichelper.domain.manager.AppLogger

abstract class BaseMemorizationViewModel<T>(
    protected val coordinator: MemorizationModeCoordinator,
    private val ttsPlaybackController: TtsPlaybackController,
    private val progressTracker: MemorizeTestProgressTracker,
    protected val appLogger: AppLogger,
    protected val qaContentReader: QaContentReader
) : ViewModel() {

    @Volatile
    protected var modeJob: Job? = null

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val events: SharedFlow<String> = _events.asSharedFlow()

    protected suspend fun emitEvent(message: String) {
        _events.emit(message)
    }

    protected abstract val _uiState: MutableStateFlow<T>
    val uiState: StateFlow<T> get() = _uiState.asStateFlow()

    protected abstract fun resetUiState(): T
    protected abstract fun initialMode(): CurrentMode
    protected abstract suspend fun startMode()

    protected open fun onStop() {}

    open fun requestExtraRepetitions() {}

    fun start() {
        if (!coordinator.requestMode(initialMode())) return
        modeJob = viewModelScope.launch { startMode() }
    }

    fun stop() {
        modeJob?.cancel()
        modeJob = null
        onStop()
        coordinator.releaseMode()
        cleanupAndPersist()
    }

    fun onLevelChanged() {
        modeJob?.cancel()
        modeJob = null
        coordinator.releaseMode()
        _uiState.update { resetUiState() }
        cleanupAndPersist()
    }

    private fun cleanupAndPersist() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
            try {
                progressTracker.persistChangedProgress()
            } catch (e: Exception) {
                appLogger.e("BaseMemorizationVM", "진행상황 저장 실패", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        modeJob?.cancel()
        modeJob = null
    }

    protected fun getSentenceFromAnswer(index: Int, isKorean: Boolean): String? {
        val currentItem = qaContentReader.getCurrentQaItem() ?: return null
        val text = if (isKorean) {
            qaContentReader.getCurrentAnswerKo(currentItem)
        } else {
            qaContentReader.getCurrentAnswer(currentItem)
        }
        return SentenceSplitter.split(text).getOrNull(index)
    }

    protected fun handleKoreanHighlight(event: MemorizeTestEvent.KoreanHighlight) {
        if (event.index != null) {
            val koSentence = getSentenceFromAnswer(event.index, isKorean = true)
            val enSentence = getSentenceFromAnswer(event.index, isKorean = false)
            ttsPlaybackController.setAnswerKoHighlightIndex(event.index, koSentence)
            ttsPlaybackController.setAnswerHighlightIndex(event.index, enSentence)
        } else {
            ttsPlaybackController.clearHighlight()
        }
    }
}
