package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TTS 재생 관련 기능을 담당하는 ViewModel
 */
class TtsViewModel @Inject constructor(
    private val ttsPlaybackController: TtsPlaybackController
) : ViewModel() {
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()
    
    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()
    
    private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
    val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex.asStateFlow()
    
    private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
    val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex.asStateFlow()
    
    private val _answerKoHighlightIndex = MutableStateFlow<Int?>(null)
    val answerKoHighlightIndex: StateFlow<Int?> = _answerKoHighlightIndex.asStateFlow()
    
    private val _recordingHighlightIndex = MutableStateFlow<Int?>(null)
    val recordingHighlightIndex: StateFlow<Int?> = _recordingHighlightIndex.asStateFlow()
    
    private val _currentKoreanTtsService = MutableStateFlow("")
    val currentKoreanTtsService: StateFlow<String> = _currentKoreanTtsService.asStateFlow()

    init {
        setupStateObservers()
    }

    private fun setupStateObservers() {
        viewModelScope.launch {
            ttsPlaybackController.isPlaying.collect { playing ->
                _isPlaying.value = playing
            }
        }
        
        viewModelScope.launch {
            ttsPlaybackController.isQuestionPlaying.collect { playing ->
                _isQuestionPlaying.value = playing
            }
        }
        
        viewModelScope.launch {
            ttsPlaybackController.isAnswerPlaying.collect { playing ->
                _isAnswerPlaying.value = playing
            }
        }
        
        viewModelScope.launch {
            ttsPlaybackController.questionHighlightIndex.collect { idx ->
                _questionHighlightIndex.value = idx
            }
        }
        
        viewModelScope.launch {
            ttsPlaybackController.answerHighlightIndex.collect { idx ->
                _answerHighlightIndex.value = idx
            }
        }
        
        viewModelScope.launch {
            ttsPlaybackController.answerKoHighlightIndex.collect { idx ->
                _answerKoHighlightIndex.value = idx
            }
        }
        
        viewModelScope.launch {
            ttsPlaybackController.recordingHighlightIndex.collect { idx ->
                _recordingHighlightIndex.value = idx
            }
        }
    }

    fun setTtsOrchestrator(orchestrator: TtsOrchestrator) {
        ttsPlaybackController.setTtsOrchestrator(orchestrator)
        Log.d("TtsViewModel", "TTS 오케스트레이터 설정 완료")
    }
    
    fun bindTtsService(context: Context, onKoreanTtsServiceUpdate: ((String) -> Unit)? = null) {
        ttsPlaybackController.bindTtsService(context, onKoreanTtsServiceUpdate)
    }
    
    fun unbindTtsService(context: Context) {
        ttsPlaybackController.unbindTtsService(context)
    }

    fun playQuestion(question: String) {
        ttsPlaybackController.playQuestion(question)
    }

    fun stopQuestion() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
        }
    }

    fun playAnswer(answer: String) {
        ttsPlaybackController.playAnswer(answer)
    }

    fun stopAnswer() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
        }
    }

    fun stopAllTts() {
        viewModelScope.launch {
            ttsPlaybackController.stopTts()
        }
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        _currentKoreanTtsService.value = serviceName
        Log.d("TtsViewModel", "한글 TTS 서비스 업데이트: $serviceName")
    }

    fun clearHighlight() {
        ttsPlaybackController.clearHighlight()
    }

    fun setQuestionHighlightIndex(index: Int?) {
        index?.let { ttsPlaybackController.setQuestionHighlightIndex(it) }
    }

    fun setAnswerHighlightIndex(index: Int?) {
        index?.let { ttsPlaybackController.setAnswerHighlightIndex(it) }
    }

    fun setAnswerKoHighlightIndex(index: Int?) {
        index?.let { ttsPlaybackController.setAnswerKoHighlightIndex(it) }
    }

    fun setRecordingHighlightIndex(index: Int?) {
        index?.let { ttsPlaybackController.setRecordingHighlightIndex(it) }
    }

    fun playAudioFile(file: java.io.File) {
        ttsPlaybackController.playAudioFile(file)
    }
} 