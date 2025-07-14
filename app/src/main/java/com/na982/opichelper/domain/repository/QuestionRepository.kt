package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.QuestionCategory
import kotlinx.coroutines.flow.Flow

interface QuestionRepository {
    suspend fun getRandomQaItem(category: QuestionCategory? = null): QaItem?
    suspend fun getQaItemsByCategory(category: QuestionCategory): Flow<List<QaItem>>
    suspend fun getFavoriteQaItems(): Flow<List<QaItem>>
    suspend fun toggleFavorite(qaItemId: String)
    suspend fun addQaItem(qaItem: QaItem): String
    suspend fun updateQaItem(qaItem: QaItem)
    suspend fun deleteQaItem(qaItemId: String)
} 