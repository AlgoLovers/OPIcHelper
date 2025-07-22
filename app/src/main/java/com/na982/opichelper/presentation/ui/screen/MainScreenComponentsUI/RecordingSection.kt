package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecordingSection(
    isRecording: Boolean,
    hasRecording: Boolean,
    isPlayingRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayRecording: () -> Unit,
    onStopPlayback: () -> Unit,
    hasMergedRecording: Boolean, // 병합 파일 존재 여부
    isPlayingMergedRecording: Boolean, // 병합 파일 재생 중 여부
    onPlayMergedRecording: () -> Unit, // 병합 파일 재생
    onStopMergedPlayback: () -> Unit, // 병합 파일 재생 중지
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "음성 녹음",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 녹음 버튼
            FloatingActionButton(
                onClick = if (isRecording) onStopRecording else onStartRecording,
                containerColor = if (isRecording) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "녹음 중지" else "녹음 시작",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // 녹음 재생 버튼 (녹음이 있을 때만 표시)
            if (hasRecording) {
                FloatingActionButton(
                    onClick = if (isPlayingRecording) onStopPlayback else onPlayRecording,
                    containerColor = if (isPlayingRecording) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingRecording) "재생 중지" else "녹음 재생",
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
            // 병합 파일 재생 버튼 (병합 파일이 있을 때만 표시)
            if (hasMergedRecording) {
                FloatingActionButton(
                    onClick = if (isPlayingMergedRecording) onStopMergedPlayback else onPlayMergedRecording,
                    containerColor = if (isPlayingMergedRecording)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingMergedRecording) "병합 재생 중지" else "병합 파일 재생",
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }
        
        if (isRecording) {
            Text(
                text = "녹음 중...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
} 