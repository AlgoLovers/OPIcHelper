package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.entity.Question
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.domain.entity.QuestionDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        loadRandomQuestion()
    }
    
    fun loadRandomQuestion(category: QuestionCategory? = null, difficulty: QuestionDifficulty? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 임시로 샘플 데이터 사용
                val sampleQuestion = Question(
                    question = "Tell me about your hometown.",
                    category = QuestionCategory.PERSONAL,
                    difficulty = QuestionDifficulty.EASY,
                    sampleAnswer = "I'm from Seoul, the capital city of South Korea. It's a vibrant and modern city with a population of over 10 million people."
                )
                _uiState.value = _uiState.value.copy(
                    currentQuestion = sampleQuestion,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class MainUiState(
    val currentQuestion: Question? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) 