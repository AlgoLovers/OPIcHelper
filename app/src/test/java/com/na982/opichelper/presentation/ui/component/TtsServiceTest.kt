package com.na982.opichelper.presentation.ui.component

import org.junit.Test
import org.junit.Assert.*

class TtsServiceTest {
    
    @Test
    fun `TtsService callback interface works correctly`() {
        // TtsService.HighlightCallback 인터페이스가 올바르게 정의되어 있는지 확인
        val callback = object : TtsService.HighlightCallback {
            var questionIndex: Int? = null
            var answerIndex: Int? = null
            
            override fun onQuestionHighlight(index: Int?) {
                questionIndex = index
            }
            
            override fun onAnswerHighlight(index: Int?) {
                answerIndex = index
            }
        }
        
        // 콜백 테스트
        callback.onQuestionHighlight(2)
        callback.onAnswerHighlight(3)
        
        assertEquals(2, callback.questionIndex)
        assertEquals(3, callback.answerIndex)
    }

    @Test
    fun `TtsService callback can handle null values`() {
        val callback = object : TtsService.HighlightCallback {
            var questionIndex: Int? = null
            var answerIndex: Int? = null
            
            override fun onQuestionHighlight(index: Int?) {
                questionIndex = index
            }
            
            override fun onAnswerHighlight(index: Int?) {
                answerIndex = index
            }
        }
        
        // null 값 테스트
        callback.onQuestionHighlight(null)
        callback.onAnswerHighlight(null)
        
        assertNull(callback.questionIndex)
        assertNull(callback.answerIndex)
    }
} 