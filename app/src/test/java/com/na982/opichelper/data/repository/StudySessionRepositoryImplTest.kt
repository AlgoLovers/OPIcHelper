package com.na982.opichelper.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.na982.opichelper.domain.manager.AppLogger
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 학습 세션 기록/streak 로직 검증 (Robolectric로 실제 SharedPreferences 사용).
 * 통계 화면의 데이터 정합성을 담당하는 핵심 로직이며, streak 계산과 누적 저장이
 * 어긋나면 사용자 학습 기록이 잘못 표시된다. (LocalDate.now 기준 상대 검증)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudySessionRepositoryImplTest {

    private lateinit var repo: StudySessionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = StudySessionRepositoryImpl(context, mockk<AppLogger>(relaxed = true), Gson())
    }

    @Test
    fun `초기 상태는 streak longest total 모두 0`() {
        assertEquals(0, repo.getStreak())
        assertEquals(0, repo.getLongestStreak())
        assertEquals(0L, repo.getTotalStudyDurationMs())
    }

    @Test
    fun `getDailyRecords는 요청 일수만큼 반환하고 초기값은 0`() {
        val records = repo.getDailyRecords(7)
        assertEquals(7, records.size)
        assertTrue(records.all { it.studyDurationMs == 0L })
    }

    @Test
    fun `recordSession 후 총 학습시간 streak longest가 반영된다`() {
        repo.recordSession(5000L)

        assertEquals(5000L, repo.getTotalStudyDurationMs())
        assertEquals(1, repo.getStreak())        // 오늘 활동 → streak 1
        assertEquals(1, repo.getLongestStreak()) // 최장 기록 갱신
        // getDailyRecords(1)의 첫 요소는 오늘
        assertEquals(5000L, repo.getDailyRecords(1).first().studyDurationMs)
    }

    @Test
    fun `같은 날 recordSession 반복은 학습시간을 누적한다`() {
        repo.recordSession(5000L)
        repo.recordSession(3000L)

        assertEquals(8000L, repo.getTotalStudyDurationMs())
        assertEquals(8000L, repo.getDailyRecords(1).first().studyDurationMs)
        assertEquals(1, repo.getStreak()) // 여전히 오늘 하루뿐
    }

    @Test
    fun `getLongestStreak는 기록 후에도 유지된다`() {
        repo.recordSession(1000L)
        assertEquals(1, repo.getLongestStreak())
    }
}
