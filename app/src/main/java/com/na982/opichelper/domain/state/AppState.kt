package com.na982.opichelper.domain.state

import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState

/**
 * 앱의 전체 상태를 관리하는 데이터 클래스
 * 모든 UI 상태를 중앙에서 관리
 * 순수한 데이터 클래스로 유지 (헬퍼 메서드 제거)
 */
data class AppState(
    // 현재 QA 아이템
    val currentQaItem: com.na982.opichelper.domain.entity.QaItem? = null,
    val currentCategory: String? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    
    // 선택된 암기 레벨
    val selectedMemorizeLevel: String = "",
    
    // 버튼 상태들
    val buttonStates: Map<ButtonFunction, ButtonState> = mapOf(
        ButtonFunction.QuestionPlay to ButtonState.Idle,
        ButtonFunction.AnswerPlay to ButtonState.Idle,
        ButtonFunction.MemorizeTest to ButtonState.Idle,
        ButtonFunction.RecordingPlay to ButtonState.Idle
    ),
    
    // TTS 재생 상태 (통합)
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val isPlaying: Boolean = false,
    
    // 하이라이트 상태 (통합) - -1은 하이라이트 없음을 의미
    val questionHighlightIndex: Int = -1,
    val answerHighlightIndex: Int = -1,
    val answerKoHighlightIndex: Int = -1,
    val recordingHighlightIndex: Int = -1,
    
    // 카드 상태
    val isQuestionCardFlipped: Boolean = false,
    val isAnswerCardFlipped: Boolean = false,
    
    // 암기 모드 상태
    val isRepeatListeningMode: Boolean = false,
    val isEnglishWritingTestMode: Boolean = false,
    val isFullMemorizationMode: Boolean = false,
    
    // TTS 서비스 상태
    val currentKoreanTtsService: String = "",
    
    // 암기 테스트 진행 상태
    val isMemorizeTestRunning: Boolean = false,
    val currentMemorizeMode: String = "NONE",
    
    // 영작 테스트 상태
    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false,
    
    // 녹음 상태
    val isRecording: Boolean = false,
    
    // 병합 파일 생성 상태
    val mergedFileCreated: Boolean = false,
    
    // 로딩 및 에러 상태
    val isLoading: Boolean = false,
    val error: String? = null
) 