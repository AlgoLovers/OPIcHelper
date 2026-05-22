package com.na982.opichelper.domain.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HighlightStateHolder @Inject constructor() {
    private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
    val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex.asStateFlow()

    private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
    val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex.asStateFlow()

    private val _answerKoHighlightIndex = MutableStateFlow<Int?>(null)
    val answerKoHighlightIndex: StateFlow<Int?> = _answerKoHighlightIndex.asStateFlow()

    private val _recordingHighlightIndex = MutableStateFlow<Int?>(null)
    val recordingHighlightIndex: StateFlow<Int?> = _recordingHighlightIndex.asStateFlow()

    private val _currentQuestionSentence = MutableStateFlow<String?>(null)
    val currentQuestionSentence: StateFlow<String?> = _currentQuestionSentence.asStateFlow()

    private val _currentAnswerSentence = MutableStateFlow<String?>(null)
    val currentAnswerSentence: StateFlow<String?> = _currentAnswerSentence.asStateFlow()

    private val _currentAnswerKoSentence = MutableStateFlow<String?>(null)
    val currentAnswerKoSentence: StateFlow<String?> = _currentAnswerKoSentence.asStateFlow()

    fun setQuestionHighlight(index: Int, sentence: String? = null) {
        _questionHighlightIndex.value = if (index < 0) null else index
        _currentQuestionSentence.value = if (index < 0) null else sentence
    }

    fun setAnswerHighlight(index: Int, sentence: String? = null) {
        _answerHighlightIndex.value = if (index < 0) null else index
        _currentAnswerSentence.value = if (index < 0) null else sentence
    }

    fun setAnswerKoHighlight(index: Int, sentence: String? = null) {
        _answerKoHighlightIndex.value = if (index < 0) null else index
        _currentAnswerKoSentence.value = if (index < 0) null else sentence
    }

    fun setRecordingHighlight(index: Int) {
        _recordingHighlightIndex.value = if (index < 0) null else index
    }

    fun clearHighlight() {
        _questionHighlightIndex.value = null
        _answerHighlightIndex.value = null
        _answerKoHighlightIndex.value = null
        _recordingHighlightIndex.value = null
        _currentQuestionSentence.value = null
        _currentAnswerSentence.value = null
        _currentAnswerKoSentence.value = null
    }
}
