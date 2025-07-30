package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * 버튼 상태 관찰을 위한 인터페이스
 * 의존성 역전 원칙을 적용하여 Domain Layer가 추상화에 의존하도록 함
 */
interface ButtonStateObserver {
    /**
     * 질문 재생 상태
     */
    val isQuestionPlaying: StateFlow<Boolean>
    
    /**
     * 답변 재생 상태
     */
    val isAnswerPlaying: StateFlow<Boolean>
    
    /**
     * 재생 완료 시 콜백 등록
     */
    fun onQuestionPlaybackCompleted(callback: () -> Unit)
    
    fun onAnswerPlaybackCompleted(callback: () -> Unit)
} 