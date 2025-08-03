package com.na982.opichelper.data.state

import android.util.Log
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.state.AppState
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱의 전체 상태를 관리하는 클래스
 * 단일 책임: 상태 관리만 담당
 */
@Singleton
class AppStateManagerImpl @Inject constructor() : AppStateManager {
    
    private val _state = MutableStateFlow(AppState())
    override val state: StateFlow<AppState> = _state.asStateFlow()
    
    /**
     * 상태 업데이트
     */
    private fun updateState(update: (AppState) -> AppState) {
        val newState = update(_state.value)
        _state.value = newState
    }
    
    // ===== 읽기 메서드들 =====
    
    override val currentQaItem: QaItem?
        get() = _state.value.currentQaItem
    
    override val currentCategory: String?
        get() = _state.value.currentCategory
    
    override val currentIndex: Int
        get() = _state.value.currentIndex
    
    override val currentSentenceIndex: Int
        get() = _state.value.currentIndex // 현재는 currentIndex와 동일
    
    // ===== 쓰기 메서드들 =====
    
    override fun updateButtonState(buttonFunction: ButtonFunction, newState: ButtonState) {
        Log.d("AppStateManager", "버튼 상태 업데이트: $buttonFunction -> $newState")
        updateState { currentState ->
            val updatedButtonStates = currentState.buttonStates.toMutableMap()
            updatedButtonStates[buttonFunction] = newState
            currentState.copy(buttonStates = updatedButtonStates)
        }
    }
    
    override fun updateTtsPlayingState(
        isQuestionPlaying: Boolean?,
        isAnswerPlaying: Boolean?,
        isPlaying: Boolean?
    ) {
        updateState { currentState ->
            currentState.copy(
                isQuestionPlaying = isQuestionPlaying ?: currentState.isQuestionPlaying,
                isAnswerPlaying = isAnswerPlaying ?: currentState.isAnswerPlaying,
                isPlaying = isPlaying ?: currentState.isPlaying
            )
        }
    }
    
    override fun updateHighlightState(
        questionHighlightIndex: Int,
        answerHighlightIndex: Int,
        answerKoHighlightIndex: Int,
        recordingHighlightIndex: Int
    ) {
        Log.d("AppStateManagerImpl", "하이라이트 상태 업데이트: question=$questionHighlightIndex, answer=$answerHighlightIndex, answerKo=$answerKoHighlightIndex, recording=$recordingHighlightIndex")
        
        updateState { currentState ->
            val newState = currentState.copy(
                questionHighlightIndex = questionHighlightIndex,
                answerHighlightIndex = answerHighlightIndex,
                answerKoHighlightIndex = answerKoHighlightIndex,
                recordingHighlightIndex = recordingHighlightIndex
            )
            Log.d("AppStateManagerImpl", "하이라이트 상태 업데이트 완료: ${newState.questionHighlightIndex}, ${newState.answerHighlightIndex}")
            newState
        }
    }
    
    override fun updateCardState(
        isQuestionCardFlipped: Boolean?,
        isAnswerCardFlipped: Boolean?
    ) {
        updateState { currentState ->
            currentState.copy(
                isQuestionCardFlipped = isQuestionCardFlipped ?: currentState.isQuestionCardFlipped,
                isAnswerCardFlipped = isAnswerCardFlipped ?: currentState.isAnswerCardFlipped
            )
        }
    }
    
    override fun updateQaItemState(
        qaItem: QaItem?,
        category: String?,
        index: Int?,
        totalCount: Int?
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
    
    override fun updateSelectedMemorizeLevel(level: String) {
        updateState { currentState ->
            currentState.copy(selectedMemorizeLevel = level)
        }
    }
    
    override fun updateTtsServiceState(service: String) {
        updateState { currentState ->
            currentState.copy(currentKoreanTtsService = service)
        }
    }
    
    override fun updateRecordingState(isRecording: Boolean) {
        updateState { currentState ->
            currentState.copy(isRecording = isRecording)
        }
    }
    
    override fun updateMergedFileCreated(created: Boolean) {
        updateState { currentState ->
            currentState.copy(mergedFileCreated = created)
        }
    }
    
    override fun updateLoadingState(isLoading: Boolean) {
        updateState { currentState ->
            currentState.copy(isLoading = isLoading)
        }
    }
    
    override fun updateErrorState(error: String?) {
        updateState { currentState ->
            currentState.copy(error = error)
        }
    }
    
    // ===== 추가 유틸리티 메서드들 =====
    
    /**
     * 모든 상태 초기화
     */
    fun resetAllState() {
        updateState { AppState() }
    }
    
    /**
     * TTS 관련 상태만 초기화
     */
    override fun resetTtsState() {
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