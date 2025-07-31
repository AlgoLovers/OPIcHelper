package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 통암기 녹음 버튼 컴포넌트 (질문재생 버튼과 동일한 스타일)
 */
@Composable
fun FullMemorizationRecordingButton(
    isQuestionPlaying: Boolean,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            if (isRecording) {
                onStopRecording()
            } else {
                onStartRecording()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isQuestionPlaying || isRecording) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    ) {
        Text(
            text = when {
                isQuestionPlaying -> "질문재생중"
                isRecording -> "녹음 중"
                else -> "답변 녹음"
            },
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
} 