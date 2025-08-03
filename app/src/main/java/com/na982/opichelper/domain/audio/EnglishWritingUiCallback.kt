package com.na982.opichelper.domain.audio

/**
 * 영작 테스트 모드의 UI 콜백을 정의하는 인터페이스
 */
interface EnglishWritingUiCallback {
    fun onCardFlip(isKorean: Boolean)
    fun onKoreanHighlight(index: Int)
    fun onRecordingHighlight(index: Int)
    fun onRecordingStateChange(isRecording: Boolean)
    fun onMergedFileCreated()
    fun onComplete()
} 