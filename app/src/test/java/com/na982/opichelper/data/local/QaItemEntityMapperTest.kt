package com.na982.opichelper.data.local

import com.google.gson.Gson
import com.na982.opichelper.domain.entity.UserLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Room Entity → Domain QaItem 매핑 검증. 레벨 문자열 파싱 폴백과, vocabulary/grammar/
 * tips의 JSON 파싱 에러 내성(손상된 JSON이 와도 빈 리스트로 안전 처리)이 핵심이다.
 * 편집 저장분에 깨진 JSON이 들어와도 앱이 크래시하지 않아야 한다.
 */
class QaItemEntityMapperTest {

    private val mapper = QaItemEntityMapper(Gson())

    private fun entity(
        level: String = "IH",
        vocabulary: String = "",
        grammar: String = "",
        tips: String = "",
        isModified: Boolean = false
    ) = QaItemEntity(
        id = "집_0_IH",
        category = "집",
        itemId = "item-1",
        level = level,
        questionEn = "Where do you live?",
        questionKo = "어디 사세요?",
        answerEn = "I live in Seoul.",
        answerKo = "서울에 살아요.",
        vocabulary = vocabulary,
        grammar = grammar,
        tips = tips,
        questionEnOriginal = "Where do you live?",
        questionKoOriginal = "어디 사세요?",
        answerEnOriginal = "I live in Seoul.",
        answerKoOriginal = "서울에 살아요.",
        isModified = isModified
    )

    @Test
    fun `엔티티 필드가 QaItem으로 매핑된다`() {
        val item = mapper.toQaItem(entity())

        assertEquals("item-1", item.id) // id는 itemId에서
        assertEquals("집", item.category)
        assertEquals("Where do you live?", item.questionEn)
        assertEquals("어디 사세요?", item.questionKo)
        val answer = item.answers[UserLevel.IH]!!
        assertEquals("I live in Seoul.", answer.answerEn)
        assertEquals("서울에 살아요.", answer.answerKo)
    }

    @Test
    fun `유효한 레벨 문자열은 해당 UserLevel로 매핑`() {
        val item = mapper.toQaItem(entity(level = "AL"))
        assertEquals(setOf(UserLevel.AL), item.answers.keys)
    }

    @Test
    fun `알 수 없는 레벨 문자열은 IH로 폴백`() {
        val item = mapper.toQaItem(entity(level = "ZZZ"))
        assertEquals(setOf(UserLevel.IH), item.answers.keys)
    }

    @Test
    fun `vocabulary grammar tips JSON 배열이 리스트로 파싱된다`() {
        val item = mapper.toQaItem(
            entity(
                vocabulary = """["live", "reside"]""",
                grammar = """["present tense"]""",
                tips = """["speak slowly"]"""
            )
        )
        val answer = item.answers[UserLevel.IH]!!
        assertEquals(listOf("live", "reside"), answer.vocabulary)
        assertEquals(listOf("present tense"), answer.grammar)
        assertEquals(listOf("speak slowly"), answer.tips)
    }

    @Test
    fun `빈 문자열 필드는 빈 리스트`() {
        val answer = mapper.toQaItem(entity(vocabulary = "", grammar = "", tips = "")).answers[UserLevel.IH]!!
        assertEquals(emptyList<String>(), answer.vocabulary)
        assertEquals(emptyList<String>(), answer.grammar)
        assertEquals(emptyList<String>(), answer.tips)
    }

    @Test
    fun `손상된 JSON은 크래시 없이 빈 리스트로 처리`() {
        val answer = mapper.toQaItem(entity(vocabulary = "이건 JSON이 아님")).answers[UserLevel.IH]!!
        assertEquals(emptyList<String>(), answer.vocabulary)
    }

    @Test
    fun `isModified 플래그가 전달된다`() {
        assertEquals(true, mapper.toQaItem(entity(isModified = true)).isModified)
        assertEquals(false, mapper.toQaItem(entity(isModified = false)).isModified)
    }
}
