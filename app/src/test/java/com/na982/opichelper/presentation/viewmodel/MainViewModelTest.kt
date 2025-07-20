package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.na982.opichelper.domain.entity.QaItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class MainViewModelTest {
    private val itemsByCategory = mapOf(
        "personal" to listOf(
            QaItem(
                id = "1",
                category = "personal",
                questionEn = "Q1",
                questionKo = "Q1K",
                answerEn = "A1",
                answerKo = "A1K"
            )
        ),
        "travel" to listOf(
            QaItem(
                id = "2",
                category = "travel",
                questionEn = "Q2",
                questionKo = "Q2K",
                answerEn = "A2",
                answerKo = "A2K"
            ),
            QaItem(
                id = "2b",
                category = "travel",
                questionEn = "Q2b",
                questionKo = "Q2Kb",
                answerEn = "A2b",
                answerKo = "A2Kb"
            )
        ),
        "work" to listOf(
            QaItem(
                id = "3",
                category = "work",
                questionEn = "Q3",
                questionKo = "Q3K",
                answerEn = "A3",
                answerKo = "A3K"
            )
        )
    )

    @Test
    fun `카테고리 선택 시 첫 번째 질문이 노출된다`() {
        val viewModel = MainViewModel(itemsByCategory)
        viewModel.selectCategory("personal")
        val qaItem = viewModel.uiState.value.currentQaItem
        assertNotNull(qaItem)
        assertEquals("personal", qaItem?.category)
        assertEquals("Q1", qaItem?.questionEn)
        assertEquals("A1", qaItem?.answerEn)
    }

    @Test
    fun `nextQaItem 호출 시 인덱스가 순차적으로 증가한다`() {
        val viewModel = MainViewModel(itemsByCategory)
        viewModel.selectCategory("travel")
        val first = viewModel.uiState.value.currentQaItem
        assertEquals("Q2", first?.questionEn)
        viewModel.nextQaItem()
        val second = viewModel.uiState.value.currentQaItem
        assertEquals("Q2b", second?.questionEn)
        assertNotEquals(first, second)
    }

    @Test
    fun `질문이 없는 카테고리 선택 시 에러가 발생한다`() {
        val viewModel = MainViewModel(itemsByCategory)
        viewModel.selectCategory("education")
        assertNull(viewModel.uiState.value.currentQaItem)
        // 에러 메시지가 설정되는지 확인
        assertTrue(viewModel.uiState.value.error?.isNotEmpty() == true)
    }

    @Test
    fun `nextQaItem 호출 시 마지막에서 처음으로 순환된다`() {
        val viewModel = MainViewModel(itemsByCategory)
        viewModel.selectCategory("work")
        val first = viewModel.uiState.value.currentQaItem?.questionEn
        assertEquals("Q3", first)
        
        // work 카테고리에는 1개 아이템만 있으므로 nextQaItem 호출 시 첫 번째로 돌아감
        viewModel.nextQaItem()
        val current = viewModel.uiState.value.currentQaItem?.questionEn
        assertEquals("Q3", current) // 순환되어 같은 아이템
    }
}