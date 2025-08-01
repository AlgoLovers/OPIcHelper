package com.na982.opichelper.presentation.viewmodel

import android.util.Log
import javax.inject.Singleton
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.event.ButtonEvent
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.state.AppStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 오디오 제어 전담 클래스
 * 책임: TTS 재생, 오디오 중지, 버튼 상태 관리
 */
@Singleton
class AudioControlManager @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val buttonEventHandler: ButtonEventHandler,
    private val appStateManager: AppStateManager
) {
    
    // 오디오 제어 관련 상태
    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()
    
    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        Log.d("AudioControlManager", "오디오 제어 매니저 초기화")
    }
    
    /**
     * 질문 재생
     */
    fun playQuestion(qaItem: QaItem) {
        Log.d("AudioControlManager", "질문 재생 시작")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                _error.value = null
                
                // 버튼 상태를 Loading으로 변경
                appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Loading)
                
                // TTS 재생
                ttsOrchestrator.speak(qaItem.questionEn) { }
                
                // 버튼 상태를 Playing으로 변경
                appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
                
                Log.d("AudioControlManager", "질문 재생 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "질문 재생 실패", e)
                _error.value = e.message
                appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
            }
        }
    }
    
    /**
     * 답변 재생
     */
    fun playAnswer(qaItem: QaItem) {
        Log.d("AudioControlManager", "답변 재생 시작")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                _error.value = null
                
                // 버튼 상태를 Loading으로 변경
                appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Loading)
                
                // TTS 재생
                ttsOrchestrator.speak(qaItem.answers.values.first().answerEn) { }
                
                // 버튼 상태를 Playing으로 변경
                appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)
                
                Log.d("AudioControlManager", "답변 재생 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "답변 재생 실패", e)
                _error.value = e.message
                appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
            }
        }
    }
    
    /**
     * 모든 오디오 중지
     */
    fun stopAllAudio() {
        Log.d("AudioControlManager", "모든 오디오 중지")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // TTS 중지
                ttsOrchestrator.stop()
                
                // 모든 버튼 상태를 Idle로 변경
                val stopEvents = listOf(
                    ButtonEvent.StopClick(ButtonFunction.QuestionPlay),
                    ButtonEvent.StopClick(ButtonFunction.AnswerPlay),
                    ButtonEvent.StopClick(ButtonFunction.MemorizeTest),
                    ButtonEvent.StopClick(ButtonFunction.RecordingPlay)
                )
                
                for (event in stopEvents) {
                    buttonEventHandler.handleEvent(event)
                }
                
                Log.d("AudioControlManager", "모든 오디오 중지 완료")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "오디오 중지 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 특정 버튼의 오디오 중지
     */
    fun stopButtonAudio(buttonFunction: ButtonFunction) {
        Log.d("AudioControlManager", "버튼 오디오 중지: $buttonFunction")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val event = ButtonEvent.StopClick(buttonFunction)
                buttonEventHandler.handleEvent(event)
                
                Log.d("AudioControlManager", "버튼 오디오 중지 완료: $buttonFunction")
                
            } catch (e: Exception) {
                Log.e("AudioControlManager", "버튼 오디오 중지 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 버튼 클릭 이벤트 처리
     */
    fun handleButtonClick(buttonFunction: ButtonFunction, qaItem: QaItem? = null) {
        Log.d("AudioControlManager", "버튼 클릭 처리: $buttonFunction")
        
        when (buttonFunction) {
            ButtonFunction.QuestionPlay -> {
                qaItem?.let { playQuestion(it) }
            }
            ButtonFunction.AnswerPlay -> {
                qaItem?.let { playAnswer(it) }
            }
            ButtonFunction.Stop -> {
                stopAllAudio()
            }
            else -> {
                // 다른 버튼들은 ButtonEventHandler에서 처리
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    try {
                        // ButtonClick 이벤트가 없으므로 StopClick으로 처리
                        val event = ButtonEvent.StopClick(buttonFunction)
                        buttonEventHandler.handleEvent(event)
                    } catch (e: Exception) {
                        Log.e("AudioControlManager", "버튼 이벤트 처리 실패", e)
                        _error.value = e.message
                    }
                }
            }
        }
    }
    
    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 재생 상태 업데이트
     */
    fun updatePlayingState(isQuestionPlaying: Boolean, isAnswerPlaying: Boolean, isPlaying: Boolean) {
        _isQuestionPlaying.value = isQuestionPlaying
        _isAnswerPlaying.value = isAnswerPlaying
        _isPlaying.value = isPlaying
    }
} 