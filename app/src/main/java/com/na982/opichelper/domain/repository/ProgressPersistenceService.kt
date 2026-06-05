package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.ScriptProgress

interface ProgressPersistenceService {
    data class NavigationState(val category: String?, val scriptIndex: Int = -1, val sentenceIndex: Int = 0)

    suspend fun saveNavigationState(state: NavigationState)
    suspend fun loadNavigationState(): NavigationState
    suspend fun saveCategoryProgress(progress: ScriptProgress)
    suspend fun loadCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String): ScriptProgress?
    suspend fun loadAllCategoryProgress(): Map<String, ScriptProgress>
    suspend fun clearCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String)
    suspend fun clearAllProgress()
}
