package com.na982.opichelper.domain.audio

interface TtsHighlightController {
    fun setQuestionHighlightIndex(index: Int)
    fun setAnswerHighlightIndex(index: Int, sentence: String? = null)
    fun setAnswerKoHighlightIndex(index: Int, sentence: String? = null)
    fun setRecordingHighlightIndex(index: Int)
    fun clearHighlight()
}
