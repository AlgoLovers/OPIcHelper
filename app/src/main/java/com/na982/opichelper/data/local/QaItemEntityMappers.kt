package com.na982.opichelper.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import javax.inject.Inject

class QaItemEntityMapper @Inject constructor(private val gson: Gson) {
    private val stringListType = object : TypeToken<List<String>>() {}.type

    fun toQaItem(entity: QaItemEntity): QaItem {
        val userLevel = UserLevel.entries.find { it.name == entity.level } ?: UserLevel.IH
        return QaItem(
            id = entity.itemId,
            category = entity.category,
            questionEn = entity.questionEn,
            questionKo = entity.questionKo,
            answers = mapOf(
                userLevel to LeveledAnswer(
                    answerEn = entity.answerEn,
                    answerKo = entity.answerKo,
                    vocabulary = tryParseList(entity.vocabulary),
                    grammar = tryParseList(entity.grammar),
                    tips = tryParseList(entity.tips)
                )
            ),
            isModified = entity.isModified
        )
    }

    private fun tryParseList(json: String?): List<String> {
        return try {
            json?.let { gson.fromJson<List<String>>(it, stringListType) ?: emptyList() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
