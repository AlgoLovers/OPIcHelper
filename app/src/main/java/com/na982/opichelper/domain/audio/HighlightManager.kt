package com.na982.opichelper.domain.audio

import com.na982.opichelper.domain.event.HighlightEventHandler

/**
 * 하이라이트 매니저 인터페이스
 * 모든 재생 관련 하이라이트를 중앙에서 관리합니다.
 */
interface HighlightManager {
    /**
     * 현재 전략 설정
     * @param strategy 하이라이트 전략
     */
    fun setStrategy(strategy: HighlightStrategy)
    
    /**
     * 하이라이트 업데이트 시작
     * @param eventHandler 이벤트 핸들러
     */
    fun startHighlightUpdates(eventHandler: HighlightEventHandler)
    
    /**
     * 하이라이트 업데이트 중지
     */
    fun stopHighlightUpdates()
    
    /**
     * 현재 하이라이트 상태 반환
     * @return 현재 하이라이트 인덱스 (-1은 하이라이트 없음)
     */
    fun getCurrentHighlightIndex(): Int
    
    /**
     * 현재 하이라이트 타입 반환
     * @return 현재 하이라이트 타입
     */
    fun getCurrentHighlightType(): HighlightType?
    
    /**
     * 모든 하이라이트 중지
     */
    fun stopAllHighlights()
    
    /**
     * 현재 재생 위치 업데이트
     * @param currentPositionMs 현재 재생 위치 (밀리초)
     */
    fun updateCurrentPosition(currentPositionMs: Int)
} 