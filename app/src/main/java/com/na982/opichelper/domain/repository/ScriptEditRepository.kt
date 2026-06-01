package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import kotlinx.coroutines.flow.Flow

interface ScriptEditRepository {
    fun getQaItemsByCategory(category: String, level: String): Flow<List<QaItem>>
    suspend fun updateQaItem(item: QaItem, level: UserLevel, scriptIndex: Int)
    suspend fun restoreOriginal(id: String)
    suspend fun restoreAllOriginal()
    suspend fun isModified(id: String): Boolean
}
