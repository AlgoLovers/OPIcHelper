package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.StateFlow

interface TtsPlaybackController : TtsHighlightController {
    val isPlaying: StateFlow<Boolean>
    val isQuestionPlaying: StateFlow<Boolean>
    val isAnswerPlaying: StateFlow<Boolean>
    val isPaused: StateFlow<Boolean>
    val questionHighlight: StateFlow<HighlightInfo>
    val answerHighlight: StateFlow<HighlightInfo>
    val answerKoHighlight: StateFlow<HighlightInfo>
    val recordingHighlight: StateFlow<HighlightInfo>

    fun playQuestion(question: String)
    fun playAnswer(answer: String)
    fun playMergedAudio(question: String, answer: String)
    fun stopTts()
    fun stopAndMarkPaused()
    fun clearPausedState()
    fun cleanupTts()
    fun stopWithoutClearingHighlight()
    fun close()
}
