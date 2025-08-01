package com.na982.opichelper.domain.strategy

import com.na982.opichelper.domain.entity.MemorizeLevel

/**
 * 암기 전략을 정의하는 인터페이스
 * 각 암기레벨별로 다른 동작을 구현
 */
interface MemorizationStrategy {
    /**
     * 암기 전략 실행
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param answerKo 한국어 답변
     * @param answerEn 영어 답변
     * @param uiCallback UI 콜백
     */
    suspend fun execute(
        category: String,
        scriptIndex: Int,
        answerKo: String,
        answerEn: String,
        uiCallback: MemorizationUiCallback
    )
    
    /**
     * 해당 전략이 처리하는 암기레벨 반환
     */
    fun getMemorizeLevel(): MemorizeLevel
}

/**
 * 암기 전략에서 사용하는 UI 콜백 인터페이스
 */
interface MemorizationUiCallback {
    fun onCardFlip(isKorean: Boolean) {}
    fun onHighlight(index: Int?) {}
    fun onKoreanHighlight(index: Int?) {}
    fun onRecordingHighlight(index: Int?) {}
    fun onRecordingStateChange(isRecording: Boolean) {}
    fun onPlayingStateChange(isPlaying: Boolean) {}
    fun onMergedFileCreated() {}
    fun onComplete() {}
} 