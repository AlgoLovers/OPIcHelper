package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

abstract class BaseMemorizationViewModel<T>(
    protected val coordinator: MemorizationModeCoordinator,
    private val ttsPlaybackController: TtsPlaybackController?,
    private val progressTracker: MemorizeTestProgressTracker?
) : ViewModel() {

    protected var currentUseCaseJob: Job? = null
    protected var eventCollectJob: Job? = null

    protected abstract val _uiState: MutableStateFlow<T>
    val uiState: StateFlow<T> get() = _uiState.asStateFlow()

    protected abstract fun resetUiState(): T

    protected open fun onStop() {}
    protected open fun onLevelChangedExtra() {}

    fun stop() {
        cancelJobs()
        coordinator.releaseMode()
        onStop()

        viewModelScope.launch {
            ttsPlaybackController?.stopTts()
            ttsPlaybackController?.clearHighlight()
        }

        viewModelScope.launch {
            try {
                progressTracker?.persistChangedProgress()
            } catch (e: Exception) {
                Log.e("BaseMemorizationVM", "진행상황 저장 실패", e)
            }
        }
    }

    fun onLevelChanged() {
        cancelJobs()
        coordinator.releaseMode()
        _uiState.value = resetUiState()
        onLevelChangedExtra()
        ttsPlaybackController?.stopTts()
        ttsPlaybackController?.clearHighlight()
    }

    protected fun cancelJobs() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        eventCollectJob?.cancel()
        eventCollectJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelJobs()
    }
}
