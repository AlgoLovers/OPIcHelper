package com.na982.opichelper.presentation.viewmodel

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.usecase.PlayMergedFileUseCase
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class PlaybackState(
    val hasEnglishWritingTestMergedFile: Boolean = false,
    val isEnglishWritingTestMergedFilePlaying: Boolean = false,
    val englishWritingTestMergedFileHighlightIndex: Int? = null,

    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,

    val isAnswerCardFlipped: Boolean = false,

    val hasProgress: Boolean = false
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController,
    private val playMergedFileUseCase: PlayMergedFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaybackState())
    val uiState: StateFlow<PlaybackState> = _uiState.asStateFlow()

    init {
        setupStateCombination()
    }

    private fun setupStateCombination() {
        viewModelScope.launch {
            combine(
                ttsPlaybackController.isPlaying,
                ttsPlaybackController.isQuestionPlaying,
                ttsPlaybackController.isAnswerPlaying,
                ttsPlaybackController.questionHighlightIndex,
                ttsPlaybackController.answerHighlightIndex,
                ttsPlaybackController.answerKoHighlightIndex,
                ttsPlaybackController.recordingHighlightIndex
            ) { values ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = values[0] as Boolean,
                    isQuestionPlaying = values[1] as Boolean,
                    isAnswerPlaying = values[2] as Boolean,
                    questionHighlightIndex = values[3] as Int?,
                    answerHighlightIndex = values[4] as Int?,
                    answerKoHighlightIndex = values[5] as Int?,
                    recordingHighlightIndex = values[6] as Int?
                )
            }.collect { }
        }

        viewModelScope.launch {
            combine(
                playMergedFileUseCase.hasFile,
                playMergedFileUseCase.isPlaying,
                playMergedFileUseCase.highlightIndex
            ) { hasFile, isPlaying, highlightIndex ->
                _uiState.value = _uiState.value.copy(
                    hasEnglishWritingTestMergedFile = hasFile,
                    isEnglishWritingTestMergedFilePlaying = isPlaying,
                    englishWritingTestMergedFileHighlightIndex = highlightIndex
                )
            }.collect { }
        }
    }

    fun setAnswerCardFlipped(isFlipped: Boolean) {
        _uiState.value = _uiState.value.copy(isAnswerCardFlipped = isFlipped)
    }

    fun setMergedAudioPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isEnglishWritingTestMergedFilePlaying = isPlaying)
    }

    fun playEnglishWritingTestMergedFile() {
        playMergedFileUseCase.play()
    }

    fun stopEnglishWritingTestMergedFile() {
        playMergedFileUseCase.stop()
    }

    fun checkEnglishWritingTestMergedFile() {
        playMergedFileUseCase.checkFile()
    }

    fun playQuestion(question: String) {
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            ttsPlaybackController.playQuestion(question)
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            stopEnglishWritingTestMergedFile()
            ttsPlaybackController.stopTts()
            ttsPlaybackController.playAnswer(answer)
        }
    }

    fun stopTts() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
            playMergedFileUseCase.stop()
            ttsPlaybackController.clearHighlight()
        }
    }

    fun cleanupAllTtsSync() {
        try {
            ttsPlaybackController.forceStopTts()
            ttsPlaybackController.clearHighlight()
            playMergedFileUseCase.stop()
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsPlaybackController.cleanupTts()
        playMergedFileUseCase.stop()
        playMergedFileUseCase.release()
    }

    fun onBackgroundMove() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isPlaying) {
                    ttsPlaybackController.pauseTts()
                }
                ttsPlaybackController.clearHighlight()
            } catch (e: Exception) {
                Log.e("PlaybackViewModel", "백그라운드 이동 처리 실패", e)
            }
        }
    }

    fun onForegroundReturn() {
        viewModelScope.launch {
            try {
                if (_uiState.value.isPlaying) {
                    ttsPlaybackController.resumeTts()
                }
            } catch (e: Exception) {
                Log.e("PlaybackViewModel", "포그라운드 복귀 처리 실패", e)
            }
        }
    }
}
