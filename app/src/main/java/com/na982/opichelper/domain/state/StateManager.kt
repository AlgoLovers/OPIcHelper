package com.na982.opichelper.domain.state

/**
 * Domain Layer의 상태 관리 인터페이스
 * Clean Architecture 원칙에 따라 Domain Layer에서 사용
 */
interface StateManager {
    /**
     * 카드 상태 업데이트
     */
    fun updateCardState(
        isQuestionCardFlipped: Boolean? = null,
        isAnswerCardFlipped: Boolean? = null
    )
    
    /**
     * 하이라이트 상태 업데이트
     */
    fun updateHighlightState(
        questionHighlightIndex: Int = -1,
        answerHighlightIndex: Int = -1,
        answerKoHighlightIndex: Int = -1,
        recordingHighlightIndex: Int = -1
    )
    
    /**
     * 녹음 상태 업데이트
     */
    fun updateRecordingState(isRecording: Boolean)
    
    /**
     * 병합 파일 생성 완료 상태 업데이트
     */
    fun updateMergedFileCreated(created: Boolean)
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction, newState: com.na982.opichelper.domain.entity.ButtonState)
    
    /**
     * TTS 재생 상태 업데이트
     */
    fun updateTtsPlayingState(
        isQuestionPlaying: Boolean? = null,
        isAnswerPlaying: Boolean? = null,
        isPlaying: Boolean? = null
    )
} 