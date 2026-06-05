package com.na982.opichelper.domain.audio

interface TtsPlayer {
    suspend fun speak(text: String): TtsSpeakResult
    fun setSpeechRate(rate: Float)
    fun stop()
    fun isAvailable(): Boolean
    fun getServiceName(): String
    fun release()
}
