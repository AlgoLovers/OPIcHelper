package com.na982.opichelper.domain.audio

/**
 * 하이라이트 타입 정의
 */
enum class HighlightType {
    QUESTION,                    // 질문 하이라이트
    ANSWER,                     // 답변 하이라이트  
    REPEAT_LISTENING,           // 반복듣기 하이라이트
    MEMORIZATION_TEST,          // 암기테스트 하이라이트
    ENGLISH_WRITING_RECORDING,  // 영작테스트 녹음 재생
    FULL_MEMORIZATION_TEST,     // 통암기 암기테스트
    FULL_MEMORIZATION_RECORDING // 통암기 녹음 재생
} 