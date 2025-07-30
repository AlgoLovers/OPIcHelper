package com.na982.opichelper.domain.event

import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.MemorizeLevel

/**
 * 버튼 이벤트를 정의하는 sealed class
 * 모든 버튼 동작을 이벤트로 추상화
 */
sealed class ButtonEvent {
    data class QuestionPlayClick(
        val question: String,
        val isFullMemorizationMode: Boolean,
        val category: String,
        val scriptIndex: Int
    ) : ButtonEvent()
    
    data class AnswerPlayClick(
        val answer: String
    ) : ButtonEvent()
    
    data class MemorizeTestClick(
        val memorizeLevel: MemorizeLevel,
        val category: String,
        val scriptIndex: Int,
        val answerKo: String,
        val answerEn: String
    ) : ButtonEvent()
    
    data class RecordingPlayClick(
        val memorizeLevel: MemorizeLevel
    ) : ButtonEvent()
    
    data class StopClick(
        val buttonFunction: ButtonFunction
    ) : ButtonEvent()
}

/**
 * 이벤트 결과를 정의하는 sealed class
 */
sealed class ButtonEventResult {
    object Success : ButtonEventResult()
    data class Error(val message: String) : ButtonEventResult()
    data class StateChanged(val newState: com.na982.opichelper.domain.entity.ButtonState) : ButtonEventResult()
} 