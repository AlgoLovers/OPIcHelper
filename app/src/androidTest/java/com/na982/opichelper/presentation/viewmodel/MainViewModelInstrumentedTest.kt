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
    fun selectCategory_showsFirstQaItem() {
        viewModel.selectCategory(QuestionCategory.PERSONAL)
        val qaItem = viewModel.uiState.value.currentQaItem
        assertNotNull(qaItem)
        assertEquals(QuestionCategory.PERSONAL, qaItem?.category)
    }

    @Test
    fun nextQaItem_incrementsIndex() {
        viewModel.selectCategory(QuestionCategory.TRAVEL)
        val first = viewModel.uiState.value.currentQaItem
        viewModel.nextQaItem()
        val second = viewModel.uiState.value.currentQaItem
        assertNotEquals(first, second)
    }

    @Test
    fun selectCategory_withNoItems_showsError() {
        // 없는 카테고리(예: EDUCATION) 선택
        viewModel.selectCategory(QuestionCategory.EDUCATION)
        assertNull(viewModel.uiState.value.currentQaItem)
        assertNotNull(viewModel.uiState.value.error)
    }
} 