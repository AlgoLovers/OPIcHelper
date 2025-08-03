package com.na982.opichelper.domain.strategy

import com.na982.opichelper.domain.entity.MemorizeLevel

/**
 * 녹음 재생 전략을 정의하는 인터페이스
 * 각 암기레벨별로 다른 녹음 재생 방식을 구현
 */
interface RecordingPlayStrategy {
    /**
     * 녹음 재생 실행
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param onHighlight 하이라이트 콜백
     * @param onCompletion 완료 콜백
     */
    suspend fun playRecording(
        category: String,
        scriptIndex: Int,
        onHighlight: (Int) -> Unit,
        onCompletion: () -> Unit
    )
    
    /**
     * 해당 전략이 처리하는 암기레벨 반환
     */
    fun getMemorizeLevel(): MemorizeLevel
} 