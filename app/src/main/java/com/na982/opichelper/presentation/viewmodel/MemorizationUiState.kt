package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.MemorizeLevel

data class MemorizationUiState(
    val isRunning: Boolean = false,
    val currentMode: CurrentMode = CurrentMode.NONE,

    val isRepeatListeningCardFlipped: Boolean = false,
    val englishWritingTestCompleted: Boolean = false,
    val stopEnglishWritingTestMergedFilePlaying: Boolean = false,
    val isEnglishWritingTestCardFlipped: Boolean = false,
    val hasFullMemorizationRecordingFile: Boolean = false,
    val isQuestionCardFlipped: Boolean = false,
    val fullMemorizationHighlightIndex: Int? = null,
    val memorizeLevels: List<String> = MemorizeLevel.allDisplayNames
)

val MemorizationUiState.isRepeatListeningRunning: Boolean
    get() = currentMode == CurrentMode.REPEAT_LISTENING && isRunning

val MemorizationUiState.isRepeatListeningMode: Boolean
    get() = currentMode == CurrentMode.REPEAT_LISTENING

val MemorizationUiState.isEnglishWritingTestRunning: Boolean
    get() = currentMode == CurrentMode.ENGLISH_WRITING && isRunning

val MemorizationUiState.isEnglishWritingTestMode: Boolean
    get() = currentMode in setOf(
        CurrentMode.ENGLISH_WRITING,
        CurrentMode.ENGLISH_WRITING_RECORDING,
        CurrentMode.ENGLISH_WRITING_PLAYING,
        CurrentMode.ENGLISH_WRITING_WITH_FILE
    )

val MemorizationUiState.isEnglishWritingTestRecording: Boolean
    get() = currentMode == CurrentMode.ENGLISH_WRITING_RECORDING

val MemorizationUiState.isEnglishWritingTestPlaying: Boolean
    get() = currentMode == CurrentMode.ENGLISH_WRITING_PLAYING

val MemorizationUiState.hasEnglishWritingTestRecording: Boolean
    get() = currentMode == CurrentMode.ENGLISH_WRITING_WITH_FILE

val MemorizationUiState.isFullMemorizationMode: Boolean
    get() = currentMode in setOf(
        CurrentMode.FULL_MEMORIZATION,
        CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
        CurrentMode.FULL_MEMORIZATION_RECORDING,
        CurrentMode.FULL_MEMORIZATION_PLAYING,
        CurrentMode.FULL_MEMORIZATION_WITH_FILE
    )

val MemorizationUiState.isFullMemorizationQuestionPlaying: Boolean
    get() = currentMode == CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING

val MemorizationUiState.isFullMemorizationRecording: Boolean
    get() = currentMode == CurrentMode.FULL_MEMORIZATION_RECORDING

val MemorizationUiState.isFullMemorizationPlaying: Boolean
    get() = currentMode == CurrentMode.FULL_MEMORIZATION_PLAYING

val MemorizationUiState.hasFullMemorizationRecording: Boolean
    get() = currentMode in setOf(
        CurrentMode.FULL_MEMORIZATION_WITH_FILE,
        CurrentMode.FULL_MEMORIZATION_PLAYING
    )

val MemorizationUiState.isFullMemorizationRecordingPlaying: Boolean
    get() = currentMode == CurrentMode.FULL_MEMORIZATION_PLAYING

val MemorizationUiState.isMemorizeTestRunning: Boolean
    get() = isRunning
