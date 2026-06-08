package com.na982.opichelper.data.repository

import android.content.Context
import com.na982.opichelper.domain.manager.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.repository.QaDataLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeveledQaDataLoader(
    private val context: Context,
    private val appLogger: AppLogger,
    private val gson: Gson
) : QaDataLoader {

    private val categoryAssetType = object : TypeToken<QaCategoryAsset>() {}.type

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
                        val categoryAsset: QaCategoryAsset = gson.fromJson(jsonString, categoryAssetType) ?: return@forEach

                        val items = categoryAsset.items.map { asset ->
                            val leveledAnswer = LeveledAnswer(
                                answerEn = asset.answer_en,
                                answerKo = asset.answer_ko
                            )

                            QaItem(
                                id = asset.id ?: "",
                                category = categoryAsset.title,
                                questionEn = asset.question_en,
                                questionKo = asset.question_ko,
                                answers = mapOf(level to leveledAnswer)
                            )
                        }

                        allQaItems.addAll(items)
                    } catch (e: Exception) {
                        appLogger.e("LeveledQaDataLoader", "JSON 파싱 실패: $fileName", e)
                    }
                }
            }

            allQaItems
        } catch (e: Exception) {
            appLogger.e("LeveledQaDataLoader", "레벨 데이터 로딩 실패", e)
            emptyList()
        }
    }

    private data class QaCategoryAsset(
        val title: String,
        val items: List<QaItemAsset>
    )

    private data class QaItemAsset(
        val id: String?,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )
}
