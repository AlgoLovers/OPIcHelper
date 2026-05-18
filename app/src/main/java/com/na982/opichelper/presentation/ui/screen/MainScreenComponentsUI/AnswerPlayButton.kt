package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.na982.opichelper.domain.audio.TtsPlayer

@Composable
fun AnswerPlayButton(
    currentAnswer: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            if (isPlaying) {
                onStopClick()
            } else {
                onPlayClick()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPlaying) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    ) {
        Text(
            text = if (isPlaying) "답변 재생 중" else "답변 1회 재생",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
} 