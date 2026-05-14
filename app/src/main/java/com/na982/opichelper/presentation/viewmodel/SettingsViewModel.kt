package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class SettingsUiState(
    val currentUserLevel: String = "",
    val currentKoreanTtsService: String = "",
    val repeatListeningCount: Int = 5,
    val answerPlayCount: Int = 1
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ttsOrchestrator: TtsOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLevel.collect { userLevel ->
                _uiState.value = _uiState.value.copy(currentUserLevel = userLevel.name)
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.repeatListeningCount.collect { count ->
                _uiState.value = _uiState.value.copy(repeatListeningCount = count)
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.answerPlayCount.collect { count ->
                _uiState.value = _uiState.value.copy(answerPlayCount = count)
            }
        }

        _uiState.value = _uiState.value.copy(
            currentKoreanTtsService = ttsOrchestrator.getCurrentKoreanTtsServiceName()
        )
    }

    fun setUserLevel(level: UserLevel) {
        viewModelScope.launch {
            userPreferencesRepository.setUserLevel(level)
        }
    }

    fun setRepeatListeningCount(count: Int) {
        userPreferencesRepository.setRepeatListeningCount(count.coerceIn(2, 10))
    }

    fun setAnswerPlayCount(count: Int) {
        userPreferencesRepository.setAnswerPlayCount(count.coerceIn(1, 10))
    }
}
