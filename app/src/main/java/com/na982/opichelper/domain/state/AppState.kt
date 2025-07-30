package com.na982.opichelper.domain.state

import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.MemorizeLevel

/**
 * 앱의 전체 상태를 관리하는 데이터 클래스
 * 모든 UI 상태를 중앙에서 관리
 */
data class AppState(
    // 현재 QA 아이템
    val currentQaItem: com.na982.opichelper.domain.entity.QaItem? = null,
    val currentCategory: String? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    
    // 선택된 암기 레벨
    val selectedMemorizeLevel: String = "",
    
    // 버튼 상태들
    val buttonStates: Map<ButtonFunction, ButtonState> = mapOf(
        ButtonFunction.QuestionPlay to ButtonState.Idle,
        ButtonFunction.AnswerPlay to ButtonState.Idle,
        ButtonFunction.MemorizeTest to ButtonState.Idle,
        ButtonFunction.RecordingPlay to ButtonState.Idle
    ),
    
    // TTS 재생 상태
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    
    // 하이라이트 상태
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,
    
    // 카드 상태
    val isQuestionCardFlipped: Boolean = false,
    val isAnswerCardFlipped: Boolean = false,
    
    // 암기 모드 상태
    val isRepeatListeningMode: Boolean = false,
    val isEnglishWritingTestMode: Boolean = false,
    val isFullMemorizationMode: Boolean = false,
    
    // 로딩 및 에러 상태
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 특정 버튼의 상태를 가져오는 함수
     */
    fun getButtonState(buttonFunction: ButtonFunction): ButtonState {
        return buttonStates[buttonFunction] ?: ButtonState.Idle
    }
    
    /**
     * 버튼 상태를 업데이트하는 함수
     */
    fun updateButtonState(buttonFunction: ButtonFunction, newState: ButtonState): AppState {
        return copy(
            buttonStates = buttonStates + (buttonFunction to newState)
        )
    }
    
    /**
     * 현재 암기 모드가 통암기인지 확인
     */
    fun isFullMemorizationModeSelected(): Boolean {
        return selectedMemorizeLevel == "통암기"
    }
} 