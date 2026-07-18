package com.na982.opichelper.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * CurrentMode.group 매핑 검증. 통암기/영작의 세부 상태(재생중/녹음중 등)가 모두
 * 올바른 ModeGroup으로 접혀야 모드 코디네이터의 상호배제와 PiP 라우팅이 맞는다.
 * when이 exhaustive이므로 새 상태가 추가되면 이 테스트가 매핑 누락을 드러낸다.
 */
class CurrentModeTest {

    @Test
    fun `NONE은 NONE 그룹`() {
        assertEquals(ModeGroup.NONE, CurrentMode.NONE.group)
    }

    @Test
    fun `반복듣기는 REPEAT_LISTENING 그룹`() {
        assertEquals(ModeGroup.REPEAT_LISTENING, CurrentMode.REPEAT_LISTENING.group)
    }

    @Test
    fun `영작 관련 상태는 모두 ENGLISH_WRITING 그룹`() {
        assertEquals(ModeGroup.ENGLISH_WRITING, CurrentMode.ENGLISH_WRITING.group)
        assertEquals(ModeGroup.ENGLISH_WRITING, CurrentMode.ENGLISH_WRITING_RECORDING.group)
    }

    @Test
    fun `통암기 세부 상태 5개는 모두 FULL_MEMORIZATION 그룹`() {
        listOf(
            CurrentMode.FULL_MEMORIZATION,
            CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
            CurrentMode.FULL_MEMORIZATION_RECORDING,
            CurrentMode.FULL_MEMORIZATION_PLAYING,
            CurrentMode.FULL_MEMORIZATION_WITH_FILE
        ).forEach { mode ->
            assertEquals("$mode 의 그룹", ModeGroup.FULL_MEMORIZATION, mode.group)
        }
    }

    @Test
    fun `모든 CurrentMode가 그룹 매핑을 가진다 (매핑 누락 없음)`() {
        // group 접근이 예외 없이 전부 동작하는지 (exhaustive when 보장 확인)
        CurrentMode.entries.forEach { it.group }
    }
}
