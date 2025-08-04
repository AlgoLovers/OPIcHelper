package com.na982.opichelper.domain.audio

/**
 * 하이라이트 전략 인터페이스
 * 각 상황에 맞는 하이라이트 계산 로직을 정의합니다.
 */
interface HighlightStrategy {
    /**
     * 현재 재생 시간에 해당하는 하이라이트 인덱스 계산
     * @param currentPositionMs 현재 재생 시간 (밀리초)
     * @return 하이라이트할 문장 인덱스 (-1은 하이라이트 없음)
     */
    fun calculateHighlightIndex(currentPositionMs: Int): Int
    
    /**
     * 하이라이트 타입 반환
     * @return 하이라이트 타입
     */
    fun getHighlightType(): HighlightType
    
    /**
     * 전략이 유효한지 확인
     * @return 유효하면 true
     */
    fun isValid(): Boolean
} 