package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaDataManager
import javax.inject.Inject

class SearchQaItemsUseCase @Inject constructor(
    private val qaDataManager: QaDataManager
) {
    fun search(query: String): List<QaItem> {
        if (query.length < 2) return emptyList()
        return qaDataManager.searchItems(query)
    }
}
