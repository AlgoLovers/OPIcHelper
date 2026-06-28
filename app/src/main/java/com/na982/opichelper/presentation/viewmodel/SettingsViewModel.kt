package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.UserLevelPreferences
import com.na982.opichelper.domain.repository.TtsPreferences
import com.na982.opichelper.domain.repository.PlaybackPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class SettingsUiState(
    val currentUserLevel: String = "",
    val currentKoreanTtsService: String = "",
    val repeatListeningCount: Int = 5,
    val answerPlayCount: Int = 1,
    val englishTtsRate: Float = 0.8f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userLevelPreferences: UserLevelPreferences,
    private val ttsPreferences: TtsPreferences,
    private val playbackPreferences: PlaybackPreferences,
    private val ttsOrchestrator: TtsOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userLevelPreferences.userLevel.collect { userLevel ->
                _uiState.update { it.copy(currentUserLevel = userLevel.name) }
            }
        }

        viewModelScope.launch {
            playbackPreferences.repeatListeningCount.collect { count ->
                _uiState.update { it.copy(repeatListeningCount = count) }
            }
        }

        viewModelScope.launch {
            playbackPreferences.answerPlayCount.collect { count ->
                _uiState.update { it.copy(answerPlayCount = count) }
            }
        }

        viewModelScope.launch {
            ttsPreferences.englishTtsRate.collect { rate ->
                _uiState.update { it.copy(englishTtsRate = rate) }
            }
        }

        _uiState.update { it.copy(currentKoreanTtsService = ttsOrchestrator.getCurrentKoreanTtsServiceName()) }
    }

    fun setUserLevel(level: UserLevel) {
        viewModelScope.launch {
            userLevelPreferences.setUserLevel(level)
        }
    }

    fun setRepeatListeningCount(count: Int) {
        playbackPreferences.setRepeatListeningCount(count.coerceIn(2, 10))
    }

    fun setAnswerPlayCount(count: Int) {
        playbackPreferences.setAnswerPlayCount(count.coerceIn(1, 10))
    }

    fun setEnglishTtsRate(rate: Float) {
        ttsPreferences.setEnglishTtsRate(rate.coerceIn(0.5f, 1.5f))
    }
}
