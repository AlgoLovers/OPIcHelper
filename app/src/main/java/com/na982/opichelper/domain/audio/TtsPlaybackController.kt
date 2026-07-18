package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface TtsPlaybackController : TtsHighlightController, TtsPauseController {
    val isPlaying: StateFlow<Boolean>
    val isQuestionPlaying: StateFlow<Boolean>
    val isAnswerPlaying: StateFlow<Boolean>
    val questionHighlight: StateFlow<HighlightInfo>
    val answerHighlight: StateFlow<HighlightInfo>
    val answerKoHighlight: StateFlow<HighlightInfo>
    val recordingHighlight: StateFlow<HighlightInfo>

    /** TTS 재생이 실패(Error/Timeout/Unavailable 또는 예외)했을 때 발행. 취소는 실패가 아니므로 제외. */
    val errors: SharedFlow<Unit>

    fun playQuestion(question: String)
    fun playAnswer(answer: String)
    fun stopTts()
    fun cleanupTts()
    fun stopWithoutClearingHighlight()
    fun reset()
    fun close()
}
