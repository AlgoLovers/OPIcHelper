package com.na982.opichelper.presentation.viewmodel

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
    val isFullMemorizationRecordingPlaying: Boolean = false,

    val isMemorizeTestRunning: Boolean = false,

    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false
)
