package com.na982.opichelper.domain.entity

/**
 * TTS 관련 상수 정의
 * 매직 넘버 제거 및 중앙화된 설정 관리
 */
object TtsConstants {
    // TTS 속도 관련 상수
    const val DEFAULT_TTS_RATE = 0.7f  // 영문 기본 속도
    const val KOREAN_TTS_RATE = 1.0f   // 한글 기본 속도
    const val MIN_TTS_RATE = 0.5f
    const val MAX_TTS_RATE = 1.5f
    
    // TTS 피치 관련 상수
    const val DEFAULT_PITCH = 1.0f
    const val MIN_PITCH = 0.9f
    const val MAX_PITCH = 1.15f
    
    // TTS 볼륨 관련 상수
    const val DEFAULT_VOLUME = 1.2f
    
    // SharedPreferences 키
    object PrefKeys {
        const val ENGLISH_TTS_RATE = "english_tts_rate"
        const val USER_LEVEL = "user_level"
    }
} 