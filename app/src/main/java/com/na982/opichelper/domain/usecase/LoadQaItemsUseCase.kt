package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.Result
import com.na982.opichelper.domain.repository.QuestionRepository
import javax.inject.Inject

/**
 * QA 아이템들을 로드하는 UseCase
 */
class LoadQaItemsUseCase @Inject constructor(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(): Result<Map<String, List<QaItem>>> {
        return try {
            val data = questionRepository.loadQaItemsFromAssets()
            Result.Success(data)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
} 