package com.na982.opichelper.domain.audio

interface TtsPlayer {
    suspend fun speak(text: String): TtsSpeakResult
    fun setSpeechRate(rate: Float)
    fun stop()
    fun pause()
    fun resume()
    fun isPlaying(): Boolean
    fun isAvailable(): Boolean
    fun getServiceName(): String
    fun release()
}
