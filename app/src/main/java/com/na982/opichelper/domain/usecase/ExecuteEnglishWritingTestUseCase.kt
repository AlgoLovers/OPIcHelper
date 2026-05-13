package com.na982.opichelper.domain.usecase

import android.util.Log
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import javax.inject.Inject

/**
 * 영작 테스트 실행 UseCase
 * 
 * 영작 테스트 실행 (부분암기 테스트)
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 비즈니스 로직 처리
 * - Repository 인터페이스에만 의존
 * - 단일 책임 원칙 준수
 */
@javax.inject.Singleton
class ExecuteEnglishWritingTestUseCase @Inject constructor(
    private val englishWritingTestRepository: EnglishWritingTestRepository
) {
    /**
     * 영작 테스트 실행
     * 
     * @param answerKo 한글 답변
     * @param answerEn 영문 답변
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param onCardFlip 카드 뒤집기 콜백
     * @param onKoreanHighlight 한글 하이라이트 콜백
     * @param onRecordingHighlight 녹음 하이라이트 콜백
     * @param onRecordingStateChange 녹음 상태 변경 콜백
     * @param onMergedFileCreated 병합 파일 생성 완료 콜백
     */
    suspend fun execute(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int,
        onCardFlip: (Boolean) -> Unit,
        onKoreanHighlight: (Int?) -> Unit,
        onRecordingHighlight: (Int?) -> Unit,
        onRecordingStateChange: (Boolean) -> Unit,
        onMergedFileCreated: () -> Unit
    ) {
        try {
            englishWritingTestRepository.executeEnglishWritingTest(
                answerKo = answerKo,
                answerEn = answerEn,
                category = category,
                scriptIndex = scriptIndex,
                onCardFlip = onCardFlip,
                onKoreanHighlight = onKoreanHighlight,
                onRecordingHighlight = onRecordingHighlight,
                onRecordingStateChange = onRecordingStateChange,
                onMergedFileCreated = onMergedFileCreated
            )
        } catch (e: Exception) {
            Log.e("ExecuteEnglishWritingTestUseCase", "영작 테스트 실행 중 오류", e)
            throw e
        }
    }
} 