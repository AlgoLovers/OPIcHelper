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
fun QuestionPlayButton(
    currentQuestion: String,
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
    
    Log.d("QuestionPlayButton", "Rendering with isPlaying=$internalIsPlaying, question=${currentQuestion.take(50)}...")
    
    Button(
        onClick = {
            Log.d("QuestionPlayButton", "Button clicked, current isPlaying=$internalIsPlaying")
            
            if (internalIsPlaying) {
                Log.d("QuestionPlayButton", "Stopping TTS playback")
                ttsPlayer?.stopTts()
                internalIsPlaying = false
            } else {
                Log.d("QuestionPlayButton", "Starting TTS playback for question")
                ttsPlayer?.let { player ->
                    try {
                        player.speakQuestion(currentQuestion)
                        internalIsPlaying = true
                        Log.d("QuestionPlayButton", "TTS playback started successfully")
                    } catch (e: Exception) {
                        Log.e("QuestionPlayButton", "Failed to start TTS playback", e)
                        internalIsPlaying = false
                    }
                } ?: run {
                    Log.e("QuestionPlayButton", "TtsPlayer is null, cannot play question")
                    internalIsPlaying = false
                }
            }
            
            Log.d("QuestionPlayButton", "State changed to isPlaying=$internalIsPlaying")
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
            text = if (internalIsPlaying) "질문 일시정지" else "질문 재생",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
} 