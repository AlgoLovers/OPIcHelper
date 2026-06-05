package com.na982.opichelper.domain.audio

interface TtsHighlightController {
    fun setAnswerHighlightIndex(index: Int, sentence: String? = null)
    fun setAnswerKoHighlightIndex(index: Int, sentence: String? = null)
    fun setRecordingHighlightIndex(index: Int)
    fun clearHighlight()
}
