package com.na982.opichelper.domain.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * 암기 테스트 관리 인터페이스
 * 책임: 반복듣기, 영작테스트, 통암기 모드 관리
 */
interface IMemorizationManager {
    
    // 상태 노출
    val uiState: StateFlow<MemorizationUiState>
    
    // 암기 테스트 모드 시작
    fun startRepeatListening(category: String, scriptIndex: Int)
    fun startEnglishWritingTest(category: String, scriptIndex: Int)
    fun startFullMemorization(category: String, scriptIndex: Int)
    
    // 현재 모드 중지
    fun stopCurrentMode()
    
    // 통암기 전용 기능
    fun stopFullMemorizationRecording()
    fun playFullMemorizationRecording()
    fun deleteFullMemorizationRecording()
    
    // 영작테스트 완료 처리
    fun onEnglishWritingTestCompleted()
    
    // 상태 초기화
    fun resetState()
    fun clearError()
} 