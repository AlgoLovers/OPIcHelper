package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.presentation.ui.component.PlayStopToggleButton

@Composable
fun MemorizeLevelPlaybackButton(
    selectedLevel: String,
    onPlayEnglishWritingTest: () -> Unit,
    onStopEnglishWritingTest: () -> Unit,
    onPlayFullMemorization: () -> Unit,
    onStopFullMemorization: () -> Unit,
    hasEnglishWritingTestMergedFile: Boolean,
    isEnglishWritingTestMergedFilePlaying: Boolean,
    hasFullMemorizationRecording: Boolean,
    isFullMemorizationPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    when (MemorizeLevel.fromDisplayName(selectedLevel)) {
        MemorizeLevel.REPEAT_LISTENING -> {
            // 반복듣기 모드에서는 별도 재생 버튼 없음
        }
        MemorizeLevel.ENGLISH_WRITING -> {
            if (hasEnglishWritingTestMergedFile) {
                PlayStopToggleButton(
                    isActive = isEnglishWritingTestMergedFilePlaying,
                    onActivate = onPlayEnglishWritingTest,
                    onDeactivate = onStopEnglishWritingTest,
                    activeLabel = "재생 중...",
                    inactiveLabel = "영작테스트 녹음 재생",
                    modifier = modifier
                )
            }
        }
        MemorizeLevel.FULL_MEMORIZATION -> {
            if (hasFullMemorizationRecording) {
                PlayStopToggleButton(
                    isActive = isFullMemorizationPlaying,
                    onActivate = onPlayFullMemorization,
                    onDeactivate = onStopFullMemorization,
                    activeLabel = "재생 중...",
                    inactiveLabel = "통암기 녹음 재생",
                    modifier = modifier
                )
            }
        }
    }
}
