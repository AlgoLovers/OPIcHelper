package com.na982.opichelper.domain.audio

sealed class TtsSpeakResult {
    data class Success(val durationMs: Long) : TtsSpeakResult()
    data class Error(val message: String) : TtsSpeakResult()
    object Timeout : TtsSpeakResult()
    object Unavailable : TtsSpeakResult()
}
