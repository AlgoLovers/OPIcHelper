package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer

@Composable
fun AnswerRepeatPlayButton(
    currentAnswer: String,
    ttsPlayer: TtsPlayer?,
    onStateChange: (Boolean) -> Unit,
    isPlaying: Boolean = false, // 외부에서 상태 제어
    modifier: Modifier = Modifier
) {
    var internalIsPlaying by remember { mutableStateOf(isPlaying) }
    
    // 외부 상태가 변경되면 내부 상태도 업데이트
    LaunchedEffect(isPlaying) {
        internalIsPlaying = isPlaying
    }
    
    Log.d("AnswerRepeatPlayButton", "Rendering with isPlaying=$internalIsPlaying, answer=${currentAnswer.take(50)}...")
    
    Button(
        onClick = {
            Log.d("AnswerRepeatPlayButton", "Button clicked, current isPlaying=$internalIsPlaying")
            
            if (internalIsPlaying) {
                Log.d("AnswerRepeatPlayButton", "Stopping repeat playback")
                ttsPlayer?.stopTts()
                internalIsPlaying = false
            } else {
                Log.d("AnswerRepeatPlayButton", "Starting answer repeat playback (5 times)")
                ttsPlayer?.let { player ->
                    try {
                        player.speakAnswer(currentAnswer, repeatCount = 5)
                        internalIsPlaying = true
                        Log.d("AnswerRepeatPlayButton", "Answer repeat playback started successfully")
                    } catch (e: Exception) {
                        Log.e("AnswerRepeatPlayButton", "Failed to start answer repeat playback", e)
                        internalIsPlaying = false
                    }
                } ?: run {
                    Log.e("AnswerRepeatPlayButton", "TtsPlayer is null, cannot play answer repeat")
                    internalIsPlaying = false
                }
            }
            
            Log.d("AnswerRepeatPlayButton", "State changed to isPlaying=$internalIsPlaying")
            onStateChange(internalIsPlaying)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (internalIsPlaying) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.tertiary
        ),
        modifier = modifier
    ) {
        Text(
            text = if (internalIsPlaying) "반복 재생 중" else "답변 5회 반복",
            color = MaterialTheme.colorScheme.onTertiary
        )
    }
} 