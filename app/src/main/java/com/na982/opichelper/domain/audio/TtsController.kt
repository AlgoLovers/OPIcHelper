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
     * 문장별 하이라이트와 함께 TTS 재생
     * @param text 재생할 텍스트
     * @param isKorean 한글 여부
     * @param onHighlight 하이라이트 콜백
     * @return 재생 시간 (밀리초)
     */
    suspend fun playSentenceWithHighlight(
        text: String,
        isKorean: Boolean,
        onHighlight: (Int) -> Unit
    ): Long
    
    /**
     * 통합 TTS 재생 (하이라이트 없음)
     * @param text 재생할 텍스트
     * @param isKorean 한글 여부
     * @param rate 재생 속도 (기본 1.0f)
     * @param waitForCompletion 완료까지 대기 여부
     * @return 재생 시간 (밀리초)
     */
    suspend fun playUnified(
        text: String,
        isKorean: Boolean,
        rate: Float = 1.0f,
        waitForCompletion: Boolean = true
    ): Long
    
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