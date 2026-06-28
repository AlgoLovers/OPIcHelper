package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem

interface QaSearch {
    fun searchItems(query: String): List<QaItem>
}
