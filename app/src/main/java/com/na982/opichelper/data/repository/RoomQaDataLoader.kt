package com.na982.opichelper.data.repository

import com.na982.opichelper.data.local.QaItemDao
import com.na982.opichelper.data.local.QaItemEntityMapper
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataLoader

class RoomQaDataLoader(
    private val qaItemDao: QaItemDao,
    private val qaItemEntityMapper: QaItemEntityMapper
) : QaDataLoader {

    override suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> {
        return qaItemDao.getByCategoryAndLevelDirect(level.name).map { qaItemEntityMapper.toQaItem(it) }
    }
}
