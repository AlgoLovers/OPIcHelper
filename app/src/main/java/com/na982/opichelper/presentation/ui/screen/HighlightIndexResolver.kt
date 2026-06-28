package com.na982.opichelper.presentation.ui.screen

import com.na982.opichelper.domain.entity.ModeGroup
import com.na982.opichelper.presentation.viewmodel.PlaybackState

fun resolveQuestionHighlightIndex(
    coordinatorGroup: ModeGroup,
    isFullMemorizationPlaying: Boolean,
    fullMemorizationHighlightIndex: Int?,
    playbackQuestionHighlightIndex: Int?
): Int? = when {
    coordinatorGroup == ModeGroup.FULL_MEMORIZATION && isFullMemorizationPlaying -> fullMemorizationHighlightIndex
    else -> playbackQuestionHighlightIndex
}

fun resolveAnswerHighlightIndex(
    coordinatorGroup: ModeGroup,
    isFullMemorizationPlaying: Boolean,
    fullMemorizationHighlightIndex: Int?,
    playbackState: PlaybackState
): Int? = when {
    coordinatorGroup == ModeGroup.FULL_MEMORIZATION && isFullMemorizationPlaying -> fullMemorizationHighlightIndex
    playbackState.isEnglishWritingTestMergedFilePlaying -> playbackState.englishWritingTestMergedFileHighlightIndex
    else -> playbackState.answerHighlight.index
}
