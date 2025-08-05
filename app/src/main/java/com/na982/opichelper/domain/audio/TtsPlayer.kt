package com.na982.opichelper.domain.audio

/**
 * TTS 플레이어 인터페이스
 * 모든 TTS 구현체가 따라야 하는 계약
 */
interface TtsPlayer {
    /**
     * 텍스트를 음성으로 재생
     * @param text 재생할 텍스트
     * @param onComplete 재생 완료 콜백
     * @return 재생 성공 여부
     */
    suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean
    
    /**
     * TTS 재생 중지
     */
    fun stop()
    
    /**
     * TTS 일시 중지
     */
    fun pause()
    
    /**
     * TTS 재개
     */
    fun resume()
    
    /**
     * 현재 재생 중인지 확인
     */
    fun isPlaying(): Boolean
    
    /**
     * TTS 서비스 사용 가능 여부
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * TTS 서비스 이름
     */
    fun getServiceName(): String
    
    /**
     * 문장별 하이라이트와 함께 TTS 재생
     */
    suspend fun speakWithHighlight(text: String, onHighlight: (Int) -> Unit)
    
    /**
     * TTS 재생 후 재생 시간 반환
     */
    suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long
    
    /**
     * TTS 플레이어 완전 해제 (앱 종료 시 사용)
     */
    fun release()
} 