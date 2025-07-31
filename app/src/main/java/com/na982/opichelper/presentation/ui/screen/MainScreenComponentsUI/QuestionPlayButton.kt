package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QuestionPlayButton(
    currentQuestion: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("QuestionPlayButton", "Rendering with isPlaying=$isPlaying, question=${currentQuestion.take(50)}...")
    
    Button(
        onClick = {
            Log.d("QuestionPlayButton", "Button clicked, current isPlaying=$isPlaying")
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
            text = if (isPlaying) "질문 재생 중" else "질문 1회 재생",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
} 