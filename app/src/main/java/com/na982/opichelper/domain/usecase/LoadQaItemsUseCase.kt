package com.na982.opichelper.domain.usecase

import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import javax.inject.Inject

/**
 * QA 아이템들을 로드하는 UseCase (Lazy Loading)
 */
class LoadQaItemsUseCase @Inject constructor(
    private val leveledQaDataLoader: LeveledQaDataLoader,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * 특정 카테고리의 QA 아이템들을 로드 (Lazy Loading)
     */
    suspend operator fun invoke(category: String): List<com.na982.opichelper.domain.entity.QaItem> {
        val currentUserLevel = userPreferencesRepository.getUserLevel()
        return leveledQaDataLoader.loadQaItemsForCategory(currentUserLevel, category)
    }
} 