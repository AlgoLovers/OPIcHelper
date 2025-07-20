package com.na982.opichelper.domain.usecase

/**
 * 통암기(암기 레벨) 테스트용 UseCase
 * - answerText: 정답 텍스트 (ViewModel에서 주입)
 *
 * execute()는 실제 통암기 평가 로직만 담당
 */
class FullMemorizationUseCase(
    private val answerText: String
) : MemorizeTestUseCase {
    override suspend fun execute() {
        // 통암기 평가 로직 구현 (예: 사용자의 암기 입력/음성 등과 answerText 비교)
    }
} 