package com.na982.opichelper.domain.repository

/**
 * 통암기 Repository 인터페이스
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 Repository 인터페이스 정의
 * - Data Layer에서 구현
 * - 순수한 데이터 처리만 담당
 */
interface FullMemorizationRepository {
    
    /**
     * 질문을 TTS로 재생하고 하이라이트 처리
     */
    suspend fun playQuestionWithHighlight(
        onHighlight: (Int?) -> Unit
    )
    
    /**
     * 녹음 시작
     * @return 녹음 파일 경로
     */
    suspend fun startRecording(category: String, scriptIndex: Int): String
    
    /**
     * 녹음 종료
     */
    suspend fun stopRecording()
    
    /**
     * 녹음 파일 재생
     */
    suspend fun playRecording(
        onHighlight: (Int?) -> Unit
    )
    
    /**
     * 녹음 파일 존재 여부 확인
     */
    fun hasRecording(): Boolean
    
    /**
     * 현재 녹음 파일 경로 반환
     */
    fun getRecordingPath(): String?
    
    /**
     * 녹음 파일 정보 초기화
     */
    fun clearRecording()
} 