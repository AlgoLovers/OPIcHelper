package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class HighlightInfo(val index: Int? = null, val sentence: String? = null)

@Singleton
class HighlightStateHolder @Inject constructor() {
    private val _questionHighlight = MutableStateFlow(HighlightInfo())
    val questionHighlight: StateFlow<HighlightInfo> = _questionHighlight.asStateFlow()

    private val _answerHighlight = MutableStateFlow(HighlightInfo())
    val answerHighlight: StateFlow<HighlightInfo> = _answerHighlight.asStateFlow()

    private val _answerKoHighlight = MutableStateFlow(HighlightInfo())
    val answerKoHighlight: StateFlow<HighlightInfo> = _answerKoHighlight.asStateFlow()

    private val _recordingHighlight = MutableStateFlow(HighlightInfo())
    val recordingHighlight: StateFlow<HighlightInfo> = _recordingHighlight.asStateFlow()

    fun setQuestionHighlight(index: Int?, sentence: String? = null) {
        _questionHighlight.update { HighlightInfo(index, sentence) }
    }

    fun setAnswerHighlight(index: Int?, sentence: String? = null) {
        _answerHighlight.update { HighlightInfo(index, sentence) }
    }

    fun setAnswerKoHighlight(index: Int?, sentence: String? = null) {
        _answerKoHighlight.update { HighlightInfo(index, sentence) }
    }

    fun setRecordingHighlight(index: Int?) {
        _recordingHighlight.update { HighlightInfo(index) }
    }

    fun clearHighlight() {
        _questionHighlight.update { HighlightInfo() }
        _answerHighlight.update { HighlightInfo() }
        _answerKoHighlight.update { HighlightInfo() }
        _recordingHighlight.update { HighlightInfo() }
    }
}
