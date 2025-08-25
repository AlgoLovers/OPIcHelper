package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.QaDataRepository
import javax.inject.Inject
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * QA 아이템들을 로드하는 UseCase (Lazy Loading)
 */
@ViewModelScoped
class LoadQaItemsUseCase @Inject constructor(
    private val qaDataRepository: QaDataRepository
) {
    /**
     * 특정 카테고리의 QA 아이템들을 로드 (Lazy Loading)
     */
    suspend operator fun invoke(category: String): List<com.na982.opichelper.domain.entity.QaItem> {
        return qaDataRepository.loadCategoryData(category)
    }
} 