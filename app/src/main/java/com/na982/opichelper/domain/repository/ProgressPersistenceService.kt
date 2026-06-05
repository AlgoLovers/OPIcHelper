package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.CategoryProgress

interface ProgressPersistenceService {
    data class NavigationState(val category: String?, val scriptIndex: Int = -1, val sentenceIndex: Int = 0)

    suspend fun saveNavigationState(state: NavigationState)
    suspend fun loadNavigationState(): NavigationState
    suspend fun saveCategoryProgress(progress: CategoryProgress)
    suspend fun loadCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String): CategoryProgress?
    suspend fun loadAllCategoryProgress(): Map<String, CategoryProgress>
    suspend fun clearCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String)
    suspend fun clearAllProgress()
}
