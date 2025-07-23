package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.Result
import com.na982.opichelper.domain.repository.QuestionRepository
import javax.inject.Inject

/**
 * 카테고리 목록을 가져오는 UseCase
 */
class GetCategoriesUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return try {
            val data = questionRepository.getAllCategories()
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
} 