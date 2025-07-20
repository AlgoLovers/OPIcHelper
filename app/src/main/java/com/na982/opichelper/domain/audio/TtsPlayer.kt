package com.na982.opichelper.domain.audio

/**
 * TTS 재생 기능을 위한 인터페이스
 */
interface TtsPlayer {
    /**
     * 질문을 TTS로 재생
     * @param text 재생할 텍스트
     * @param rate 재생 속도 (기본값: 0.8f)
     */
    fun speakQuestion(text: String, rate: Float = 0.8f)
    
    /**
     * 답변을 TTS로 재생
     * @param text 재생할 텍스트
     * @param rate 재생 속도 (기본값: 0.8f)
     */
    fun speakAnswer(text: String, rate: Float = 0.8f)
    
    /**
     * 답변을 지정된 횟수만큼 반복 재생
     * @param text 재생할 텍스트
     * @param repeatCount 반복 횟수
     * @param rate 재생 속도 (기본값: 0.8f)
     */
    fun speakAnswer(text: String, repeatCount: Int, rate: Float = 0.8f)
    
    /**
     * TTS 재생 중지
     */
    fun stopTts()
} 