package com.na982.opichelper.presentation.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.domain.entity.QuestionCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainViewModelInstrumentedTest {
    private lateinit var viewModel: MainViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel(context as android.app.Application)
    }

    @Test
    fun readPersonalCategoryQuestionsFromAssets() {
        viewModel.selectCategory(QuestionCategory.PERSONAL)
        val question = viewModel.uiState.value.currentQuestion
        assertNotNull(question)
        assertEquals(QuestionCategory.PERSONAL, question?.category)
        assertTrue(question?.question?.isNotEmpty() == true)
    }

    @Test
    fun readTravelCategoryQuestionsFromAssets() {
        viewModel.selectCategory(QuestionCategory.TRAVEL)
        val question = viewModel.uiState.value.currentQuestion
        assertNotNull(question)
        assertEquals(QuestionCategory.TRAVEL, question?.category)
        assertTrue(question?.question?.isNotEmpty() == true)
    }

    @Test
    fun readWorkCategoryQuestionsFromAssets() {
        viewModel.selectCategory(QuestionCategory.WORK)
        val question = viewModel.uiState.value.currentQuestion
        assertNotNull(question)
        assertEquals(QuestionCategory.WORK, question?.category)
        assertTrue(question?.question?.isNotEmpty() == true)
    }
} 