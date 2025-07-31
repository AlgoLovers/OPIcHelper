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
    
    // TTS 재생 상태 (통합)
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val isPlaying: Boolean = false,
    
    // 하이라이트 상태 (통합)
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
    
    // TTS 서비스 상태
    val currentKoreanTtsService: String = "",
    
    // 암기 테스트 진행 상태
    val isMemorizeTestRunning: Boolean = false,
    val currentMemorizeMode: String = "NONE",
    
    // 영작 테스트 상태
    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false,
    
    // 로딩 및 에러 상태
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 버튼 상태 업데이트 헬퍼 메서드
     */
    fun updateButtonState(buttonFunction: ButtonFunction, newState: ButtonState): AppState {
        val updatedButtonStates = buttonStates.toMutableMap()
        updatedButtonStates[buttonFunction] = newState
        return copy(buttonStates = updatedButtonStates)
    }
    
    /**
     * TTS 재생 상태 업데이트 헬퍼 메서드
     */
    fun updateTtsPlayingState(
        isQuestionPlaying: Boolean = this.isQuestionPlaying,
        isAnswerPlaying: Boolean = this.isAnswerPlaying,
        isPlaying: Boolean = this.isPlaying
    ): AppState {
        return copy(
            isQuestionPlaying = isQuestionPlaying,
            isAnswerPlaying = isAnswerPlaying,
            isPlaying = isPlaying
        )
    }
    
    /**
     * 하이라이트 상태 업데이트 헬퍼 메서드
     */
    fun updateHighlightState(
        questionHighlightIndex: Int? = this.questionHighlightIndex,
        answerHighlightIndex: Int? = this.answerHighlightIndex,
        answerKoHighlightIndex: Int? = this.answerKoHighlightIndex,
        recordingHighlightIndex: Int? = this.recordingHighlightIndex
    ): AppState {
        return copy(
            questionHighlightIndex = questionHighlightIndex,
            answerHighlightIndex = answerHighlightIndex,
            answerKoHighlightIndex = answerKoHighlightIndex,
            recordingHighlightIndex = recordingHighlightIndex
        )
    }
    
    /**
     * 암기 모드 상태 업데이트 헬퍼 메서드
     */
    fun updateMemorizationModeState(
        isRepeatListeningMode: Boolean = this.isRepeatListeningMode,
        isEnglishWritingTestMode: Boolean = this.isEnglishWritingTestMode,
        isFullMemorizationMode: Boolean = this.isFullMemorizationMode
    ): AppState {
        return copy(
            isRepeatListeningMode = isRepeatListeningMode,
            isEnglishWritingTestMode = isEnglishWritingTestMode,
            isFullMemorizationMode = isFullMemorizationMode
        )
    }
    
    /**
     * 암기 테스트 실행 상태 확인
     */
    fun isFullMemorizationModeSelected(): Boolean {
        return selectedMemorizeLevel == "통암기"
    }
} 