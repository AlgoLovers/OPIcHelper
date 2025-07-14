package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.Question
import com.na982.opichelper.domain.entity.QuestionCategory
import org.junit.Assert.*
import org.junit.Test

class MainViewModelQuestionKoTest {

    @Test
    fun question_has_korean_translation() {
        val q = Question(
            id = "1",
            question = "How are you?",
            questionKo = "어떻게 지내세요?",
            category = QuestionCategory.PERSONAL
        )
        assertEquals("어떻게 지내세요?", q.questionKo)
        assertEquals("How are you?", q.question)
    }
} 