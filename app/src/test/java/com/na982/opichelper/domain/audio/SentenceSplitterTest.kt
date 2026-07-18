package com.na982.opichelper.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SentenceSplitter는 TTS 하이라이트/문장별 녹음의 기준이 되는 순수 로직이다.
 * 분할 규칙이 바뀌면 문장 인덱스가 어긋나 재생·녹음이 밀리므로 회귀를 막는다.
 */
class SentenceSplitterTest {

    @Test
    fun `빈 문자열은 빈 리스트`() {
        assertEquals(emptyList<String>(), SentenceSplitter.split(""))
    }

    @Test
    fun `공백만 있으면 빈 리스트`() {
        assertEquals(emptyList<String>(), SentenceSplitter.split("   "))
    }

    @Test
    fun `구두점 없는 단일 문장은 그대로 한 개`() {
        assertEquals(listOf("No punctuation"), SentenceSplitter.split("No punctuation"))
    }

    @Test
    fun `마침표로 끝나는 단일 문장`() {
        assertEquals(listOf("Hello."), SentenceSplitter.split("Hello."))
    }

    @Test
    fun `마침표 뒤 공백으로 두 문장 분할`() {
        assertEquals(listOf("Hello.", "World."), SentenceSplitter.split("Hello. World."))
    }

    @Test
    fun `물음표 느낌표 마침표 혼합`() {
        assertEquals(
            listOf("One!", "Two?", "Three."),
            SentenceSplitter.split("One! Two? Three.")
        )
    }

    @Test
    fun `한글 마침표도 분할 기준`() {
        assertEquals(
            listOf("안녕하세요。", "반갑습니다。"),
            SentenceSplitter.split("안녕하세요。 반갑습니다。")
        )
    }

    @Test
    fun `분할 결과에 빈 문자열이 남지 않는다`() {
        val result = SentenceSplitter.split("A.  B.   C.")
        assertTrue(result.none { it.isBlank() })
        assertEquals(3, result.size)
    }

    @Test
    fun `join은 공백으로 연결`() {
        assertEquals("a b c", SentenceSplitter.join(listOf("a", "b", "c")))
    }

    @Test
    fun `join 단일 요소`() {
        assertEquals("only", SentenceSplitter.join(listOf("only")))
    }
}
