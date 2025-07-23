package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.Result
import com.na982.opichelper.domain.repository.QuestionRepository
import javax.inject.Inject

/**
 * 카테고리 선택을 담당하는 UseCase
 */
class SelectCategoryUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(category: String): Result<List<QaItem>> {
        return try {
            val data = questionRepository.getQaItemsByCategory(category)
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
} 