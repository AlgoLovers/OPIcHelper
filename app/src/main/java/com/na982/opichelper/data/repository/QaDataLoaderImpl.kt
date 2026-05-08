package com.na982.opichelper.data.repository

import android.content.Context
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataLoader
import javax.inject.Inject

class QaDataLoaderImpl @Inject constructor(
    private val context: Context
) : QaDataLoader {

    private val levelFolderMapping = mapOf(
        UserLevel.AL to "al",
        UserLevel.IH to "ih",
        UserLevel.IH_RAW to "ih_raw",
        UserLevel.IM to "im"
    )

    // Fallback: delegates to LeveledQaDataLoader
    private val delegate = LeveledQaDataLoader(context)

    override suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> {
        return delegate.loadQaItemsForLevel(level)
    }
}
