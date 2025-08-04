package com.na982.opichelper.domain.event

import android.util.Log
import com.na982.opichelper.domain.state.AppStateManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하이라이트 이벤트 핸들러
 * 하이라이트 관련 이벤트를 처리하여 앱 상태를 업데이트합니다.
 */
@Singleton
class HighlightEventHandler @Inject constructor(
    private val appStateManager: AppStateManager
) {
    
    /**
     * 하이라이트 이벤트 처리
     */
    fun handle(event: HighlightEvent) {
        Log.d("HighlightEventHandler", "하이라이트 이벤트 처리: $event")
        
        when (event) {
            is HighlightEvent.UpdateHighlight -> {
                handleUpdateHighlight(event)
            }
            is HighlightEvent.ClearHighlight -> {
                handleClearHighlight()
            }
            is HighlightEvent.ClearAllHighlights -> {
                handleClearAllHighlights()
            }
        }
    }
    
    /**
     * 하이라이트 업데이트 처리
     */
    private fun handleUpdateHighlight(event: HighlightEvent.UpdateHighlight) {
        when (event.type) {
            com.na982.opichelper.domain.audio.HighlightType.QUESTION -> {
                appStateManager.updateHighlightState(
                    questionHighlightIndex = event.index,
                    answerHighlightIndex = -1,
                    answerKoHighlightIndex = -1,
                    recordingHighlightIndex = -1
                )
            }
            com.na982.opichelper.domain.audio.HighlightType.ANSWER -> {
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = event.index,
                    answerKoHighlightIndex = event.index,
                    recordingHighlightIndex = -1
                )
            }
            com.na982.opichelper.domain.audio.HighlightType.ENGLISH_WRITING_RECORDING,
            com.na982.opichelper.domain.audio.HighlightType.FULL_MEMORIZATION_RECORDING -> {
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = -1,
                    answerKoHighlightIndex = -1,
                    recordingHighlightIndex = event.index
                )
            }
            else -> {
                // 기타 타입들은 기본적으로 recordingHighlightIndex 사용
                appStateManager.updateHighlightState(
                    questionHighlightIndex = -1,
                    answerHighlightIndex = -1,
                    answerKoHighlightIndex = -1,
                    recordingHighlightIndex = event.index
                )
            }
        }
        
        Log.d("HighlightEventHandler", "하이라이트 업데이트 완료: 타입=${event.type}, 인덱스=${event.index}")
    }
    
    /**
     * 하이라이트 제거 처리
     */
    private fun handleClearHighlight() {
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        Log.d("HighlightEventHandler", "하이라이트 제거 완료")
    }
    
    /**
     * 모든 하이라이트 제거 처리
     */
    private fun handleClearAllHighlights() {
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        Log.d("HighlightEventHandler", "모든 하이라이트 제거 완료")
    }
} 