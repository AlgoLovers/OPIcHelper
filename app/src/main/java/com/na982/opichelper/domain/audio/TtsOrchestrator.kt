package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.StateFlow

interface TtsOrchestrator {
    suspend fun speakWithHighlight(text: String, onHighlight: (index: Int?, sentence: String?) -> Unit): TtsSpeakResult
    suspend fun speakAndWaitForCompletion(text: String): TtsSpeakResult
    fun stop()
    fun releaseAllPlayers()
    fun getCurrentKoreanTtsServiceName(): String
}
