package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback

/**
 * 반복듣기 Repository 인터페이스
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 Repository 인터페이스 정의
 * - Data Layer에서 구현체 제공
 * - 의존성 역전 원칙 준수
 */
interface RepeatListeningRepository {
    /**
     * 반복듣기 실행
     */
    suspend fun executeRepeatListening(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int = 5
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