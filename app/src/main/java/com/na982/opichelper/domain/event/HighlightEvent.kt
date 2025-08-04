package com.na982.opichelper.domain.event

import com.na982.opichelper.domain.audio.HighlightType

/**
 * 하이라이트 관련 이벤트
 */
sealed class HighlightEvent {
    /**
     * 하이라이트 업데이트 이벤트
     */
    data class UpdateHighlight(
        val type: HighlightType,
        val index: Int
    ) : HighlightEvent()
    
    /**
     * 하이라이트 제거 이벤트
     */
    object ClearHighlight : HighlightEvent()
    
    /**
     * 모든 하이라이트 제거 이벤트
     */
    object ClearAllHighlights : HighlightEvent()
} 