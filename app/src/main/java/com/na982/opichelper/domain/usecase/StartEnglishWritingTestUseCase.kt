package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.audio.EnglishWritingUiCallback
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 영작 테스트 시작 UseCase
 * 
 * 영작 테스트 실행 (부분암기 테스트)
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 비즈니스 로직 처리
 * - Repository 인터페이스에만 의존
 * - 단일 책임 원칙 준수
 */
@Singleton
class StartEnglishWritingTestUseCase @Inject constructor(
    private val englishWritingTestRepository: EnglishWritingTestRepository
) {
    /**
     * 영작 테스트 시작
     * 
     * @param answerKo 한글 답변
     * @param answerEn 영문 답변
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param uiCallback UI 콜백 인터페이스
     */
    suspend fun execute(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int,
        uiCallback: EnglishWritingUiCallback
    ) {
        Log.d("StartEnglishWritingTestUseCase", "영작 테스트 시작")
        
        try {
            englishWritingTestRepository.executeEnglishWritingTest(
                answerKo = answerKo,
                answerEn = answerEn,
                category = category,
                scriptIndex = scriptIndex,
                uiCallback = uiCallback
            )
        } catch (e: Exception) {
            Log.e("StartEnglishWritingTestUseCase", "영작 테스트 실행 중 오류", e)
            throw e
        }
    }
} 