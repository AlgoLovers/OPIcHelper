package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * 통암기 녹음 버튼 컴포넌트 (질문재생 버튼과 동일한 스타일)
 */
@Composable
fun FullMemorizationRecordingButton(
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
            containerColor = if (isRecording) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    ) {
        Text(
            text = if (isRecording) "녹음 종료" else "답변 녹음", // "녹음중" → "녹음 종료"로 변경
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
} 