package com.na982.opichelper.domain.usecase

/**
 * 영작 테스트(암기 레벨) 테스트용 UseCase
 * - answerText: 정답 텍스트 (ViewModel에서 주입)
 *
 * execute()는 실제 영작 평가 로직만 담당
 */
class EnglishWritingTestUseCase(
    private val answerText: String
) : MemorizeTestUseCase {
    override suspend fun execute() {
        // 영작 테스트 로직 구현 (예: 입력값과 answerText 비교)
    }
} 