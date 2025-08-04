package com.na982.opichelper.domain.audio.strategy

import android.util.Log
import com.na982.opichelper.domain.audio.HighlightStrategy
import com.na982.opichelper.domain.audio.HighlightType

/**
 * TTS 질문 하이라이트 전략
 */
class QuestionHighlightStrategy : HighlightStrategy {
    override fun calculateHighlightIndex(currentPositionMs: Int): Int {
        // TTS 질문은 보통 첫 번째 문장만 하이라이트
        Log.d("QuestionHighlightStrategy", "질문 하이라이트: 현재시간=${currentPositionMs}ms")
        return 0
    }
    
    override fun getHighlightType(): HighlightType = HighlightType.QUESTION
    
    override fun isValid(): Boolean = true
}

/**
 * TTS 답변 하이라이트 전략
 */
class AnswerHighlightStrategy : HighlightStrategy {
    override fun calculateHighlightIndex(currentPositionMs: Int): Int {
        // TTS 답변은 보통 첫 번째 문장만 하이라이트
        Log.d("AnswerHighlightStrategy", "답변 하이라이트: 현재시간=${currentPositionMs}ms")
        return 0
    }
    
    override fun getHighlightType(): HighlightType = HighlightType.ANSWER
    
    override fun isValid(): Boolean = true
}

/**
 * 반복듣기 하이라이트 전략
 */
class RepeatListeningHighlightStrategy : HighlightStrategy {
    override fun calculateHighlightIndex(currentPositionMs: Int): Int {
        // 반복듣기는 현재 재생 중인 문장을 하이라이트
        Log.d("RepeatListeningHighlightStrategy", "반복듣기 하이라이트: 현재시간=${currentPositionMs}ms")
        return 0
    }
    
    override fun getHighlightType(): HighlightType = HighlightType.REPEAT_LISTENING
    
    override fun isValid(): Boolean = true
}

/**
 * 암기테스트 하이라이트 전략
 */
class MemorizationTestHighlightStrategy : HighlightStrategy {
    override fun calculateHighlightIndex(currentPositionMs: Int): Int {
        // 암기테스트는 현재 진행 중인 문장을 하이라이트
        Log.d("MemorizationTestHighlightStrategy", "암기테스트 하이라이트: 현재시간=${currentPositionMs}ms")
        return 0
    }
    
    override fun getHighlightType(): HighlightType = HighlightType.MEMORIZATION_TEST
    
    override fun isValid(): Boolean = true
}

/**
 * 통암기 테스트 하이라이트 전략
 */
class FullMemorizationTestHighlightStrategy : HighlightStrategy {
    override fun calculateHighlightIndex(currentPositionMs: Int): Int {
        // 통암기 테스트는 현재 진행 중인 문장을 하이라이트
        Log.d("FullMemorizationTestHighlightStrategy", "통암기 테스트 하이라이트: 현재시간=${currentPositionMs}ms")
        return 0
    }
    
    override fun getHighlightType(): HighlightType = HighlightType.FULL_MEMORIZATION_TEST
    
    override fun isValid(): Boolean = true
} 