package com.na982.opichelper.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TTS 재생 관련 기능을 담당하는 ViewModel
 * 상태 관리는 AppStateManager에 위임
 * TTS 제어는 TtsController 인터페이스를 통해 수행
 */
class TtsViewModel @Inject constructor(
    private val ttsController: TtsController,
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

    fun setTtsOrchestrator(orchestrator: TtsController) {
        Log.d("TtsViewModel", "TTS 컨트롤러 설정 완료")
    }
    
    fun bindTtsService(context: Context, onKoreanTtsServiceUpdate: ((String) -> Unit)? = null) {
        Log.d("TtsViewModel", "TTS 서비스 바인딩")
    }
    
    fun unbindTtsService(context: Context) {
        Log.d("TtsViewModel", "TTS 서비스 언바인딩")
    }

    fun playQuestion(question: String) {
        viewModelScope.launch {
            ttsController.playQuestion(question)
        }
    }

    fun stopQuestion() {
        viewModelScope.launch {
            ttsController.stopTts()
        }
    }

    fun playAnswer(answer: String) {
        viewModelScope.launch {
            ttsController.playAnswer(answer)
        }
    }

    fun stopAnswer() {
        viewModelScope.launch {
            ttsController.stopTts()
        }
    }

    fun stopAllTts() {
        viewModelScope.launch {
            ttsController.stopAllTts()
        }
    }

    fun updateKoreanTtsServiceName(serviceName: String) {
        appStateManager.updateKoreanTtsService(serviceName)
        Log.d("TtsViewModel", "한글 TTS 서비스 업데이트: $serviceName")
    }

    fun clearHighlight() {
        // 하이라이트는 TtsControllerImpl에서만 처리
    }

    fun setQuestionHighlightIndex(index: Int?) {
        // 하이라이트는 TtsControllerImpl에서만 처리
    }

    fun setAnswerHighlightIndex(index: Int?) {
        // 하이라이트는 TtsControllerImpl에서만 처리
    }

    fun setAnswerKoHighlightIndex(index: Int?) {
        // 하이라이트는 TtsControllerImpl에서만 처리
    }

    fun setRecordingHighlightIndex(index: Int?) {
        // 하이라이트는 TtsControllerImpl에서만 처리
    }

    fun playAudioFile(file: java.io.File) {
        // 오디오 파일 재생은 별도 구현 필요
    }
} 