package com.na982.opichelper.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ValidateScriptEditUseCase는 스크립트 편집 저장 가능 여부를 결정하는 순수 도메인 로직이다.
 * 검증 규칙(빈 문장 금지, 문장 끝 구두점 필수)이 바뀌면 편집 저장 UX가 직접 영향을 받으므로 회귀를 막는다.
 */
class ValidateScriptEditUseCaseTest {

    private val useCase = ValidateScriptEditUseCase()

    @Test
    fun `한영 모두 구두점으로 끝나면 유효`() {
        val result = useCase.validate(listOf(SentencePair("안녕하세요.", "Hello.")))
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `빈 리스트는 유효`() {
        val result = useCase.validate(emptyList())
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `한국어가 비면 EmptySentence 오류`() {
        val result = useCase.validate(listOf(SentencePair("", "Hello.")))
        assertFalse(result.isValid)
        assertTrue(result.errors.contains(ValidationError.EmptySentence(0, isKorean = true)))
    }

    @Test
    fun `영어가 비면 EmptySentence 오류`() {
        val result = useCase.validate(listOf(SentencePair("안녕하세요.", "")))
        assertFalse(result.isValid)
        assertTrue(result.errors.contains(ValidationError.EmptySentence(0, isKorean = false)))
    }

    @Test
    fun `공백만 있는 문장은 빈 문장으로 처리되고 구두점 오류는 내지 않는다`() {
        val result = useCase.validate(listOf(SentencePair("   ", "Hello.")))
        assertFalse(result.isValid)
        assertTrue(result.errors.contains(ValidationError.EmptySentence(0, isKorean = true)))
        assertFalse(result.errors.any { it is ValidationError.MissingPunctuation && it.isKorean })
    }

    @Test
    fun `구두점 없이 끝나면 MissingPunctuation 오류`() {
        val result = useCase.validate(listOf(SentencePair("안녕하세요", "Hello")))
        assertFalse(result.isValid)
        assertTrue(result.errors.contains(ValidationError.MissingPunctuation(0, isKorean = true)))
        assertTrue(result.errors.contains(ValidationError.MissingPunctuation(0, isKorean = false)))
    }

    @Test
    fun `느낌표와 물음표도 문장 끝으로 인정`() {
        val result = useCase.validate(listOf(SentencePair("정말?", "Really!")))
        assertTrue(result.isValid)
    }

    @Test
    fun `CJK 마침표도 문장 끝으로 인정`() {
        val result = useCase.validate(listOf(SentencePair("안녕하세요。", "Hello。")))
        assertTrue(result.isValid)
    }

    @Test
    fun `구두점 뒤 공백이 있어도 유효`() {
        val result = useCase.validate(listOf(SentencePair("안녕하세요.  ", "Hello.  ")))
        assertTrue(result.isValid)
    }

    @Test
    fun `여러 문장 쌍의 오류 인덱스가 정확`() {
        val result = useCase.validate(
            listOf(
                SentencePair("좋아요.", "Good."),
                SentencePair("나쁨", "Bad.")
            )
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.contains(ValidationError.MissingPunctuation(1, isKorean = true)))
        assertFalse(result.errors.any { it is ValidationError.MissingPunctuation && it.index == 0 })
    }
}
