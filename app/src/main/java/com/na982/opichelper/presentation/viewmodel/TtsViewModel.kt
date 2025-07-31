package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.state.AppStateManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TTS 재생 관련 기능을 담당하는 ViewModel
 * 상태 관리는 AppStateManager에 위임
 */
class TtsViewModel @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val appStateManager: AppStateManager
) : ViewModel() {
    
    // AppStateManager에서 상태를 가져오므로 내부 상태 제거
    val isPlaying: StateFlow<Boolean> = appStateManager.state.map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val isQuestionPlaying: StateFlow<Boolean> = appStateManager.state.map { it.isQuestionPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val isAnswerPlaying: StateFlow<Boolean> = appStateManager.state.map { it.isAnswerPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val questionHighlightIndex: StateFlow<Int?> = appStateManager.state.map { it.questionHighlightIndex }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val answerHighlightIndex: StateFlow<Int?> = appStateManager.state.map { it.answerHighlightIndex }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val answerKoHighlightIndex: StateFlow<Int?> = appStateManager.state.map { it.answerKoHighlightIndex }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val recordingHighlightIndex: StateFlow<Int?> = appStateManager.state.map { it.recordingHighlightIndex }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val currentKoreanTtsService: StateFlow<String> = appStateManager.state.map { it.currentKoreanTtsService }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    init {
        setupStateObservers()
    }

    private fun setupStateObservers() {
        // AppStateManager가 단일 진실 소스이므로 별도 설정 불필요
        Log.d("TtsViewModel", "AppStateManager 기반 상태 관찰 설정 완료")
    }

    fun setTtsOrchestrator(orchestrator: TtsOrchestrator) {
        Log.d("TtsViewModel", "TTS 오케스트레이터 설정 완료")
    }
    
    fun bindTtsService(context: Context, onKoreanTtsServiceUpdate: ((String) -> Unit)? = null) {
        Log.d("TtsViewModel", "TTS 서비스 바인딩")
    }
    
    fun unbindTtsService(context: Context) {
        Log.d("TtsViewModel", "TTS 서비스 언바인딩")
    }

    fun playQuestion(question: String) {
        viewModelScope.launch {
            ttsOrchestrator.speak(question, null)
        }
    }

    fun stopQuestion() {
        viewModelScope.launch {
            ttsOrchestrator.stop()
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            ttsOrchestrator.speak(answer, null)
        }
    }

    fun stopAnswer() {
        viewModelScope.launch {
            ttsOrchestrator.stop()
        }
    }

    fun stopAllTts() {
        viewModelScope.launch {
            ttsOrchestrator.stop()
        }
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        appStateManager.updateKoreanTtsService(serviceName)
        Log.d("TtsViewModel", "한글 TTS 서비스 업데이트: $serviceName")
    }

    fun clearHighlight() {
        appStateManager.resetHighlightState()
    }

    fun setQuestionHighlightIndex(index: Int?) {
        appStateManager.updateHighlightState(questionHighlightIndex = index)
    }

    fun setAnswerHighlightIndex(index: Int?) {
        appStateManager.updateHighlightState(answerHighlightIndex = index)
    }

    fun setAnswerKoHighlightIndex(index: Int?) {
        appStateManager.updateHighlightState(answerKoHighlightIndex = index)
    }

    fun setRecordingHighlightIndex(index: Int?) {
        appStateManager.updateHighlightState(recordingHighlightIndex = index)
    }

    fun playAudioFile(file: java.io.File) {
        // 오디오 파일 재생은 별도 구현 필요
    }
} 