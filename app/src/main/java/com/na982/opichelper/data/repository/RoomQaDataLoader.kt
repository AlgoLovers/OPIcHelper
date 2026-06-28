package com.na982.opichelper.data.repository

import com.na982.opichelper.data.local.QaItemDao
import com.na982.opichelper.data.local.toQaItem
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataLoader

class RoomQaDataLoader(
    private val qaItemDao: QaItemDao
) : QaDataLoader {

    override suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> {
        return qaItemDao.getByCategoryAndLevelDirect(level.name).map { it.toQaItem() }
    }
}
