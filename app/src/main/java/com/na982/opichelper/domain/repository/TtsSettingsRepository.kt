package com.na982.opichelper.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * TTS 설정 관리 인터페이스
 * TTS 속도 설정만 담당
 */
interface TtsSettingsRepository {
    /**
     * 영문 TTS 속도 가져오기
     */
    fun getEnglishTtsRate(): Float
    
    /**
     * 영문 TTS 속도 설정
     */
    fun setEnglishTtsRate(rate: Float)
    
    /**
     * 영문 TTS 속도 StateFlow
     */
    val englishTtsRate: StateFlow<Float>
}
