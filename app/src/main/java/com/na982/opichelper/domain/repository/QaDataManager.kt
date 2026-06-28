package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.StateFlow

interface QaDataManager {
    val currentQaItem: StateFlow<QaItem?>
    val currentCategory: StateFlow<String?>
    val categories: StateFlow<List<String>>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    suspend fun init()
    suspend fun reload()
    suspend fun selectCategory(category: String)
    fun hasNextQaItem(): Boolean
    suspend fun nextQaItem()
    suspend fun previousQaItem()
    fun getCurrentIndex(): Int
    fun getCurrentCategory(): String?
    fun getCurrentQaItem(): QaItem?
    fun getCurrentAnswer(qaItem: QaItem?): String
    fun getCurrentAnswerKo(qaItem: QaItem?): String
    fun getItemsInCategory(category: String): List<QaItem>
    fun searchItems(query: String): List<QaItem>
    suspend fun navigateToIndex(index: Int)
    fun clearError()
    fun release()
}
