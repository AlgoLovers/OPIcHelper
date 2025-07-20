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
     * TTS 재생 중지
     */
    fun stopTts()

    // 한글/영문 TTS 재생 후 재생 시간(ms) 반환
    suspend fun speakAndGetDuration(text: String, isKorean: Boolean = false, rate: Float = 0.75f): Long

    // 문장별 하이라이트 콜백과 함께 TTS 재생
    suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit)
} 