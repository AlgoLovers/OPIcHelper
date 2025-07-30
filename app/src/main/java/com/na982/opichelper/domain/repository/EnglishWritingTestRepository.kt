package com.na982.opichelper.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 영작 테스트 Repository 인터페이스
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 Repository 인터페이스 정의
 * - Data Layer에서 구현체 제공
 * - 의존성 역전 원칙 준수
 */
interface EnglishWritingTestRepository {
    /**
     * 영작 테스트 실행
     */
    suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int,
        onCardFlip: (Boolean) -> Unit,
        onKoreanHighlight: (Int?) -> Unit,
        onRecordingHighlight: (Int?) -> Unit,
        onRecordingStateChange: (Boolean) -> Unit,
        onMergedFileCreated: () -> Unit
    )
    
    /**
     * 현재 진행 상황 가져오기
     */
    suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData?
    
    /**
     * 진행 상황 업데이트
     */
    suspend fun updateProgress(progressData: ProgressData)
    
    /**
     * 진행 상황 초기화
     */
    suspend fun clearProgress(category: String, scriptIndex: Int)
}

/**
 * 진행 상황 데이터 클래스
 */
data class ProgressData(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean
) 