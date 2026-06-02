package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel

interface ScriptEditRepository {
    suspend fun updateQaItem(item: QaItem, level: UserLevel, scriptIndex: Int)
    suspend fun restoreOriginal(id: String)
}
