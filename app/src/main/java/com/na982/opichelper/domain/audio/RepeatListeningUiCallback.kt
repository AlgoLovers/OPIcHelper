package com.na982.opichelper.domain.audio

/**
 * 반복 듣기 모드의 UI 콜백을 정의하는 인터페이스
 */
interface RepeatListeningUiCallback {
    fun onCardFlip(isKorean: Boolean)
    fun onHighlight(index: Int)
    fun onKoreanHighlight(index: Int)
    fun onComplete()
} 