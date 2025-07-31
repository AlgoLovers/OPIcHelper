package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * TTS 제어를 위한 Domain Layer 인터페이스
 * 클린 아키텍처 원칙: Domain Layer가 Infrastructure Layer에 의존하지 않음
 */
interface TtsController {
    /**
     * 질문 TTS 재생 (하이라이트 포함)
     */
    suspend fun playQuestion(question: String)
    
    /**
     * 답변 TTS 재생 (하이라이트 포함)
     */
    suspend fun playAnswer(answer: String)
    
    /**
     * TTS 중지 (하이라이트 초기화 포함)
     */
    suspend fun stopTts()
    
    /**
     * 모든 TTS 중지 (하이라이트 초기화 포함)
     */
    suspend fun stopAllTts()
    
    /**
     * 현재 재생 상태
     */
    fun isPlaying(): StateFlow<Boolean>
    
    /**
     * 질문 재생 상태
     */
    fun isQuestionPlaying(): StateFlow<Boolean>
    
    /**
     * 답변 재생 상태
     */
    fun isAnswerPlaying(): StateFlow<Boolean>
} 