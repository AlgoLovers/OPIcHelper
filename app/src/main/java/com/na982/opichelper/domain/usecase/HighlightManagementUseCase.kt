package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.state.AppStateManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하이라이트 상태 관리를 위한 UseCase
 * 클린 아키텍처 원칙: Domain Layer에서 하이라이트 상태 관리
 */
@Singleton
class HighlightManagementUseCase @Inject constructor(
    private val appStateManager: AppStateManager
) {
    
    /**
     * TTS 하이라이트 설정
     */
    fun setTtsHighlight(
        questionHighlightIndex: Int? = null,
        answerHighlightIndex: Int? = null,
        answerKoHighlightIndex: Int? = null
    ) {
        Log.d("HighlightManagementUseCase", "TTS 하이라이트 설정: q=$questionHighlightIndex, a=$answerHighlightIndex, ak=$answerKoHighlightIndex")
        appStateManager.updateHighlightState(
            questionHighlightIndex = questionHighlightIndex,
            answerHighlightIndex = answerHighlightIndex,
            answerKoHighlightIndex = answerKoHighlightIndex
        )
    }
    
    /**
     * 녹음 하이라이트 설정
     */
    fun setRecordingHighlight(recordingHighlightIndex: Int?) {
        Log.d("HighlightManagementUseCase", "녹음 하이라이트 설정: $recordingHighlightIndex")
        appStateManager.updateHighlightState(
            recordingHighlightIndex = recordingHighlightIndex
        )
    }
    
    /**
     * 모든 하이라이트 초기화
     */
    fun clearAllHighlights() {
        Log.d("HighlightManagementUseCase", "모든 하이라이트 초기화")
        appStateManager.updateHighlightState(
            questionHighlightIndex = null,
            answerHighlightIndex = null,
            answerKoHighlightIndex = null,
            recordingHighlightIndex = null
        )
    }
    
    /**
     * TTS 하이라이트만 초기화
     */
    fun clearTtsHighlights() {
        Log.d("HighlightManagementUseCase", "TTS 하이라이트 초기화")
        appStateManager.updateHighlightState(
            questionHighlightIndex = null,
            answerHighlightIndex = null,
            answerKoHighlightIndex = null
        )
    }
    
    /**
     * 녹음 하이라이트만 초기화
     */
    fun clearRecordingHighlight() {
        Log.d("HighlightManagementUseCase", "녹음 하이라이트 초기화")
        appStateManager.updateHighlightState(
            recordingHighlightIndex = null
        )
    }
} 