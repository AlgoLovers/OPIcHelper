package com.na982.opichelper.domain.audio

import com.na982.opichelper.domain.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 버튼 상태를 중앙에서 관리하는 클래스
 * 단일 책임 원칙에 따라 버튼 상태만을 관리
 */
@Singleton
class ButtonStateManager @Inject constructor() {
    
    // 각 버튼의 상태를 개별적으로 관리
    private val _questionButtonState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    val questionButtonState: StateFlow<ButtonState> = _questionButtonState.asStateFlow()
    
    private val _answerButtonState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    val answerButtonState: StateFlow<ButtonState> = _answerButtonState.asStateFlow()
    
    private val _memorizeTestButtonState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    val memorizeTestButtonState: StateFlow<ButtonState> = _memorizeTestButtonState.asStateFlow()
    
    private val _recordingPlayButtonState = MutableStateFlow<ButtonState>(ButtonState.Idle)
    val recordingPlayButtonState: StateFlow<ButtonState> = _recordingPlayButtonState.asStateFlow()
    
    // 현재 활성화된 암기 레벨
    private val _currentMemorizeLevel = MutableStateFlow(MemorizeLevel.REPEAT_LISTENING)
    val currentMemorizeLevel: StateFlow<MemorizeLevel> = _currentMemorizeLevel.asStateFlow()
    
    // 녹음 파일 존재 여부
    private val _hasRecordingFile = MutableStateFlow(false)
    val hasRecordingFile: StateFlow<Boolean> = _hasRecordingFile.asStateFlow()
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonType: ButtonFunction, state: ButtonState) {
        when (buttonType) {
            is ButtonFunction.QuestionPlay -> {
                _questionButtonState.value = state
                Log.d("ButtonStateManager", "질문 버튼 상태 변경: $state")
            }
            is ButtonFunction.AnswerPlay -> {
                _answerButtonState.value = state
                Log.d("ButtonStateManager", "답변 버튼 상태 변경: $state")
            }
            is ButtonFunction.MemorizeTest -> {
                _memorizeTestButtonState.value = state
                Log.d("ButtonStateManager", "암기 테스트 버튼 상태 변경: $state")
            }
            is ButtonFunction.RecordingPlay -> {
                _recordingPlayButtonState.value = state
                Log.d("ButtonStateManager", "녹음 재생 버튼 상태 변경: $state")
            }
            is ButtonFunction.Stop -> {
                // 모든 버튼을 Idle로 초기화
                resetAllButtons()
            }
        }
    }
    
    /**
     * 암기 레벨 변경
     */
    fun setMemorizeLevel(level: MemorizeLevel) {
        Log.d("ButtonStateManager", "암기 레벨 변경: ${_currentMemorizeLevel.value} -> $level")
        _currentMemorizeLevel.value = level
        resetAllButtons()
    }
    
    /**
     * 녹음 파일 존재 여부 업데이트
     */
    fun setHasRecordingFile(hasFile: Boolean) {
        _hasRecordingFile.value = hasFile
        Log.d("ButtonStateManager", "녹음 파일 존재 여부 변경: $hasFile")
    }
    
    /**
     * 모든 버튼 상태 초기화
     */
    fun resetAllButtons() {
        _questionButtonState.value = ButtonState.Idle
        _answerButtonState.value = ButtonState.Idle
        _memorizeTestButtonState.value = ButtonState.Idle
        _recordingPlayButtonState.value = ButtonState.Idle
        Log.d("ButtonStateManager", "모든 버튼 상태 초기화")
    }
    
    /**
     * 특정 버튼의 설정을 반환
     */
    fun getButtonConfig(buttonType: ButtonFunction): ButtonConfig {
        return when (buttonType) {
            is ButtonFunction.QuestionPlay -> {
                val text = when (_questionButtonState.value) {
                    is ButtonState.Playing -> "질문 재생 중"
                    is ButtonState.Loading -> "로딩 중..."
                    is ButtonState.Error -> "오류"
                    else -> "질문 재생"
                }
                ButtonConfig(
                    function = buttonType,
                    state = _questionButtonState.value,
                    text = text,
                    isEnabled = _questionButtonState.value !is ButtonState.Loading
                )
            }
            is ButtonFunction.AnswerPlay -> {
                val text = when (_answerButtonState.value) {
                    is ButtonState.Playing -> "답변 재생 중"
                    is ButtonState.Loading -> "로딩 중..."
                    is ButtonState.Error -> "오류"
                    else -> "답변 1회 재생"
                }
                ButtonConfig(
                    function = buttonType,
                    state = _answerButtonState.value,
                    text = text,
                    isEnabled = _answerButtonState.value !is ButtonState.Loading
                )
            }
            is ButtonFunction.MemorizeTest -> {
                val text = when {
                    _memorizeTestButtonState.value is ButtonState.Playing -> "${getMemorizeLevelText()} 종료"
                    _memorizeTestButtonState.value is ButtonState.Loading -> "로딩 중..."
                    _memorizeTestButtonState.value is ButtonState.Error -> "오류"
                    else -> getMemorizeLevelText()
                }
                ButtonConfig(
                    function = buttonType,
                    state = _memorizeTestButtonState.value,
                    text = text,
                    isEnabled = _memorizeTestButtonState.value !is ButtonState.Loading
                )
            }
            is ButtonFunction.RecordingPlay -> {
                val text = when (_recordingPlayButtonState.value) {
                    is ButtonState.Playing -> "재생 중..."
                    is ButtonState.Loading -> "로딩 중..."
                    is ButtonState.Error -> "오류"
                    else -> getRecordingPlayText()
                }
                ButtonConfig(
                    function = buttonType,
                    state = _recordingPlayButtonState.value,
                    text = text,
                    isEnabled = _recordingPlayButtonState.value !is ButtonState.Loading,
                    isVisible = _hasRecordingFile.value
                )
            }
            is ButtonFunction.Stop -> {
                ButtonConfig(
                    function = buttonType,
                    state = ButtonState.Idle,
                    text = "중지",
                    isEnabled = false
                )
            }
        }
    }
    
    /**
     * 현재 암기 레벨에 따른 텍스트 반환
     */
    private fun getMemorizeLevelText(): String {
        return when (_currentMemorizeLevel.value) {
            MemorizeLevel.REPEAT_LISTENING -> "반복듣기"
            MemorizeLevel.ENGLISH_WRITING -> "부분암기 테스트"
            MemorizeLevel.FULL_MEMORIZATION -> "통암기"
        }
    }
    
    /**
     * 현재 암기 레벨에 따른 녹음 재생 버튼 텍스트 반환
     */
    private fun getRecordingPlayText(): String {
        return when (_currentMemorizeLevel.value) {
            MemorizeLevel.REPEAT_LISTENING -> "녹음 재생" // 실제로는 표시되지 않음
            MemorizeLevel.ENGLISH_WRITING -> "영작테스트 녹음 재생"
            MemorizeLevel.FULL_MEMORIZATION -> "통암기 녹음 재생"
        }
    }
} 