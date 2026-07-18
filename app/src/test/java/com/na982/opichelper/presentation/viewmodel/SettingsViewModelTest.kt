package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.PlaybackPreferences
import com.na982.opichelper.domain.repository.TtsPreferences
import com.na982.opichelper.domain.repository.UserLevelPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 설정 값 범위 강제(coerce) 검증. 슬라이더 UI가 아니라 여기가 실제 상한/하한을
 * 강제하므로, 손상 입력이나 UI 변경이 있어도 저장 값이 허용 범위를 벗어나지 않아야 한다.
 * (반복듣기 2~50, 답변 재생 1~10, 영어 TTS 속도 0.5~1.5)
 *
 * Preferences는 StateFlow 프로퍼티가 있어 mockk relaxed가 불안정하므로 손 fake를 쓴다.
 */
class SettingsViewModelTest {

    private class FakePlaybackPreferences : PlaybackPreferences {
        var lastRepeatSet: Int? = null
        var lastAnswerSet: Int? = null
        override val repeatListeningCount = MutableStateFlow(5)
        override val answerPlayCount = MutableStateFlow(1)
        override fun getRepeatListeningCount() = repeatListeningCount.value
        override fun setRepeatListeningCount(count: Int) { lastRepeatSet = count; repeatListeningCount.value = count }
        override fun getAnswerPlayCount() = answerPlayCount.value
        override fun setAnswerPlayCount(count: Int) { lastAnswerSet = count; answerPlayCount.value = count }
        override fun isAutoAdvance() = false
    }

    private class FakeTtsPreferences : TtsPreferences {
        var lastRateSet: Float? = null
        override val englishTtsRate = MutableStateFlow(0.8f)
        override fun getEnglishTtsRate() = englishTtsRate.value
        override fun setEnglishTtsRate(rate: Float) { lastRateSet = rate; englishTtsRate.value = rate }
    }

    private class FakeUserLevelPreferences : UserLevelPreferences {
        override val userLevel: StateFlow<UserLevel> = MutableStateFlow(UserLevel.IH)
        override fun getUserLevel() = userLevel.value
        override fun setUserLevel(level: UserLevel) {}
    }

    private lateinit var playback: FakePlaybackPreferences
    private lateinit var tts: FakeTtsPreferences
    private lateinit var userLevel: FakeUserLevelPreferences
    private lateinit var orchestrator: TtsOrchestrator

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        playback = FakePlaybackPreferences()
        tts = FakeTtsPreferences()
        userLevel = FakeUserLevelPreferences()
        orchestrator = mockk(relaxed = true)
        every { orchestrator.getCurrentKoreanTtsServiceName() } returns "삼성"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = SettingsViewModel(userLevel, tts, playback, orchestrator)

    // ── 반복듣기 횟수: 2~50 ──────────────────────────────────────────
    @Test
    fun `반복듣기 횟수는 상한 50으로 제한`() {
        vm().setRepeatListeningCount(100)
        assertEquals(50, playback.lastRepeatSet)
    }

    @Test
    fun `반복듣기 횟수는 하한 2로 제한`() {
        vm().setRepeatListeningCount(1)
        assertEquals(2, playback.lastRepeatSet)
    }

    @Test
    fun `반복듣기 횟수 범위 안의 값은 그대로`() {
        vm().setRepeatListeningCount(37)
        assertEquals(37, playback.lastRepeatSet)
    }

    // ── 답변 재생 횟수: 1~10 ─────────────────────────────────────────
    @Test
    fun `답변 재생 횟수는 상한 10으로 제한`() {
        vm().setAnswerPlayCount(25)
        assertEquals(10, playback.lastAnswerSet)
    }

    @Test
    fun `답변 재생 횟수는 하한 1로 제한`() {
        vm().setAnswerPlayCount(0)
        assertEquals(1, playback.lastAnswerSet)
    }

    // ── 영어 TTS 속도: 0.5~1.5 ───────────────────────────────────────
    @Test
    fun `TTS 속도는 상한 1_5로 제한`() {
        vm().setEnglishTtsRate(3.0f)
        assertEquals(1.5f, tts.lastRateSet!!, 0.0001f)
    }

    @Test
    fun `TTS 속도는 하한 0_5로 제한`() {
        vm().setEnglishTtsRate(0.1f)
        assertEquals(0.5f, tts.lastRateSet!!, 0.0001f)
    }
}
