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
        Log.d("AppStateManager", "상태 업데이트: ${_state.value} -> $newState")
        _state.value = newState
    }
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction, newState: com.na982.opichelper.domain.entity.ButtonState) {
        Log.d("AppStateManager", "버튼 상태 업데이트: $buttonFunction -> $newState")
        updateState { currentState ->
            val updatedState = currentState.updateButtonState(buttonFunction, newState)
            Log.d("AppStateManager", "버튼 상태 업데이트 완료: $buttonFunction = $newState")
            updatedState
        }
    }
    
    /**
     * TTS 재생 상태 업데이트
     */
    fun updateTtsPlayingState(isQuestionPlaying: Boolean, isAnswerPlaying: Boolean) {
        updateState { currentState ->
            currentState.copy(
                isQuestionPlaying = isQuestionPlaying,
                isAnswerPlaying = isAnswerPlaying
            )
        }
    }
    
    /**
     * 하이라이트 상태 업데이트
     */
    fun updateHighlightState(
        questionHighlightIndex: Int? = null,
        answerHighlightIndex: Int? = null,
        answerKoHighlightIndex: Int? = null,
        recordingHighlightIndex: Int? = null
    ) {
        updateState { currentState ->
            currentState.copy(
                questionHighlightIndex = questionHighlightIndex ?: currentState.questionHighlightIndex,
                answerHighlightIndex = answerHighlightIndex ?: currentState.answerHighlightIndex,
                answerKoHighlightIndex = answerKoHighlightIndex ?: currentState.answerKoHighlightIndex,
                recordingHighlightIndex = recordingHighlightIndex ?: currentState.recordingHighlightIndex
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
} 