package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel

interface QaDataLoader {
    suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem>
}
