package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaSearch
import javax.inject.Inject

class SearchQaItemsUseCase @Inject constructor(
    private val qaSearch: QaSearch
) {
    fun search(query: String): List<QaItem> {
        if (query.length < 2) return emptyList()
        return qaSearch.searchItems(query)
    }
}
