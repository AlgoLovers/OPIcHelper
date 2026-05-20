package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.na982.opichelper.presentation.ui.component.PlayStopToggleButton

@Composable
fun AnswerPlayButton(
    @Suppress("UNUSED_PARAMETER") currentAnswer: String,
    isPlaying: Boolean,
    repeatCount: Int = 1,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayStopToggleButton(
        isActive = isPlaying,
        onActivate = onPlayClick,
        onDeactivate = onStopClick,
        activeLabel = "답변 재생 중",
        inactiveLabel = "답변 ${repeatCount}회 재생",
        modifier = modifier
    )
}
