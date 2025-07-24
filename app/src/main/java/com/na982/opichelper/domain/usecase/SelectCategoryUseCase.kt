package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.QaDataLoader
import javax.inject.Inject

/**
 * 카테고리 선택을 담당하는 UseCase
 */
class SelectCategoryUseCase @Inject constructor(
    private val qaDataLoader: QaDataLoader
) {
    suspend operator fun invoke(category: String): List<com.na982.opichelper.domain.entity.QaItem> {
        val data = qaDataLoader.getQaItemsByCategory(category)
        return data
    }
} 