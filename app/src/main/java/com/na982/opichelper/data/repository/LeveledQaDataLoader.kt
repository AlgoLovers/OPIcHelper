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

                        // Gson은 리플렉션으로 객체를 만들어 Kotlin의 non-null 타입을 강제하지 못한다.
                        // JSON에 필드가 빠지면 null이 그대로 주입되므로, 여기서 방어하지 않으면
                        // 항목 하나의 필드 누락이 NPE로 번져 카테고리(파일) 전체가 목록에서 사라진다.
                        // → 문제 항목만 건너뛰고 나머지는 정상 로드한다.
                        val title = categoryAsset.title
                        if (title.isNullOrBlank() || categoryAsset.items == null) {
                            appLogger.w("LeveledQaDataLoader", "카테고리 정보 누락으로 건너뜀: $fileName")
                            return@forEach
                        }

                        val items = categoryAsset.items.mapNotNull { asset ->
                            val questionEn = asset.question_en
                            val questionKo = asset.question_ko
                            val answerEn = asset.answer_en
                            val answerKo = asset.answer_ko
                            if (questionEn == null || questionKo == null || answerEn == null || answerKo == null) {
                                appLogger.w("LeveledQaDataLoader", "필수 필드 누락 항목 건너뜀: $fileName (id=${asset.id})")
                                return@mapNotNull null
                            }

                            QaItem(
                                id = asset.id ?: "",
                                category = title,
                                questionEn = questionEn,
                                questionKo = questionKo,
                                answers = mapOf(level to LeveledAnswer(answerEn = answerEn, answerKo = answerKo))
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

    // Gson이 JSON에서 채우는 원본 타입. 필드 누락 시 null 주입을 허용하기 위해 전부 nullable로 둔다.
    // 실제 non-null 보장은 loadQaItemsForLevel의 방어 파싱에서 수행한다.
    private data class QaCategoryAsset(
        val title: String?,
        val items: List<QaItemAsset>?
    )

    private data class QaItemAsset(
        val id: String?,
        val question_en: String?,
        val question_ko: String?,
        val answer_en: String?,
        val answer_ko: String?
    )
}
