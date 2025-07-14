package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.entity.Question
import com.na982.opichelper.domain.entity.QuestionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class MainViewModel : AndroidViewModel {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val questionsByCategory: MutableMap<QuestionCategory, List<Question>>
    private val questionIndexByCategory: MutableMap<QuestionCategory, Int> = mutableMapOf()

    // Primary constructor for test (inject data)
    constructor(questionsByCategory: Map<QuestionCategory, List<Question>>) : super(Application()) {
        this.questionsByCategory = questionsByCategory.toMutableMap()
        for (category in questionsByCategory.keys) {
            questionIndexByCategory[category] = 0
        }
    }

    // Secondary constructor for production (load from assets)
    constructor(application: Application) : this(mutableMapOf()) {
        loadQuestionsFromAssets(application)
    }

    private fun loadQuestionsFromAssets(application: Application) {
        val context = application
        val gson = Gson()
        val categories = listOf(
            QuestionCategory.PERSONAL,
            QuestionCategory.TRAVEL,
            QuestionCategory.WORK
        )
        val fileNames = mapOf(
            QuestionCategory.PERSONAL to "questions_personal.json",
            QuestionCategory.TRAVEL to "questions_travel.json",
            QuestionCategory.WORK to "questions_work.json"
        )
        for (category in categories) {
            try {
                val inputStream = context.assets.open(fileNames[category] ?: continue)
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<List<QuestionAsset>>() {}.type
                val assetQuestions: List<QuestionAsset> = gson.fromJson(reader, type)
                val questions = assetQuestions.map {
                    Question(
                        question = it.question_en, // 영문
                        questionKo = it.question_ko, // 한글 번역
                        category = category,
                        sampleAnswer = ""
                    )
                }
                questionsByCategory[category] = questions
                questionIndexByCategory[category] = 0
            } catch (e: Exception) {
                questionsByCategory[category] = emptyList()
                questionIndexByCategory[category] = 0
            }
        }
    }

    fun selectCategory(category: QuestionCategory) {
        val questions = questionsByCategory[category] ?: emptyList()
        val index = questionIndexByCategory[category] ?: 0
        if (questions.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                currentQuestion = questions[index],
                currentCategory = category,
                isLoading = false,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentQuestion = null,
                currentCategory = category,
                isLoading = false,
                error = "해당 카테고리에 질문이 없습니다."
            )
        }
    }

    fun nextQuestion() {
        val category = _uiState.value.currentCategory ?: return
        val questions = questionsByCategory[category] ?: return
        if (questions.isEmpty()) return
        val currentIndex = questionIndexByCategory[category] ?: 0
        val nextIndex = (currentIndex + 1) % questions.size
        questionIndexByCategory[category] = nextIndex
        _uiState.value = _uiState.value.copy(
            currentQuestion = questions[nextIndex],
            isLoading = false,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    data class QuestionAsset(
        val category: String,
        val question_en: String,
        val question_ko: String
    )
}

data class MainUiState(
    val currentQuestion: Question? = null,
    val currentCategory: QuestionCategory? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) 