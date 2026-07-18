package com.na982.opichelper.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 진행률 계산의 0 나눗셈 방어 검증. totalScripts가 0일 때 NaN이 아니라 0f를
 * 반환해야 통계 화면이 깨지지 않는다 (레벨 전환 직후 등 총 스크립트 0 상황 존재).
 */
class StudyStatisticsTest {

    @Test
    fun `CategoryProgress rate는 완료 나누기 총수`() {
        val cp = CategoryProgress(category = "집", completedScripts = 3, totalScripts = 12)
        assertEquals(0.25f, cp.rate, 0.0001f)
    }

    @Test
    fun `CategoryProgress 총수 0이면 rate는 0 (NaN 아님)`() {
        val cp = CategoryProgress(category = "집", completedScripts = 0, totalScripts = 0)
        assertEquals(0f, cp.rate, 0f)
    }

    @Test
    fun `StudyStatistics completionRate는 완료 나누기 총수`() {
        val stats = StudyStatistics(totalCompletedScripts = 5, totalScripts = 20)
        assertEquals(0.25f, stats.completionRate, 0.0001f)
    }

    @Test
    fun `StudyStatistics 총수 0이면 completionRate는 0 (NaN 아님)`() {
        val stats = StudyStatistics(totalCompletedScripts = 0, totalScripts = 0)
        assertEquals(0f, stats.completionRate, 0f)
    }

    @Test
    fun `기본값 StudyStatistics는 모두 0`() {
        val stats = StudyStatistics()
        assertEquals(0, stats.totalCompletedScripts)
        assertEquals(0, stats.totalScripts)
        assertEquals(0f, stats.completionRate, 0f)
    }
}
