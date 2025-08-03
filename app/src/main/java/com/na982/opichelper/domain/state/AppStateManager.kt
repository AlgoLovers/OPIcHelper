package com.na982.opichelper.domain.state

import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 상태 관리를 위한 통합 인터페이스
 * 읽기와 쓰기 기능을 모두 제공
 */
interface AppStateManager {
    // ===== 읽기 메서드들 =====
    
    /**
     * 현재 QA 아이템 가져오기
     */
    val currentQaItem: QaItem?
    
    /**
     * 현재 카테고리 가져오기
     */
    val currentCategory: String?
    
    /**
     * 현재 인덱스 가져오기
     */
    val currentIndex: Int
    
    /**
     * 현재 문장 인덱스 가져오기
     */
    val currentSentenceIndex: Int
    
    /**
     * 앱 상태 StateFlow
     */
    val state: StateFlow<AppState>
    
    // ===== 쓰기 메서드들 =====
    
    /**
     * 카드 상태 업데이트
     */
    fun updateCardState(
        isQuestionCardFlipped: Boolean? = null,
        isAnswerCardFlipped: Boolean? = null
    )
    
    /**
     * 하이라이트 상태 업데이트
     */
    fun updateHighlightState(
        questionHighlightIndex: Int = -1,
        answerHighlightIndex: Int = -1,
        answerKoHighlightIndex: Int = -1,
        recordingHighlightIndex: Int = -1
    )
    
    /**
     * 녹음 상태 업데이트
     */
    fun updateRecordingState(isRecording: Boolean)
    
    /**
     * 병합 파일 생성 완료 상태 업데이트
     */
    fun updateMergedFileCreated(created: Boolean)
    
    /**
     * 버튼 상태 업데이트
     */
    fun updateButtonState(buttonFunction: ButtonFunction, newState: ButtonState)
    
    /**
     * TTS 재생 상태 업데이트
     */
    fun updateTtsPlayingState(
        isQuestionPlaying: Boolean? = null,
        isAnswerPlaying: Boolean? = null,
        isPlaying: Boolean? = null
    )
    
    /**
     * 로딩 상태 업데이트
     */
    fun updateLoadingState(isLoading: Boolean)
    
    /**
     * 에러 상태 업데이트
     */
    fun updateErrorState(error: String?)
    
    /**
     * QA 아이템 상태 업데이트
     */
    fun updateQaItemState(
        qaItem: QaItem? = null,
        category: String? = null,
        index: Int? = null,
        totalCount: Int? = null
    )
    
    /**
     * TTS 서비스 상태 업데이트
     */
    fun updateTtsServiceState(service: String)
    
    /**
     * 선택된 암기 레벨 업데이트
     */
    fun updateSelectedMemorizeLevel(level: String)
    
    /**
     * TTS 관련 상태만 초기화
     */
    fun resetTtsState()
} 