package com.na982.opichelper.domain.audio

import android.util.Log
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 버튼 상태를 중앙에서 관리하는 클래스
 * AppStateManager를 통해 상태 관리 (단일 진실 소스)
 */
@Singleton
class ButtonStateManager @Inject constructor(
    private val appStateManager: AppStateManager
) {
    
    // AppStateManager에서 상태를 가져오므로 내부 상태 제거
    val questionButtonState: StateFlow<ButtonState> = appStateManager.state.map { 
        it.buttonStates[ButtonFunction.QuestionPlay] ?: ButtonState.Idle 
    }.stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), ButtonState.Idle)
    
    val answerButtonState: StateFlow<ButtonState> = appStateManager.state.map { 
        it.buttonStates[ButtonFunction.AnswerPlay] ?: ButtonState.Idle 
    }.stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), ButtonState.Idle)
    
    val memorizeTestButtonState: StateFlow<ButtonState> = appStateManager.state.map { 
        it.buttonStates[ButtonFunction.MemorizeTest] ?: ButtonState.Idle 
    }.stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), ButtonState.Idle)
    
    val recordingPlayButtonState: StateFlow<ButtonState> = appStateManager.state.map { 
        it.buttonStates[ButtonFunction.RecordingPlay] ?: ButtonState.Idle 
    }.stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), ButtonState.Idle)
    
    // 현재 활성화된 암기 레벨
    val currentMemorizeLevel: StateFlow<MemorizeLevel> = appStateManager.state.map { 
        when (it.selectedMemorizeLevel) {
            "반복듣기" -> MemorizeLevel.REPEAT_LISTENING
            "영작테스트" -> MemorizeLevel.ENGLISH_WRITING
            "통암기" -> MemorizeLevel.FULL_MEMORIZATION
            else -> MemorizeLevel.REPEAT_LISTENING
        }
    }.stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), MemorizeLevel.REPEAT_LISTENING)
    
    // 녹음 파일 존재 여부 (AppState에 추가 필요)
    val hasRecordingFile: StateFlow<Boolean> = appStateManager.state.map { 
        false // TODO: AppState에 hasRecordingFile 추가
    }.stateIn(CoroutineScope(Dispatchers.Main), SharingStarted.WhileSubscribed(), false)
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonType: ButtonFunction, state: ButtonState) {
        Log.d("ButtonStateManager", "버튼 상태 업데이트: $buttonType -> $state")
        appStateManager.updateButtonState(buttonType, state)
    }
    
    /**
     * 모든 버튼 상태를 Idle로 초기화
     */
    fun resetAllButtonStates() {
        Log.d("ButtonStateManager", "모든 버튼 상태 초기화")
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)
    }
    
    /**
     * 특정 버튼 상태 확인
     */
    fun isButtonPlaying(buttonType: ButtonFunction): Boolean {
        return appStateManager.state.value.buttonStates[buttonType] == ButtonState.Playing
    }
    
    /**
     * 모든 버튼이 Idle 상태인지 확인
     */
    fun areAllButtonsIdle(): Boolean {
        val buttonStates = appStateManager.state.value.buttonStates
        return buttonStates.values.all { it == ButtonState.Idle }
    }
} 