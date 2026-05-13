package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import javax.inject.Inject

/**
 * 반복듣기 실행 UseCase
 * 
 * 반복듣기 실행
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 비즈니스 로직 처리
 * - Repository 인터페이스에만 의존
 * - 단일 책임 원칙 준수
 */
@javax.inject.Singleton
class ExecuteRepeatListeningUseCase @Inject constructor(
    private val repeatListeningRepository: RepeatListeningRepository
) {
    /**
     * 반복듣기 실행
     * 
     * @param data 반복듣기 데이터
     * @param uiCallback UI 콜백 인터페이스
     * @param repeatCount 반복 횟수 (기본 5회)
     */
    suspend fun execute(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int = 5
    ) {
        try {
            repeatListeningRepository.executeRepeatListening(
                data = data,
                uiCallback = uiCallback,
                repeatCount = repeatCount
            )
        } catch (e: Exception) {
            Log.e("ExecuteRepeatListeningUseCase", "반복듣기 실행 중 오류", e)
            throw e
        }
    }
} 