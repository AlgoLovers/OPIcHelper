package com.na982.opichelper.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.repository.QaDataLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeveledQaDataLoader(private val context: Context) : QaDataLoader {

    private val gson = Gson()

    private val levelFolderMapping = mapOf(
        UserLevel.AL to "al",
        UserLevel.IH to "ih",
        UserLevel.IH_RAW to "ih_raw",
        UserLevel.IM to "im"
    )

    override suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> = withContext(Dispatchers.IO) {
        try {
            val folderName = levelFolderMapping[level] ?: "ih"
            val allQaItems = mutableListOf<QaItem>()

            context.assets.list(folderName)?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        val jsonString = context.assets.open("$folderName/$fileName").bufferedReader().use { it.readText() }
                        val type = object : TypeToken<List<QaItemAsset>>() {}.type
                        val assetItems: List<QaItemAsset> = gson.fromJson(jsonString, type) ?: emptyList()

                        val items = assetItems.map { asset ->
                            val leveledAnswer = LeveledAnswer(
                                answerEn = asset.answer_en,
                                answerKo = asset.answer_ko
                            )

                            QaItem(
                                id = asset.id ?: "",
                                category = asset.title,
                                questionEn = asset.question_en,
                                questionKo = asset.question_ko,
                                answers = mapOf(level to leveledAnswer)
                            )
                        }

                        allQaItems.addAll(items)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            allQaItems
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private data class QaItemAsset(
        val title: String,
        val id: String?,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )

    suspend fun loadAllLevelData(): Map<UserLevel, List<QaItem>> = withContext(Dispatchers.IO) {
        UserLevel.values().associateWith { level ->
            loadQaItemsForLevel(level)
        }
    }
}
