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
fun AnswerPlayButton(
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
    
    Log.d("AnswerPlayButton", "Rendering with isPlaying=$internalIsPlaying, answer=${currentAnswer.take(50)}...")
    
    Button(
        onClick = {
            Log.d("AnswerPlayButton", "Button clicked, current isPlaying=$internalIsPlaying")
            
            if (internalIsPlaying) {
                Log.d("AnswerPlayButton", "Stopping answer playback")
                ttsPlayer?.stopTts()
                internalIsPlaying = false
            } else {
                Log.d("AnswerPlayButton", "Starting answer playback (1 time)")
                ttsPlayer?.let { player ->
                    try {
                        player.speakAnswer(currentAnswer)
                        internalIsPlaying = true
                        Log.d("AnswerPlayButton", "Answer playback started successfully")
                    } catch (e: Exception) {
                        Log.e("AnswerPlayButton", "Failed to start answer playback", e)
                        internalIsPlaying = false
                    }
                } ?: run {
                    Log.e("AnswerPlayButton", "TtsPlayer is null, cannot play answer")
                    internalIsPlaying = false
                }
            }
            
            Log.d("AnswerPlayButton", "State changed to isPlaying=$internalIsPlaying")
            onStateChange(internalIsPlaying)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (internalIsPlaying) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    ) {
        Text(
            text = if (internalIsPlaying) "답변 재생 중" else "답변 1회 재생",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
} 