package com.na982.opichelper.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.repository.ProgressPersistenceService.NavigationState
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 네비게이션 상태/카테고리 진행상황의 SharedPreferences 영속화 왕복 검증 (Robolectric).
 * 앱 재시작 시 마지막 위치/진행률 복원의 정확성을 담보한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressPersistenceServiceImplTest {

    private lateinit var svc: ProgressPersistenceServiceImpl

    @Before
    fun setUp() {
        svc = ProgressPersistenceServiceImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            mockk<AppLogger>(relaxed = true),
            Gson()
        )
    }

    @Test
    fun `저장 전 네비게이션 상태는 기본값`() = runTest {
        val s = svc.loadNavigationState()
        assertNull(s.category)
        assertEquals(-1, s.scriptIndex)
        assertEquals(0, s.sentenceIndex)
    }

    @Test
    fun `네비게이션 상태 저장 후 그대로 복원된다`() = runTest {
        svc.saveNavigationState(NavigationState(category = "집", scriptIndex = 3, sentenceIndex = 2))
        val s = svc.loadNavigationState()
        assertEquals("집", s.category)
        assertEquals(3, s.scriptIndex)
        assertEquals(2, s.sentenceIndex)
    }

    @Test
    fun `카테고리 진행상황 저장 후 키로 로드된다`() = runTest {
        val p = ScriptProgress("집", 0, "반복 듣기", currentSentenceIndex = 4, totalSentences = 5, isMemorizeTestRunning = false)
        svc.saveCategoryProgress(p)

        val all = svc.loadAllCategoryProgress()
        assertEquals(1, all.size)
        assertTrue(all.containsKey(p.getKey()))
        assertEquals(4, all[p.getKey()]!!.currentSentenceIndex)
    }

    @Test
    fun `여러 진행상황이 각각 로드된다`() = runTest {
        svc.saveCategoryProgress(ScriptProgress("집", 0, "반복 듣기", 4, 5, false))
        svc.saveCategoryProgress(ScriptProgress("영화", 1, "영작 테스트", 2, 3, false))

        assertEquals(2, svc.loadAllCategoryProgress().size)
    }

    @Test
    fun `진행상황 삭제 후 로드되지 않는다`() = runTest {
        svc.saveCategoryProgress(ScriptProgress("집", 0, "반복 듣기", 4, 5, false))
        svc.clearCategoryProgress("집", 0, "반복 듣기")
        assertTrue(svc.loadAllCategoryProgress().isEmpty())
    }
}
