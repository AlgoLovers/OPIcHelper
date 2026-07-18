package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.repository.QaContentReader
import com.na982.opichelper.domain.repository.StudySessionStatisticsReader
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 학습 통계 집계 로직 검증. 완료율/카테고리 진행률 계산은 통계 화면의 핵심이고,
 * 0 나눗셈 방어가 깨지면 화면이 NaN/크래시로 이어지므로 회귀를 막는다.
 */
class StudyStatisticsCalculatorTest {

    private val statsReader = mockk<StudySessionStatisticsReader>()
    private val progressTracker = mockk<MemorizeTestProgressTracker>()
    private val qaContentReader = mockk<QaContentReader>()

    private fun calculator() = StudyStatisticsCalculator(statsReader, progressTracker, qaContentReader)

    private fun dummyItems(n: Int): List<QaItem> =
        List(n) { QaItem(id = "$it", category = "집", questionEn = "", questionKo = "", answers = emptyMap()) }

    private fun completed(category: String, idx: Int, level: String) =
        ScriptProgress(category, idx, level, currentSentenceIndex = 4, totalSentences = 5, isMemorizeTestRunning = false)

    private fun stubSession(duration: Long = 0L, streak: Int = 0, longest: Int = 0) {
        every { statsReader.getTotalStudyDurationMs() } returns duration
        every { statsReader.getStreak() } returns streak
        every { statsReader.getLongestStreak() } returns longest
        every { statsReader.getDailyRecords(any()) } returns emptyList()
    }

    @Test
    fun `데이터가 전혀 없으면 모든 값이 0이고 완료율은 0 (0 나눗셈 방어)`() {
        stubSession()
        every { progressTracker.progressMap } returns MutableStateFlow(emptyMap())
        every { qaContentReader.getCategories() } returns emptyList()

        val stats = calculator().calculate()

        assertEquals(0, stats.totalScripts)
        assertEquals(0, stats.totalCompletedScripts)
        assertEquals(0f, stats.completionRate, 0f) // totalScripts 0 → NaN이 아니라 0
    }

    @Test
    fun `총 스크립트 수는 카테고리별 항목수 곱하기 모드수(3)`() {
        stubSession()
        every { progressTracker.progressMap } returns MutableStateFlow(emptyMap())
        every { qaContentReader.getCategories() } returns listOf("집")
        every { qaContentReader.getItemsInCategory("집") } returns dummyItems(2)

        val stats = calculator().calculate()

        assertEquals(6, stats.totalScripts) // 2항목 × 3모드
    }

    @Test
    fun `완료 스크립트 집계와 완료율 계산`() {
        stubSession()
        every { qaContentReader.getCategories() } returns listOf("집")
        every { qaContentReader.getItemsInCategory("집") } returns dummyItems(2)
        every { progressTracker.progressMap } returns MutableStateFlow(
            mapOf(
                "집_0_반복 듣기" to completed("집", 0, "반복 듣기"),
                "집_1_영작 테스트" to completed("집", 1, "영작 테스트"),
                // 실행 중인 항목은 완료로 세지 않아야 함
                "집_0_통암기" to completed("집", 0, "통암기").copy(isMemorizeTestRunning = true)
            )
        )

        val stats = calculator().calculate()

        assertEquals(2, stats.totalCompletedScripts)
        assertEquals(2f / 6f, stats.completionRate, 0.0001f)
    }

    @Test
    fun `모드별 완료 분석은 세 모드를 모두 포함하고 완료 개수를 센다`() {
        stubSession()
        every { qaContentReader.getCategories() } returns listOf("집")
        every { qaContentReader.getItemsInCategory("집") } returns dummyItems(2)
        every { progressTracker.progressMap } returns MutableStateFlow(
            mapOf(
                "집_0_반복 듣기" to completed("집", 0, "반복 듣기"),
                "집_1_반복 듣기" to completed("집", 1, "반복 듣기"),
                "집_0_영작 테스트" to completed("집", 0, "영작 테스트")
            )
        )

        val breakdown = calculator().calculate().modeBreakdown

        assertEquals(3, breakdown.size)
        assertEquals(2, breakdown["반복 듣기"])
        assertEquals(1, breakdown["영작 테스트"])
        assertEquals(0, breakdown["통암기"])
    }

    @Test
    fun `카테고리 진행률은 완료수와 총수(항목x3)로 계산`() {
        stubSession()
        every { qaContentReader.getCategories() } returns listOf("집")
        every { qaContentReader.getItemsInCategory("집") } returns dummyItems(2)
        every { progressTracker.progressMap } returns MutableStateFlow(
            mapOf("집_0_반복 듣기" to completed("집", 0, "반복 듣기"))
        )

        val cat = calculator().calculate().categoryProgress.single()

        assertEquals("집", cat.category)
        assertEquals(1, cat.completedScripts)
        assertEquals(6, cat.totalScripts)
        assertEquals(1f / 6f, cat.rate, 0.0001f)
    }

    @Test
    fun `세션 통계값은 reader에서 그대로 전달된다`() {
        stubSession(duration = 123_456L, streak = 5, longest = 9)
        every { progressTracker.progressMap } returns MutableStateFlow(emptyMap())
        every { qaContentReader.getCategories() } returns emptyList()

        val stats = calculator().calculate()

        assertEquals(123_456L, stats.totalStudyDurationMs)
        assertEquals(5, stats.streak)
        assertEquals(9, stats.longestStreak)
    }
}
