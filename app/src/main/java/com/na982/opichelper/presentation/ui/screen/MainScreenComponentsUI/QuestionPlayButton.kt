package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.na982.opichelper.presentation.ui.component.PlayStopToggleButton

@Composable
fun QuestionPlayButton(
    @Suppress("UNUSED_PARAMETER") currentQuestion: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayStopToggleButton(
        isActive = isPlaying,
        onActivate = onPlayClick,
        onDeactivate = onStopClick,
        activeLabel = "질문 재생 중",
        inactiveLabel = "질문 1회 재생",
        modifier = modifier
    )
}
