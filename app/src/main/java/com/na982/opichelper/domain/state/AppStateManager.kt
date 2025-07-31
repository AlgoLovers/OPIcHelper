package com.na982.opichelper.domain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱의 전체 상태를 관리하는 클래스
 * 단일 책임: 상태 관리만 담당
 */
@Singleton
class AppStateManager @Inject constructor() {
    
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    /**
     * 상태 업데이트
     */
    fun updateState(update: (AppState) -> AppState) {
        val newState = update(_state.value)
        _state.value = newState
    }
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction, newState: com.na982.opichelper.domain.entity.ButtonState) {
        Log.d("AppStateManager", "버튼 상태 업데이트: $buttonFunction -> $newState")
        updateState { currentState ->
            val updatedButtonStates = currentState.buttonStates.toMutableMap()
            updatedButtonStates[buttonFunction] = newState
            currentState.copy(buttonStates = updatedButtonStates)
        }
    }
    
    /**
     * TTS 재생 상태 업데이트 (통합)
     */
    fun updateTtsPlayingState(
        isQuestionPlaying: Boolean? = null,
        isAnswerPlaying: Boolean? = null,
        isPlaying: Boolean? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                isQuestionPlaying = isQuestionPlaying ?: currentState.isQuestionPlaying,
                isAnswerPlaying = isAnswerPlaying ?: currentState.isAnswerPlaying,
                isPlaying = isPlaying ?: currentState.isPlaying
            )
        }
    }
    
    /**
     * 하이라이트 상태 업데이트 (통합)
     */
    fun updateHighlightState(
        questionHighlightIndex: Int = -1,
        answerHighlightIndex: Int = -1,
        answerKoHighlightIndex: Int = -1,
        recordingHighlightIndex: Int = -1
    ) {
        updateState { currentState ->
            // -1이 명시적으로 전달되면 -1로 설정, 그렇지 않으면 기존 값 유지
            val newQuestionHighlightIndex = questionHighlightIndex
            val newAnswerHighlightIndex = answerHighlightIndex
            val newAnswerKoHighlightIndex = answerKoHighlightIndex
            val newRecordingHighlightIndex = recordingHighlightIndex
            
            currentState.copy(
                questionHighlightIndex = newQuestionHighlightIndex,
                answerHighlightIndex = newAnswerHighlightIndex,
                answerKoHighlightIndex = newAnswerKoHighlightIndex,
                recordingHighlightIndex = newRecordingHighlightIndex
            )
        }
    }
    
    /**
     * 카드 상태 업데이트
     */
    fun updateCardState(
        isQuestionCardFlipped: Boolean? = null,
        isAnswerCardFlipped: Boolean? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                isQuestionCardFlipped = isQuestionCardFlipped ?: currentState.isQuestionCardFlipped,
                isAnswerCardFlipped = isAnswerCardFlipped ?: currentState.isAnswerCardFlipped
            )
        }
    }
    
    /**
     * 암기 모드 상태 업데이트
     */
    fun updateMemorizationModeState(
        isRepeatListeningMode: Boolean? = null,
        isEnglishWritingTestMode: Boolean? = null,
        isFullMemorizationMode: Boolean? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                isRepeatListeningMode = isRepeatListeningMode ?: currentState.isRepeatListeningMode,
                isEnglishWritingTestMode = isEnglishWritingTestMode ?: currentState.isEnglishWritingTestMode,
                isFullMemorizationMode = isFullMemorizationMode ?: currentState.isFullMemorizationMode
            )
        }
    }
    
    /**
     * 현재 QA 아이템 업데이트
     */
    fun updateCurrentQaItem(
        qaItem: com.na982.opichelper.domain.entity.QaItem?,
        category: String? = null,
        index: Int? = null,
        totalCount: Int? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                currentQaItem = qaItem,
                currentCategory = category ?: currentState.currentCategory,
                currentIndex = index ?: currentState.currentIndex,
                totalCount = totalCount ?: currentState.totalCount
            )
        }
    }
    
    /**
     * 선택된 암기 레벨 업데이트
     */
    fun updateSelectedMemorizeLevel(level: String) {
        updateState { currentState ->
            currentState.copy(selectedMemorizeLevel = level)
        }
    }
    
    /**
     * TTS 서비스 상태 업데이트
     */
    fun updateKoreanTtsService(serviceName: String) {
        updateState { currentState ->
            currentState.copy(currentKoreanTtsService = serviceName)
        }
    }
    
    /**
     * 암기 테스트 진행 상태 업데이트
     */
    fun updateMemorizeTestState(
        isRunning: Boolean? = null,
        currentMode: String? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                isMemorizeTestRunning = isRunning ?: currentState.isMemorizeTestRunning,
                currentMemorizeMode = currentMode ?: currentState.currentMemorizeMode
            )
        }
    }
    
    /**
     * 영작 테스트 상태 업데이트
     */
    fun updateEnglishWritingTestState(
        completed: Boolean? = null,
        stopMergedFilePlaying: Boolean? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                englishWritingTestCompleted = completed ?: currentState.englishWritingTestCompleted,
                stopEnglishWritingTestMergedFilePlaying = stopMergedFilePlaying ?: currentState.stopEnglishWritingTestMergedFilePlaying
            )
        }
    }
    
    /**
     * 녹음 상태 업데이트
     */
    fun updateRecordingState(isRecording: Boolean) {
        updateState { currentState ->
            currentState.copy(isRecording = isRecording)
        }
    }
    
    /**
     * 병합 파일 생성 완료 상태 업데이트
     */
    fun updateMergedFileCreated(created: Boolean) {
        updateState { currentState ->
            currentState.copy(mergedFileCreated = created)
        }
    }
    
    /**
     * 로딩 상태 업데이트
     */
    fun updateLoadingState(isLoading: Boolean) {
        updateState { currentState ->
            currentState.copy(isLoading = isLoading)
        }
    }
    
    /**
     * 에러 상태 업데이트
     */
    fun updateErrorState(error: String?) {
        updateState { currentState ->
            currentState.copy(error = error)
        }
    }
    
    /**
     * 모든 상태 초기화
     */
    fun resetAllState() {
        updateState { AppState() }
    }
    
    /**
     * TTS 관련 상태만 초기화
     */
    fun resetTtsState() {
        updateTtsPlayingState(isQuestionPlaying = false, isAnswerPlaying = false, isPlaying = false)
        updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
    }
    
    /**
     * 하이라이트 상태만 초기화
     */
    fun resetHighlightState() {
        updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
    }
} 