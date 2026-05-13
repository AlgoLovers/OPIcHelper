package com.na982.opichelper.domain.audio

sealed class MemorizeTestEvent {
    data class CardFlip(val isKorean: Boolean) : MemorizeTestEvent()
    data class Highlight(val index: Int?) : MemorizeTestEvent()
    data class KoreanHighlight(val index: Int?) : MemorizeTestEvent()
    data class RecordingHighlight(val index: Int?) : MemorizeTestEvent()
    data class RecordingStateChange(val isRecording: Boolean) : MemorizeTestEvent()
    object MergedFileCreated : MemorizeTestEvent()
    object Completed : MemorizeTestEvent()
}
