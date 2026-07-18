package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.repository.DataSeeder
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.ProgressPersistenceService.NavigationState
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.UserLevelPreferences
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA 네비게이션(다음/이전/인덱스 이동)과 경계 처리 검증. 사용자가 가장 많이 쓰는
 * 경로이고, 인덱스 경계가 어긋나면 앱이 잘못된 항목을 보이거나 크래시한다.
 * 특히 복원 시 저장된 인덱스가 범위를 벗어나도 크래시하지 않아야 한다.
 */
class QaDataManagerNavigationTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeLoader(private val items: List<QaItem>) : QaDataLoader {
        override suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> = items
    }

    private class FakeUserLevel : UserLevelPreferences {
        override val userLevel: StateFlow<UserLevel> = MutableStateFlow(UserLevel.IH)
        override fun getUserLevel() = UserLevel.IH
        override fun setUserLevel(level: UserLevel) {}
    }

    private class FakeProgress(private val nav: NavigationState) : ProgressPersistenceService {
        var lastSaved: NavigationState? = null
        override suspend fun saveNavigationState(state: NavigationState) { lastSaved = state }
        override suspend fun loadNavigationState(): NavigationState = nav
        override suspend fun saveCategoryProgress(progress: ScriptProgress) {}
        override suspend fun loadAllCategoryProgress(): Map<String, ScriptProgress> = emptyMap()
        override suspend fun clearCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String) {}
    }

    private class FakeSeeder : DataSeeder {
        override suspend fun seedIfNeeded() {}
    }

    private fun item(category: String, id: String, q: String = "question") =
        QaItem(
            id = id,
            category = category,
            questionEn = "$q $id",
            questionKo = "질문 $id",
            answers = mapOf(UserLevel.IH to LeveledAnswer("answer $id", "답변 $id"))
        )

    // 집: 3개, 영화: 2개 (preferredOrder상 집이 먼저)
    private val items = listOf(
        item("집", "1"), item("집", "2"), item("집", "3"),
        item("영화", "10"), item("영화", "11")
    )

    private fun manager(nav: NavigationState = NavigationState(category = null)) =
        QaDataManagerImpl(
            FakeLoader(items),
            FakeUserLevel(),
            FakeProgress(nav),
            FakeSeeder(),
            mockk<AppLogger>(relaxed = true)
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init 후 첫 카테고리 첫 항목이 선택된다`() = runTest(dispatcher) {
        val m = manager()
        m.init()

        assertEquals("집", m.getCurrentCategory())
        assertEquals(0, m.getCurrentIndex())
        assertEquals("1", m.getCurrentQaItem()?.id)
        assertTrue(m.hasNextQaItem())
    }

    @Test
    fun `nextQaItem은 인덱스를 전진시키고 마지막에서 멈춘다`() = runTest(dispatcher) {
        val m = manager()
        m.init()

        m.nextQaItem()
        assertEquals(1, m.getCurrentIndex())
        m.nextQaItem()
        assertEquals(2, m.getCurrentIndex())
        assertFalse(m.hasNextQaItem()) // 3개 중 마지막

        m.nextQaItem() // 마지막에서 한 번 더 — 경계 초과 없이 그대로
        assertEquals(2, m.getCurrentIndex())
        assertEquals("3", m.getCurrentQaItem()?.id)
    }

    @Test
    fun `previousQaItem은 인덱스를 감소시키고 0에서 멈춘다`() = runTest(dispatcher) {
        val m = manager()
        m.init()
        m.nextQaItem(); m.nextQaItem() // index 2

        m.previousQaItem()
        assertEquals(1, m.getCurrentIndex())
        m.previousQaItem()
        assertEquals(0, m.getCurrentIndex())
        m.previousQaItem() // 0에서 한 번 더 — 음수 없이 그대로
        assertEquals(0, m.getCurrentIndex())
    }

    @Test
    fun `navigateToIndex는 범위 밖 값을 무시한다`() = runTest(dispatcher) {
        val m = manager()
        m.init()

        m.navigateToIndex(99) // 범위 밖 — 무시
        assertEquals(0, m.getCurrentIndex())
        m.navigateToIndex(-1) // 음수 — 무시
        assertEquals(0, m.getCurrentIndex())
        m.navigateToIndex(2) // 유효
        assertEquals(2, m.getCurrentIndex())
    }

    @Test
    fun `selectCategory는 카테고리를 바꾸고 인덱스를 0으로 리셋`() = runTest(dispatcher) {
        val m = manager()
        m.init()
        m.nextQaItem() // 집 index 1

        m.selectCategory("영화")
        assertEquals("영화", m.getCurrentCategory())
        assertEquals(0, m.getCurrentIndex())
        assertEquals("10", m.getCurrentQaItem()?.id)
    }

    @Test
    fun `존재하지 않는 카테고리 선택은 무시된다`() = runTest(dispatcher) {
        val m = manager()
        m.init()

        m.selectCategory("없는카테고리")
        assertEquals("집", m.getCurrentCategory()) // 변경 없음
    }

    @Test
    fun `복원 시 저장된 인덱스가 범위를 벗어나면 크래시 없이 현재 항목이 null`() = runTest(dispatcher) {
        // 집은 3개(0~2)인데 저장된 scriptIndex=99 → 인덱스 가드가 크래시 대신 null 처리
        val m = manager(NavigationState(category = "집", scriptIndex = 99))
        m.init()

        assertEquals("집", m.getCurrentCategory())
        assertNull(m.getCurrentQaItem())
    }

    @Test
    fun `검색은 2글자 미만이면 빈 결과, 이상이면 내용 매칭`() = runTest(dispatcher) {
        val m = manager()
        m.init()

        assertTrue(m.searchItems("q").isEmpty()) // 1글자 → 빈 결과
        assertEquals(5, m.searchItems("question").size) // 모든 questionEn에 포함
        assertEquals(1, m.searchItems("답변 10").size) // 특정 답변만
    }
}
