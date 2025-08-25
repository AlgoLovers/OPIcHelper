package com.na982.opichelper.domain.button

import android.util.Log
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 버튼 상태 관리자
 * 책임: 각 버튼의 상태를 관리하고 관찰자들에게 알림
 */
@ViewModelScoped
class ButtonStateManager @Inject constructor() {
    
    // 각 버튼의 상태를 관리하는 StateFlow
    private val _questionPlayState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    private val _answerPlayState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    private val _memorizeTestState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    private val _recordingPlayState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    
    // 외부에서 읽기 전용으로 접근할 수 있는 StateFlow
    val questionPlayState: StateFlow<ButtonState> = _questionPlayState.asStateFlow()
    val answerPlayState: StateFlow<ButtonState> = _answerPlayState.asStateFlow()
    val memorizeTestState: StateFlow<ButtonState> = _memorizeTestState.asStateFlow()
    val recordingPlayState: StateFlow<ButtonState> = _recordingPlayState.asStateFlow()
    
    // 관찰자 리스트
    private val observers = mutableListOf<ButtonStateObserver>()
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonFunction: ButtonFunction, newState: ButtonState) {
        Log.d("ButtonStateManager", "버튼 상태 업데이트: $buttonFunction -> $newState")
        
        when (buttonFunction) {
            is ButtonFunction.QuestionPlay -> {
                _questionPlayState.value = newState
            }
            is ButtonFunction.AnswerPlay -> {
                _answerPlayState.value = newState
            }
            is ButtonFunction.MemorizeTest -> {
                _memorizeTestState.value = newState
            }
            is ButtonFunction.RecordingPlay -> {
                _recordingPlayState.value = newState
            }
            is ButtonFunction.Stop -> {
                // Stop 버튼은 모든 버튼을 Idle로 변경
                resetAllButtonStates()
                return
            }
        }
        
        // 관찰자들에게 상태 변경 알림
        notifyObservers(buttonFunction, newState)
    }
    
    /**
     * 모든 버튼 상태를 Idle로 초기화
     */
    fun resetAllButtonStates() {
        Log.d("ButtonStateManager", "모든 버튼 상태 초기화")
        
        _questionPlayState.value = ButtonState.Idle
        _answerPlayState.value = ButtonState.Idle
        _memorizeTestState.value = ButtonState.Idle
        _recordingPlayState.value = ButtonState.Idle
        
        // 모든 관찰자에게 초기화 알림
        observers.forEach { observer ->
            observer.onAllButtonsReset()
        }
    }
    
    /**
     * 관찰자 추가
     */
    fun addObserver(observer: ButtonStateObserver) {
        observers.add(observer)
        Log.d("ButtonStateManager", "관찰자 추가: ${observers.size}명")
    }
    
    /**
     * 관찰자 제거
     */
    fun removeObserver(observer: ButtonStateObserver) {
        observers.remove(observer)
        Log.d("ButtonStateManager", "관찰자 제거: ${observers.size}명")
    }
    
    /**
     * 관찰자들에게 상태 변경 알림
     */
    private fun notifyObservers(buttonFunction: ButtonFunction, newState: ButtonState) {
        observers.forEach { observer ->
            try {
                observer.onButtonStateChanged(buttonFunction, newState)
            } catch (e: Exception) {
                Log.e("ButtonStateManager", "관찰자 알림 실패", e)
            }
        }
    }
    
    /**
     * 특정 버튼의 현재 상태 가져오기
     */
    fun getButtonState(buttonFunction: ButtonFunction): ButtonState {
        return when (buttonFunction) {
            is ButtonFunction.QuestionPlay -> _questionPlayState.value
            is ButtonFunction.AnswerPlay -> _answerPlayState.value
            is ButtonFunction.MemorizeTest -> _memorizeTestState.value
            is ButtonFunction.RecordingPlay -> _recordingPlayState.value
            is ButtonFunction.Stop -> ButtonState.Idle
        }
    }
    
    /**
     * 모든 버튼이 Idle 상태인지 확인
     */
    fun areAllButtonsIdle(): Boolean {
        return _questionPlayState.value == ButtonState.Idle &&
                _answerPlayState.value == ButtonState.Idle &&
                _memorizeTestState.value == ButtonState.Idle &&
                _recordingPlayState.value == ButtonState.Idle
    }
} 