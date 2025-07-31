package com.na982.opichelper.domain.entity

/**
 * 버튼의 기본 상태를 나타내는 sealed class
 */
sealed class ButtonState {
    object Idle : ButtonState()
    object Loading : ButtonState()
    object Playing : ButtonState()
    object Recording : ButtonState()
    object Paused : ButtonState()
    object Error : ButtonState()
}

/**
 * 버튼의 동작을 정의하는 인터페이스
 */
interface ButtonAction {
    fun execute()
    fun stop()
    fun pause()
    fun resume()
}

/**
 * 암기 레벨별 버튼 타입을 정의하는 enum
 */
enum class MemorizeLevel {
    REPEAT_LISTENING,    // 반복 듣기
    ENGLISH_WRITING,     // 영작 테스트
    FULL_MEMORIZATION    // 통암기
}

/**
 * 버튼의 기능을 정의하는 sealed class
 */
sealed class ButtonFunction {
    object QuestionPlay : ButtonFunction()
    object AnswerPlay : ButtonFunction()
    object MemorizeTest : ButtonFunction()
    object RecordingPlay : ButtonFunction()
    object Stop : ButtonFunction()
}

/**
 * 버튼의 상태와 기능을 결합한 데이터 클래스
 */
data class ButtonConfig(
    val function: ButtonFunction,
    val state: ButtonState,
    val text: String,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true
) 