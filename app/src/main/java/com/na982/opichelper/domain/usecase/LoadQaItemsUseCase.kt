package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.QaDataLoader
import javax.inject.Inject

/**
 * QA 아이템들을 로드하는 UseCase
 */
class LoadQaItemsUseCase @Inject constructor(
    private val qaDataLoader: QaDataLoader
) {
    suspend operator fun invoke(): Map<String, List<com.na982.opichelper.domain.entity.QaItem>> {
        val data = qaDataLoader.loadQaItemsFromAssets()
        return data
    }
} 