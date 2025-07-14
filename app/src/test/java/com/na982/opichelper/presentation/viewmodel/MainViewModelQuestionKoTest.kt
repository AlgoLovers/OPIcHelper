package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.QaItem
import org.junit.Assert.*
import org.junit.Test

class MainViewModelQuestionKoTest {

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
            )
        )
    )

    @Test
    fun qaItem_has_korean_translation() {
        val qa = QaItem(
            id = "1",
            category = "personal",
            questionEn = "How are you?",
            questionKo = "어떻게 지내세요?",
            answerEn = "I'm fine, thank you.",
            answerKo = "저는 잘 지내고 있습니다."
        )
        assertEquals("어떻게 지내세요?", qa.questionKo)
        assertEquals("How are you?", qa.questionEn)
        assertEquals("I'm fine, thank you.", qa.answerEn)
        assertEquals("저는 잘 지내고 있습니다.", qa.answerKo)
    }

    @Test
    fun `카테고리 선택 시 한글 질문이 노출된다`() {
        val viewModel = MainViewModel(itemsByCategory)
        viewModel.selectCategory("personal")
        val qaItem = viewModel.uiState.value.currentQaItem
        assertNotNull(qaItem)
        assertEquals("Q1K", qaItem?.questionKo)
    }
} 