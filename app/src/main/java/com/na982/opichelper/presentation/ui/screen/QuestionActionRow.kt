package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.ModeGroup
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.FullMemorizationRecordingButton
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.QuestionPlayButton
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.presentation.viewmodel.MemorizationController

@Composable
fun QuestionActionRow(
    isQuestionPlaying: Boolean,
    questionEn: String,
    coordinatorGroup: ModeGroup,
    isFullMemorizationQuestionPlaying: Boolean,
    isFullMemorizationRecording: Boolean,
    selectedLevel: String,
    isRepeatListeningPlaying: Boolean,
    isCoordinatorRunning: Boolean,
    coordinator: MemorizationModeCoordinator,
    memorizationController: MemorizationController,
    onPlayQuestion: () -> Unit,
    onStopTts: () -> Unit,
    onStartFullMemorization: () -> Unit,
    onStopFullMemorizationRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuestionPlayButton(
            isPlaying = isQuestionPlaying,
            onPlayClick = onPlayQuestion,
            onStopClick = onStopTts,
            modifier = Modifier.weight(1f)
        )

        if (coordinatorGroup == ModeGroup.FULL_MEMORIZATION) {
            FullMemorizationRecordingButton(
                isQuestionPlaying = isFullMemorizationQuestionPlaying,
                isRecording = isFullMemorizationRecording,
                onStartRecording = onStartFullMemorization,
                onStopRecording = onStopFullMemorizationRecording,
                modifier = Modifier.weight(1f)
            )
        } else {
            Button(
                onClick = {
                    if (MemorizeLevel.fromDisplayName(selectedLevel) != MemorizeLevel.FULL_MEMORIZATION) {
                        onStopTts()
                    }
                    if (isRepeatListeningPlaying || isCoordinatorRunning) {
                        memorizationController.stopCurrent(coordinator)
                    } else {
                        memorizationController.startForLevel(selectedLevel)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRepeatListeningPlaying || isCoordinatorRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when {
                        isRepeatListeningPlaying || isCoordinatorRunning -> "${selectedLevel} 종료"
                        MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.ENGLISH_WRITING -> "부분암기 테스트"
                        MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION -> "통암기"
                        else -> selectedLevel.ifEmpty { "암기 테스트" }
                    },
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
