package com.na982.opichelper.domain.repository

interface QaNavigator {
    suspend fun selectCategory(category: String)
    fun hasNextQaItem(): Boolean
    suspend fun nextQaItem()
    suspend fun previousQaItem()
    suspend fun navigateToIndex(index: Int)
}
