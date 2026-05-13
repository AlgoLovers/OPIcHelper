package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel

@Composable
fun MemorizeLevelPlaybackButton(
    selectedLevel: String,
    mainViewModel: PlaybackViewModel,
    memorizationViewModel: MemorizationViewModel,
    hasEnglishWritingTestMergedFile: Boolean,
    isEnglishWritingTestMergedFilePlaying: Boolean,
    hasFullMemorizationRecording: Boolean,
    isFullMemorizationRecordingPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    when (MemorizeLevel.fromDisplayName(selectedLevel)) {
        MemorizeLevel.REPEAT_LISTENING -> {}
        MemorizeLevel.ENGLISH_WRITING -> {
            if (hasEnglishWritingTestMergedFile) {
                Button(
                    onClick = {
                        if (isEnglishWritingTestMergedFilePlaying) {
                            mainViewModel.stopEnglishWritingTestMergedFile()
                        } else {
                            mainViewModel.playEnglishWritingTestMergedFile()
                        }
                    },
                    enabled = hasEnglishWritingTestMergedFile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnglishWritingTestMergedFilePlaying)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = modifier
                ) {
                    Text(
                        text = if (isEnglishWritingTestMergedFilePlaying) "재생 중..." else "영작테스트 녹음 재생",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        MemorizeLevel.FULL_MEMORIZATION -> {
            if (hasFullMemorizationRecording) {
                Button(
                    onClick = {
                        if (isFullMemorizationRecordingPlaying) {
                            memorizationViewModel.stopFullMemorizationPlaying()
                        } else {
                            memorizationViewModel.playFullMemorizationRecording()
                        }
                    },
                    enabled = hasFullMemorizationRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFullMemorizationRecordingPlaying)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = modifier
                ) {
                    Text(
                        text = if (isFullMemorizationRecordingPlaying) "재생 중..." else "통암기 녹음 재생",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
