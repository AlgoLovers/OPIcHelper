package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.na982.opichelper.domain.entity.CurrentMode
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.ModeGroup
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.AnswerCard
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.AnswerPlayButton
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.MemorizeLevelPlaybackButton
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.RecordingAnimation
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationController
import com.na982.opichelper.presentation.viewmodel.PlaybackState
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningUiState

@Composable
fun AnswerSection(
    qaItem: QaItem,
    coordinatorGroup: ModeGroup,
    isFullMemorizationQuestionPlaying: Boolean,
    isFullMemorizationRecording: Boolean,
    isFullMemorizationPlaying: Boolean,
    fullMemorizationHighlightIndex: Int?,
    playbackState: PlaybackState,
    repeatListeningState: RepeatListeningUiState,
    englishWritingTestIsCardFlipped: Boolean,
    selectedLevel: String,
    currentMode: CurrentMode,
    coordinator: MemorizationModeCoordinator,
    memorizationController: MemorizationController,
    playbackViewModel: PlaybackViewModel,
    fullMemorizationViewModel: FullMemorizationViewModel,
    currentAnswer: String,
    currentAnswerKo: String,
    currentUserLevel: com.na982.opichelper.domain.entity.UserLevel,
    currentIndex: Int,
    qaItemCategory: String,
    qaItemId: String,
    answerPlayCount: Int,
    hasFullMemorizationRecording: Boolean,
    onEditScript: (EditScriptState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (coordinatorGroup != ModeGroup.FULL_MEMORIZATION || (!isFullMemorizationQuestionPlaying && !isFullMemorizationRecording)) {
        AnswerCard(
            currentAnswer = currentAnswer,
            currentAnswerKo = currentAnswerKo,
            highlightIndex = resolveAnswerHighlightIndex(
                coordinatorGroup = coordinatorGroup,
                isFullMemorizationPlaying = isFullMemorizationPlaying,
                fullMemorizationHighlightIndex = fullMemorizationHighlightIndex,
                playbackState = playbackState
            ),
            answerKoHighlightIndex = playbackState.answerKoHighlight.index,
            recordingHighlightIndex = playbackState.recordingHighlight.index,
            resumeHighlightIndex = if (!repeatListeningState.isPlaying) repeatListeningState.resumeSentenceIndex else null,
            isFlipped = when {
                coordinatorGroup == ModeGroup.ENGLISH_WRITING -> englishWritingTestIsCardFlipped
                playbackState.isEnglishWritingTestMergedFilePlaying -> false
                repeatListeningState.isCardFlipped -> repeatListeningState.isCardFlipped
                else -> false
            },
            isModified = qaItem.isModified,
            onEdit = {
                onEditScript(EditScriptState(
                    qaItem = qaItem,
                    isQuestion = false,
                    level = currentUserLevel,
                    scriptIndex = currentIndex,
                    entityId = "${qaItemCategory}_${qaItemId}_${currentUserLevel.name}"
                ))
            },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        RecordingAnimation(
            isRecording = isFullMemorizationRecording,
            onStopRecording = { fullMemorizationViewModel.stopRecording() }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isRepeatListening = MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.REPEAT_LISTENING
        AnswerPlayButton(
            isPlaying = playbackState.isAnswerPlaying,
            repeatCount = answerPlayCount,
            onPlayClick = {
                memorizationController.stopCurrent(coordinator)
                playbackViewModel.playAnswer(currentAnswer)
            },
            onStopClick = { playbackViewModel.stopTts() },
            modifier = if (isRepeatListening) Modifier.fillMaxWidth() else Modifier.weight(1f)
        )

        if (!isRepeatListening) {
            MemorizeLevelPlaybackButton(
                selectedLevel = selectedLevel,
                onPlayEnglishWritingTest = { playbackViewModel.playEnglishWritingTestMergedFile() },
                onStopEnglishWritingTest = { playbackViewModel.stopEnglishWritingTestMergedFile() },
                onPlayFullMemorization = { fullMemorizationViewModel.playRecording() },
                onStopFullMemorization = { fullMemorizationViewModel.stopPlaying() },
                hasEnglishWritingTestMergedFile = playbackState.hasEnglishWritingTestMergedFile,
                isEnglishWritingTestMergedFilePlaying = playbackState.isEnglishWritingTestMergedFilePlaying,
                hasFullMemorizationRecording = hasFullMemorizationRecording,
                isFullMemorizationPlaying = currentMode == CurrentMode.FULL_MEMORIZATION_PLAYING,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
