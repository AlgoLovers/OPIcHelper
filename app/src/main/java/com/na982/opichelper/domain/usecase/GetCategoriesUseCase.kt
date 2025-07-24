package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.QaDataLoader
import javax.inject.Inject

/**
 * 카테고리 목록을 가져오는 UseCase
 */
class GetCategoriesUseCase @Inject constructor(
    private val qaDataLoader: QaDataLoader
) {
    suspend operator fun invoke(): List<String> {
        val data = qaDataLoader.getAllCategories()
        return data
    }
} 