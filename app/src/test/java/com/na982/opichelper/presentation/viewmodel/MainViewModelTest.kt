package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.na982.opichelper.domain.entity.Question
import com.na982.opichelper.domain.entity.QuestionCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class MainViewModelTest {
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        // Mock data for testing
        val questions = mapOf(
            QuestionCategory.PERSONAL to listOf(
                Question(id = "1", question = "Personal Q1", category = QuestionCategory.PERSONAL),
                Question(id = "2", question = "Personal Q2", category = QuestionCategory.PERSONAL)
            ),
            QuestionCategory.TRAVEL to listOf(
                Question(id = "3", question = "Travel Q1", category = QuestionCategory.TRAVEL),
                Question(id = "4", question = "Travel Q2", category = QuestionCategory.TRAVEL)
            ),
            QuestionCategory.WORK to listOf(
                Question(id = "5", question = "Work Q1", category = QuestionCategory.WORK),
                Question(id = "6", question = "Work Q2", category = QuestionCategory.WORK)
            )
        )
        viewModel = MainViewModel(questions)
    }

    @Test
    fun `카테고리 선택 시 첫 번째 질문이 노출된다`() {
        viewModel.selectCategory(QuestionCategory.PERSONAL)
        val question = viewModel.uiState.value.currentQuestion
        assertNotNull(question)
        assertEquals(QuestionCategory.PERSONAL, question?.category)
    }

    @Test
    fun `nextQuestion 호출 시 인덱스가 순차적으로 증가한다`() {
        viewModel.selectCategory(QuestionCategory.TRAVEL)
        val first = viewModel.uiState.value.currentQuestion
        viewModel.nextQuestion()
        val second = viewModel.uiState.value.currentQuestion
        assertNotEquals(first, second)
    }

    @Test
    fun `질문이 없는 카테고리 선택 시 에러가 발생한다`() {
        // 없는 카테고리(예: EDUCATION) 선택
        viewModel.selectCategory(QuestionCategory.EDUCATION)
        assertNull(viewModel.uiState.value.currentQuestion)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `nextQuestion 호출 시 마지막에서 처음으로 순환된다`() {
        viewModel.selectCategory(QuestionCategory.WORK)
        val first = viewModel.uiState.value.currentQuestion?.question
        val questionCount = 2 // WORK 카테고리 질문 개수
        repeat(questionCount) {
            viewModel.nextQuestion()
        }
        val current = viewModel.uiState.value.currentQuestion?.question
        assertEquals(first, current)
    }
} 