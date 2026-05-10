package com.na982.opichelper.domain.audio

sealed class PlaybackEvent {
    data class CardFlip(val isKorean: Boolean) : PlaybackEvent()
    data class Highlight(val index: Int?) : PlaybackEvent()
    data class KoreanHighlight(val index: Int?) : PlaybackEvent()
    data class RecordingHighlight(val index: Int?) : PlaybackEvent()
    data class RecordingStateChange(val isRecording: Boolean) : PlaybackEvent()
    data class PlayingStateChange(val isPlaying: Boolean) : PlaybackEvent()
    data object MergedFileCreated : PlaybackEvent()
    data object Complete : PlaybackEvent()
}
