package com.na982.opichelper.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.na982.opichelper.domain.entity.UserLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 사용자 설정의 기본값·왕복·영속화 검증 (Robolectric). dual-write(StateFlow + prefs)가
 * 실제로 저장되어 앱 재시작(새 인스턴스) 시 복원되는지, 손상된 레벨 문자열이 IH로
 * 안전하게 폴백되는지 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repo: UserPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = UserPreferencesRepository(context)
    }

    @Test
    fun `초기 기본값`() {
        assertEquals(UserLevel.IH, repo.getUserLevel())
        assertEquals(0.8f, repo.getEnglishTtsRate(), 0.0001f)
        assertEquals(5, repo.getRepeatListeningCount())
        assertEquals(1, repo.getAnswerPlayCount())
        assertEquals("", repo.getMemorizeLevel())
        assertFalse(repo.isOnboardingCompleted())
        assertFalse(repo.isPipGuideCompleted())
        assertFalse(repo.isAutoAdvance())
        assertEquals(0, repo.getSeedVersion())
    }

    @Test
    fun `설정 변경이 getter와 StateFlow에 즉시 반영된다`() {
        repo.setUserLevel(UserLevel.AL)
        assertEquals(UserLevel.AL, repo.getUserLevel())
        assertEquals(UserLevel.AL, repo.userLevel.value)

        repo.setRepeatListeningCount(30)
        assertEquals(30, repo.getRepeatListeningCount())
        assertEquals(30, repo.repeatListeningCount.value)

        repo.setEnglishTtsRate(1.2f)
        assertEquals(1.2f, repo.englishTtsRate.value, 0.0001f)
    }

    @Test
    fun `저장한 값이 새 인스턴스(앱 재시작)에서 복원된다`() {
        repo.setUserLevel(UserLevel.IM)
        repo.setRepeatListeningCount(42)
        repo.setAnswerPlayCount(7)
        repo.setEnglishTtsRate(1.1f)
        repo.setMemorizeLevel("영작 테스트")

        // 같은 context로 새 인스턴스 생성 → init에서 prefs 로드
        val restored = UserPreferencesRepository(context)
        assertEquals(UserLevel.IM, restored.getUserLevel())
        assertEquals(42, restored.getRepeatListeningCount())
        assertEquals(7, restored.getAnswerPlayCount())
        assertEquals(1.1f, restored.getEnglishTtsRate(), 0.0001f)
        assertEquals("영작 테스트", restored.getMemorizeLevel())
    }

    @Test
    fun `온보딩 PiP 시드버전 플래그 왕복`() {
        repo.setOnboardingCompleted()
        assertTrue(repo.isOnboardingCompleted())

        repo.setPipGuideCompleted()
        assertTrue(repo.isPipGuideCompleted())

        repo.setSeedVersion(3)
        assertEquals(3, repo.getSeedVersion())
    }

    @Test
    fun `저장된 레벨 문자열이 손상되면 IH로 폴백`() {
        // prefs에 잘못된 레벨 문자열을 직접 써 두고 새 인스턴스 생성
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit().putString("user_level", "존재하지않는레벨").apply()

        val loaded = UserPreferencesRepository(context)
        assertEquals(UserLevel.IH, loaded.getUserLevel())
    }
}
