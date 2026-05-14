package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.MemorizeLevel

data class MemorizationUiState(
    val isRunning: Boolean = false,
    val currentMode: CurrentMode = CurrentMode.NONE,

    val isRepeatListeningCardFlipped: Boolean = false,
    val isRepeatListeningRunning: Boolean = false,
    val isRepeatListeningMode: Boolean = false,

    val isEnglishWritingTestCardFlipped: Boolean = false,
    val isEnglishWritingTestRunning: Boolean = false,
    val isEnglishWritingTestMode: Boolean = false,
    val isEnglishWritingTestRecording: Boolean = false,
    val isEnglishWritingTestPlaying: Boolean = false,
    val hasEnglishWritingTestRecording: Boolean = false,

    val isFullMemorizationMode: Boolean = false,
    val isFullMemorizationQuestionPlaying: Boolean = false,
    val isFullMemorizationRecording: Boolean = false,
    val isFullMemorizationPlaying: Boolean = false,
    val hasFullMemorizationRecording: Boolean = false,
    val hasFullMemorizationRecordingFile: Boolean = false,
    val isFullMemorizationRecordingPlaying: Boolean = false,

    val isMemorizeTestRunning: Boolean = false,

    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false,

    val isQuestionCardFlipped: Boolean = false,
    val fullMemorizationHighlightIndex: Int? = null,
    val memorizeLevels: List<String> = MemorizeLevel.allDisplayNames
)
