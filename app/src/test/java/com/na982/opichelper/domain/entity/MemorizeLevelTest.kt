package com.na982.opichelper.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MemorizeLevel의 displayName은 UI 표시 + SharedPreferences 저장 키로 쓰인다.
 * fromDisplayName은 저장된 문자열을 다시 enum으로 복원하는 경로라, 매핑이 어긋나면
 * 레벨 복원이 깨진다. 화면-모드 매핑(toModeGroup)도 함께 검증한다.
 */
class MemorizeLevelTest {

    @Test
    fun `displayName 왕복 변환이 일관적이다`() {
        MemorizeLevel.entries.forEach { level ->
            assertEquals(level, MemorizeLevel.fromDisplayName(level.displayName))
        }
    }

    @Test
    fun `알 수 없는 이름은 반복듣기로 폴백`() {
        assertEquals(MemorizeLevel.REPEAT_LISTENING, MemorizeLevel.fromDisplayName("존재하지 않는 레벨"))
    }

    @Test
    fun `빈 문자열도 반복듣기로 폴백`() {
        assertEquals(MemorizeLevel.REPEAT_LISTENING, MemorizeLevel.fromDisplayName(""))
    }

    @Test
    fun `allDisplayNames는 세 모드를 순서대로`() {
        assertEquals(
            listOf("반복 듣기", "영작 테스트", "통암기"),
            MemorizeLevel.allDisplayNames
        )
    }

    @Test
    fun `각 레벨은 대응하는 ModeGroup으로 매핑`() {
        assertEquals(ModeGroup.REPEAT_LISTENING, MemorizeLevel.REPEAT_LISTENING.toModeGroup())
        assertEquals(ModeGroup.ENGLISH_WRITING, MemorizeLevel.ENGLISH_WRITING.toModeGroup())
        assertEquals(ModeGroup.FULL_MEMORIZATION, MemorizeLevel.FULL_MEMORIZATION.toModeGroup())
    }
}
