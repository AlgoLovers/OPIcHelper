package com.na982.opichelper.data.local

import com.google.gson.Gson
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.AppDataPreferences

class AssetSeeder(
    private val qaDataLoader: QaDataLoader,
    private val dao: QaItemDao,
    private val appDataPreferences: AppDataPreferences,
    private val gson: Gson
) : com.na982.opichelper.domain.repository.DataSeeder {

    companion object {
        const val CURRENT_SEED_VERSION = 1
    }

    override suspend fun seedIfNeeded() {
        val storedVersion = appDataPreferences.getSeedVersion()
        if (storedVersion == CURRENT_SEED_VERSION && dao.getCount() > 0) return

        val entities = mutableListOf<QaItemEntity>()

        for (level in UserLevel.entries) {
            val items = qaDataLoader.loadQaItemsForLevel(level)
            items.forEachIndexed { index, item ->
                val answer = item.answers[level] ?: return@forEachIndexed
                val safeItemId = item.id.ifBlank { index.toString() }
                entities.add(QaItemEntity(
                    id = "${item.category}_${safeItemId}_${level.name}",
                    category = item.category,
                    itemId = safeItemId,
                    level = level.name,
                    questionEn = item.questionEn,
                    questionKo = item.questionKo,
                    answerEn = answer.answerEn,
                    answerKo = answer.answerKo,
                    vocabulary = gson.toJson(answer.vocabulary),
                    grammar = gson.toJson(answer.grammar),
                    tips = gson.toJson(answer.tips),
                    questionEnOriginal = item.questionEn,
                    questionKoOriginal = item.questionKo,
                    answerEnOriginal = answer.answerEn,
                    answerKoOriginal = answer.answerKo
                ))
            }
        }
        dao.insertAll(entities)
        appDataPreferences.setSeedVersion(CURRENT_SEED_VERSION)
    }
}
