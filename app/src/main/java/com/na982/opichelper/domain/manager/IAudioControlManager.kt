package com.na982.opichelper.domain.manager

import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.StateFlow

/**
 * 오디오 제어 인터페이스
 * 단일 책임: 오디오 재생만 담당
 */
interface IAudioControlManager {
    
    /**
     * 에러 상태
     */
    val error: StateFlow<String?>
    
    /**
     * 질문 재생
     */
    fun playQuestion(qaItem: QaItem, onCompletion: () -> Unit)
    
    /**
     * 답변 재생
     */
    fun playAnswer(qaItem: QaItem, onCompletion: () -> Unit)
    
    /**
     * 모든 오디오 중지
     */
    fun stopAllAudio()
    
    /**
     * 특정 버튼의 오디오 중지
     */
    fun stopSpecificAudio(buttonFunction: String)
} 