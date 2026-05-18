package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.na982.opichelper.presentation.ui.component.PlayStopToggleButton

@Composable
fun FullMemorizationRecordingButton(
    isQuestionPlaying: Boolean,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayStopToggleButton(
        isActive = isQuestionPlaying || isRecording,
        onActivate = onStartRecording,
        onDeactivate = onStopRecording,
        activeLabel = if (isQuestionPlaying) "질문재생중" else "녹음 중",
        inactiveLabel = "답변 녹음",
        modifier = modifier
    )
}
