package com.na982.opichelper.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel

private val mappingGson = Gson()
private val stringListType = object : TypeToken<List<String>>() {}.type

fun QaItemEntity.toQaItem(): QaItem {
    val userLevel = UserLevel.entries.find { it.name == level } ?: UserLevel.IH
    return QaItem(
        id = itemId,
        category = category,
        questionEn = questionEn,
        questionKo = questionKo,
        answers = mapOf(
            userLevel to LeveledAnswer(
                answerEn = answerEn,
                answerKo = answerKo,
                vocabulary = try { mappingGson.fromJson<List<String>>(vocabulary, stringListType) ?: emptyList() } catch (_: Exception) { emptyList() },
                grammar = try { mappingGson.fromJson<List<String>>(grammar, stringListType) ?: emptyList() } catch (_: Exception) { emptyList() },
                tips = try { mappingGson.fromJson<List<String>>(tips, stringListType) ?: emptyList() } catch (_: Exception) { emptyList() }
            )
        )
    )
}
