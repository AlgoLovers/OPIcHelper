package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.QaBrowserViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningViewModel
import com.na982.opichelper.presentation.viewmodel.EnglishWritingTestViewModel
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.presentation.viewmodel.CurrentMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    qaViewModel: QaBrowserViewModel = hiltViewModel(),
    repeatListeningViewModel: RepeatListeningViewModel = hiltViewModel(),
    englishWritingTestViewModel: EnglishWritingTestViewModel = hiltViewModel(),
    fullMemorizationViewModel: FullMemorizationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val playbackState by playbackViewModel.uiState.collectAsState()
    val qaState by qaViewModel.uiState.collectAsState()
    val coordinator = repeatListeningViewModel.modeCoordinator
    val coordinatorMode by coordinator.currentMode.collectAsState()
    val coordinatorRunning by coordinator.isRunning.collectAsState()
    val repeatListeningState by repeatListeningViewModel.uiState.collectAsState()
    val englishWritingTestState by englishWritingTestViewModel.uiState.collectAsState()
    val fullMemorizationState by fullMemorizationViewModel.uiState.collectAsState()

    val memorizeLevels = MemorizeLevel.allDisplayNames
    val selectedLevel = qaState.selectedMemorizeLevel
    val currentQaItemState = qaState.currentQaItem
    val categories = qaState.categories
    val currentCategoryState = qaState.currentCategory

    val isQuestionCardFlipped = false
    val isRepeatListeningCardFlipped = repeatListeningState.isCardFlipped

    val hasEnglishWritingTestMergedFile = playbackState.hasEnglishWritingTestMergedFile
    val englishWritingTestCompleted = englishWritingTestState.completed
    val isEnglishWritingTestMergedFilePlaying = playbackState.isEnglishWritingTestMergedFilePlaying
    val englishWritingTestMergedFileHighlightIndex = playbackState.englishWritingTestMergedFileHighlightIndex
    val stopEnglishWritingTestMergedFilePlaying = englishWritingTestState.stopMergedFilePlaying
    val isEnglishWritingTestCardFlipped = englishWritingTestState.isCardFlipped

    val fullMemorizationHighlightIndex = fullMemorizationState.highlightIndex
    val isFullMemorizationQuestionPlaying = coordinatorMode == CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING
    val isFullMemorizationRecording = coordinatorMode == CurrentMode.FULL_MEMORIZATION_RECORDING
    val isFullMemorizationPlaying = coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING
    val hasFullMemorizationRecording = fullMemorizationState.hasRecordingFile
    val isFullMemorizationRecordingPlaying = coordinatorMode == CurrentMode.FULL_MEMORIZATION_PLAYING

    val isMemorizeTestRunning = coordinatorRunning
    val isFullMemorizationMode = MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION
    val isEnglishWritingTestMode = coordinatorMode in setOf(
        CurrentMode.ENGLISH_WRITING,
        CurrentMode.ENGLISH_WRITING_RECORDING,
        CurrentMode.ENGLISH_WRITING_PLAYING,
        CurrentMode.ENGLISH_WRITING_WITH_FILE
    )

    LaunchedEffect(qaState.currentQaItem) {
        playbackViewModel.checkEnglishWritingTestMergedFile()
    }

    LaunchedEffect(englishWritingTestCompleted) {
        if (englishWritingTestCompleted) {
            playbackViewModel.checkEnglishWritingTestMergedFile()
            englishWritingTestViewModel.resetCompleted()
        }
    }

    LaunchedEffect(isEnglishWritingTestMode) {
        if (!isEnglishWritingTestMode) {
            playbackViewModel.checkEnglishWritingTestMergedFile()
        }
    }

    LaunchedEffect(stopEnglishWritingTestMergedFilePlaying) {
        if (stopEnglishWritingTestMergedFilePlaying) {
            playbackViewModel.stopEnglishWritingTestMergedFile()
            englishWritingTestViewModel.resetStopMergedFilePlaying()
        }
    }

    LaunchedEffect(selectedLevel) {
        repeatListeningViewModel.onLevelChanged()
        englishWritingTestViewModel.onLevelChanged()
        fullMemorizationViewModel.onLevelChanged()
    }

    // 크로스 모드: 영작 녹음 완료 시 통암기 녹음 상태 업데이트
    LaunchedEffect(Unit) {
        englishWritingTestViewModel.onRecordingStateChanged = {
            fullMemorizationViewModel.updateRecordingStatus()
        }
    }

    val isDarkTheme = isSystemInDarkTheme()

    OPicHelperThemeWithMemorizeLevel(
        darkTheme = isDarkTheme,
        memorizeLevel = selectedLevel
    ) {
        BackHandler(enabled = false) {}

        val qaItem = currentQaItemState
        val category = currentCategoryState
        val itemsInCategory = remember(category) {
            category?.let { qaViewModel.getItemsInCategory(it) } ?: emptyList()
        }
        val currentIndex = remember(category to qaItem) {
            if (category != null && qaItem != null) {
                val index = itemsInCategory.indexOfFirst { it.id == qaItem.id }
                if (index >= 0) index + 1 else 0
            } else 0
        }
        val totalCount = itemsInCategory.size

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            AppTitle(
                currentLevel = qaState.currentUserLevel,
                onSettingsClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CategorySelector(
                            selectedCategory = category ?: "",
                            categories = categories,
                            onCategorySelected = {
                                playbackViewModel.stopTts()
                                qaViewModel.selectCategory(it)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📚 학습할 주제를 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        MemorizeLevelSelector(
                            levels = memorizeLevels,
                            selectedLevel = selectedLevel,
                            onLevelSelected = { qaViewModel.setSelectedMemorizeLevel(it) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎯 학습 난이도를 선택하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            when {
                qaState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                qaState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = qaState.error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                qaItem != null -> {
                    QuestionCard(
                        currentQuestion = qaItem.questionEn,
                        currentQuestionKo = qaItem.questionKo,
                        highlightIndex = when {
                            (isFullMemorizationMode && isFullMemorizationPlaying) -> fullMemorizationHighlightIndex
                            else -> playbackState.questionHighlightIndex
                        },
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        isFlipped = isQuestionCardFlipped,
                        currentCategory = category ?: "",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestionPlayButton(
                            currentQuestion = qaItem.questionEn,
                            isPlaying = playbackState.isQuestionPlaying,
                            onPlayClick = {
                                stopCurrentMemorization(selectedLevel, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel, coordinator)
                                playbackViewModel.playQuestion(qaItem.questionEn)
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        if (isFullMemorizationMode) {
                            FullMemorizationRecordingButton(
                                isQuestionPlaying = isFullMemorizationQuestionPlaying,
                                isRecording = isFullMemorizationRecording,
                                onStartRecording = { fullMemorizationViewModel.start() },
                                onStopRecording = { fullMemorizationViewModel.stopRecording() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (MemorizeLevel.fromDisplayName(selectedLevel) != MemorizeLevel.FULL_MEMORIZATION) {
                                        playbackViewModel.stopTts()
                                    }
                                    onMemorizeTestButtonClick(
                                        selectedLevel, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMemorizeTestRunning)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = when {
                                        isMemorizeTestRunning -> "${selectedLevel} 종료"
                                        MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.ENGLISH_WRITING -> "부분암기 테스트"
                                        MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION -> "통암기"
                                        else -> selectedLevel.ifEmpty { "암기 테스트" }
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isFullMemorizationMode || (!isFullMemorizationQuestionPlaying && !isFullMemorizationRecording)) {
                        AnswerCard(
                            currentAnswer = qaViewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = qaViewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = when {
                                (isFullMemorizationMode && isFullMemorizationPlaying) || isFullMemorizationRecordingPlaying -> fullMemorizationHighlightIndex
                                isEnglishWritingTestMergedFilePlaying -> englishWritingTestMergedFileHighlightIndex
                                else -> playbackState.answerHighlightIndex
                            },
                            answerKoHighlightIndex = playbackState.answerKoHighlightIndex,
                            recordingHighlightIndex = playbackState.recordingHighlightIndex,
                            isFlipped = when {
                                isEnglishWritingTestMode -> isEnglishWritingTestCardFlipped
                                isEnglishWritingTestMergedFilePlaying -> false
                                isRepeatListeningCardFlipped -> isRepeatListeningCardFlipped
                                else -> playbackState.isAnswerCardFlipped
                            },
                            isRepeatListeningCardFlipped = isRepeatListeningCardFlipped,
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
                        AnswerPlayButton(
                            currentAnswer = qaViewModel.getCurrentAnswer(qaItem),
                            isPlaying = playbackState.isAnswerPlaying,
                            onPlayClick = {
                                stopCurrentMemorization(selectedLevel, repeatListeningViewModel, englishWritingTestViewModel, fullMemorizationViewModel, coordinator)
                                qaItem.let { playbackViewModel.playAnswer(qaViewModel.getCurrentAnswer(it)) }
                            },
                            onStopClick = { playbackViewModel.stopTts() },
                            modifier = Modifier.weight(1f)
                        )

                        MemorizeLevelPlaybackButton(
                            selectedLevel = selectedLevel,
                            onPlayEnglishWritingTest = { playbackViewModel.playEnglishWritingTestMergedFile() },
                            onStopEnglishWritingTest = { playbackViewModel.stopEnglishWritingTestMergedFile() },
                            onPlayFullMemorization = { fullMemorizationViewModel.playRecording() },
                            onStopFullMemorization = { fullMemorizationViewModel.stopPlaying() },
                            hasEnglishWritingTestMergedFile = hasEnglishWritingTestMergedFile,
                            isEnglishWritingTestMergedFilePlaying = isEnglishWritingTestMergedFilePlaying,
                            hasFullMemorizationRecording = hasFullMemorizationRecording,
                            isFullMemorizationRecordingPlaying = isFullMemorizationRecordingPlaying,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationSection(
                        onPreviousQuestion = {
                            playbackViewModel.stopTts()
                            qaViewModel.previousQaItem()
                        },
                        onNextQuestion = {
                            playbackViewModel.stopTts()
                            qaViewModel.nextQaItem()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    }
}

private fun onMemorizeTestButtonClick(
    selectedLevel: String,
    repeatListeningViewModel: RepeatListeningViewModel,
    englishWritingTestViewModel: EnglishWritingTestViewModel,
    fullMemorizationViewModel: FullMemorizationViewModel
) {
    val level = MemorizeLevel.fromDisplayName(selectedLevel)
    when (level) {
        MemorizeLevel.REPEAT_LISTENING -> {
            if (repeatListeningViewModel.uiState.value.isCardFlipped || true) {
                repeatListeningViewModel.start()
            }
        }
        MemorizeLevel.ENGLISH_WRITING -> {
            englishWritingTestViewModel.start()
        }
        MemorizeLevel.FULL_MEMORIZATION -> {
            fullMemorizationViewModel.start()
        }
    }
}

private fun stopCurrentMemorization(
    selectedLevel: String,
    repeatListeningViewModel: RepeatListeningViewModel,
    englishWritingTestViewModel: EnglishWritingTestViewModel,
    fullMemorizationViewModel: FullMemorizationViewModel,
    coordinator: MemorizationModeCoordinator
) {
    val currentMode = coordinator.currentMode.value
    when {
        currentMode == CurrentMode.REPEAT_LISTENING -> repeatListeningViewModel.stop()
        currentMode in setOf(CurrentMode.ENGLISH_WRITING, CurrentMode.ENGLISH_WRITING_RECORDING,
            CurrentMode.ENGLISH_WRITING_PLAYING, CurrentMode.ENGLISH_WRITING_WITH_FILE) -> englishWritingTestViewModel.stop()
        currentMode in setOf(CurrentMode.FULL_MEMORIZATION, CurrentMode.FULL_MEMORIZATION_QUESTION_PLAYING,
            CurrentMode.FULL_MEMORIZATION_RECORDING, CurrentMode.FULL_MEMORIZATION_PLAYING,
            CurrentMode.FULL_MEMORIZATION_WITH_FILE) -> fullMemorizationViewModel.stop()
    }
}
