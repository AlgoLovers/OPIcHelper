package com.na982.opichelper.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ScriptProgress.isCompleted()는 학습 통계의 "완료 스크립트" 집계와 진행률 계산의
 * 핵심 판정이다. 조건이 어긋나면 완료율이 잘못 표시되므로 회귀를 막는다.
 */
class ScriptProgressTest {

    private fun progress(
        currentSentenceIndex: Int,
        totalSentences: Int,
        isRunning: Boolean
    ) = ScriptProgress(
        category = "집",
        scriptIndex = 0,
        memorizeLevel = "반복 듣기",
        currentSentenceIndex = currentSentenceIndex,
        totalSentences = totalSentences,
        isMemorizeTestRunning = isRunning
    )

    @Test
    fun `마지막 문장 도달 & 실행중 아님 = 완료`() {
        assertTrue(progress(currentSentenceIndex = 4, totalSentences = 5, isRunning = false).isCompleted())
    }

    @Test
    fun `마지막 문장 미도달 = 미완료`() {
        assertFalse(progress(currentSentenceIndex = 3, totalSentences = 5, isRunning = false).isCompleted())
    }

    @Test
    fun `실행 중이면 인덱스가 끝이어도 미완료`() {
        assertFalse(progress(currentSentenceIndex = 4, totalSentences = 5, isRunning = true).isCompleted())
    }

    @Test
    fun `문장 하나짜리는 인덱스 0에서 완료`() {
        assertTrue(progress(currentSentenceIndex = 0, totalSentences = 1, isRunning = false).isCompleted())
    }

    @Test
    fun `progressKey는 카테고리_인덱스_레벨 형식`() {
        assertEquals("집_2_반복 듣기", ScriptProgress.progressKey("집", 2, "반복 듣기"))
    }

    @Test
    fun `getKey는 progressKey와 동일`() {
        val p = progress(currentSentenceIndex = 0, totalSentences = 3, isRunning = false)
        assertEquals(ScriptProgress.progressKey(p.category, p.scriptIndex, p.memorizeLevel), p.getKey())
    }

    @Test
    fun `toPersistable은 needsSave를 false로`() {
        val p = progress(currentSentenceIndex = 1, totalSentences = 3, isRunning = false).copy(needsSave = true)
        assertFalse(p.toPersistable().needsSave)
    }
}
