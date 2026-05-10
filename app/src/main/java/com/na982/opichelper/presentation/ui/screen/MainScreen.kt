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
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.domain.entity.QaItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    memorizationViewModel: MemorizationViewModel? = null,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val memorizationViewModelInstance = memorizationViewModel ?: hiltViewModel<MemorizationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val memorizeLevels by memorizationViewModelInstance.memorizeLevels.collectAsState()
    val selectedLevel = uiState.selectedMemorizeLevel
    val currentQaItemState = uiState.currentQaItem
    val categories = uiState.categories
    val currentCategoryState = uiState.currentCategory

    val currentMode by memorizationViewModelInstance.currentMode.collectAsState()
    val isQuestionCardFlipped by viewModel.isQuestionCardFlipped.collectAsState()

    val memorizationUiState by memorizationViewModelInstance.uiState.collectAsState()

    val isRepeatListeningCardFlipped = memorizationUiState.isRepeatListeningCardFlipped

    val hasEnglishWritingTestMergedFile by viewModel.hasEnglishWritingTestMergedFile.collectAsState()
    val englishWritingTestCompleted by memorizationViewModelInstance.englishWritingTestCompleted.collectAsState()
    val isEnglishWritingTestMergedFilePlaying by viewModel.isEnglishWritingTestMergedFilePlaying.collectAsState()
    val englishWritingTestMergedFileHighlightIndex by viewModel.englishWritingTestMergedFileHighlightIndex.collectAsState()
    val stopEnglishWritingTestMergedFilePlaying by memorizationViewModelInstance.stopEnglishWritingTestMergedFilePlaying.collectAsState()
    val isEnglishWritingTestCardFlipped = memorizationUiState.isEnglishWritingTestCardFlipped

    val fullMemorizationHighlightIndex by memorizationViewModelInstance.fullMemorizationHighlightIndex.collectAsState()
    val isFullMemorizationQuestionPlaying = memorizationUiState.isFullMemorizationQuestionPlaying
    val isFullMemorizationRecording = memorizationUiState.isFullMemorizationRecording
    val isFullMemorizationPlaying = memorizationUiState.isFullMemorizationPlaying
    val hasFullMemorizationRecording = memorizationUiState.hasFullMemorizationRecording
    val isFullMemorizationRecordingPlaying = memorizationUiState.isFullMemorizationRecordingPlaying

    val isMemorizeTestRunning = memorizationUiState.isMemorizeTestRunning
    val isFullMemorizationMode = memorizationUiState.isFullMemorizationMode
    val isEnglishWritingTestMode = memorizationUiState.isEnglishWritingTestMode

    LaunchedEffect(Unit) {
        memorizationViewModelInstance.resetStateOnAppRestart()
    }

    LaunchedEffect(uiState.currentQaItem) {
        viewModel.checkEnglishWritingTestMergedFile()
    }

    LaunchedEffect(englishWritingTestCompleted) {
        if (englishWritingTestCompleted) {
            viewModel.checkEnglishWritingTestMergedFile()
            memorizationViewModelInstance.resetEnglishWritingTestCompleted()
        }
    }

    LaunchedEffect(isEnglishWritingTestMode) {
        if (!isEnglishWritingTestMode) {
            viewModel.checkEnglishWritingTestMergedFile()
        }
    }

    LaunchedEffect(stopEnglishWritingTestMergedFilePlaying) {
        if (stopEnglishWritingTestMergedFilePlaying) {
            viewModel.stopEnglishWritingTestMergedFile()
            memorizationViewModelInstance.resetStopEnglishWritingTestMergedFilePlaying()
        }
    }

    LaunchedEffect(selectedLevel) {
        memorizationViewModelInstance.onMemorizeLevelChanged()
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
            category?.let { viewModel.getItemsInCategory(it) } ?: emptyList()
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
            if (uiState.currentKoreanTtsService.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "한글 TTS",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = uiState.currentKoreanTtsService,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AppTitle(
                currentLevel = uiState.currentUserLevel,
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
                                viewModel.stopAllTts()
                                viewModel.selectCategory(it)
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
                            onLevelSelected = { viewModel.setSelectedMemorizeLevel(it) }
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
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                uiState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.error!!,
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
                            else -> uiState.questionHighlightIndex
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
                            isPlaying = uiState.isQuestionPlaying,
                            onPlayClick = {
                                memorizationViewModelInstance.stopMemorization()
                                viewModel.playQuestion(qaItem.questionEn)
                            },
                            onStopClick = { viewModel.stopAllTts() },
                            modifier = Modifier.weight(1f)
                        )

                        if (isFullMemorizationMode) {
                            FullMemorizationRecordingButton(
                                isQuestionPlaying = isFullMemorizationQuestionPlaying,
                                isRecording = isFullMemorizationRecording,
                                onStartRecording = { memorizationViewModelInstance.startFullMemorizationMode() },
                                onStopRecording = { memorizationViewModelInstance.stopFullMemorizationRecording() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (MemorizeLevel.fromDisplayName(selectedLevel) != MemorizeLevel.FULL_MEMORIZATION) {
                                        viewModel.stopAllTts()
                                    }
                                    memorizationViewModelInstance.onMemorizeTestButtonClick(selectedLevel)
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
                            currentAnswer = viewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = viewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = when {
                                (isFullMemorizationMode && isFullMemorizationPlaying) || isFullMemorizationRecordingPlaying -> fullMemorizationHighlightIndex
                                isEnglishWritingTestMergedFilePlaying -> englishWritingTestMergedFileHighlightIndex
                                else -> uiState.answerHighlightIndex
                            },
                            answerKoHighlightIndex = uiState.answerKoHighlightIndex,
                            recordingHighlightIndex = uiState.recordingHighlightIndex,
                            isFlipped = when {
                                isEnglishWritingTestMode -> isEnglishWritingTestCardFlipped
                                isEnglishWritingTestMergedFilePlaying -> false
                                isRepeatListeningCardFlipped -> isRepeatListeningCardFlipped
                                else -> uiState.isAnswerCardFlipped
                            },
                            isRepeatListeningCardFlipped = isRepeatListeningCardFlipped,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        RecordingAnimation(
                            isRecording = isFullMemorizationRecording,
                            onStopRecording = { memorizationViewModelInstance.stopFullMemorizationRecording() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnswerPlayButton(
                            currentAnswer = viewModel.getCurrentAnswer(qaItem),
                            isPlaying = uiState.isAnswerPlaying,
                            onPlayClick = {
                                memorizationViewModelInstance.stopMemorization()
                                qaItem.let { viewModel.playAnswer(viewModel.getCurrentAnswer(it)) }
                            },
                            onStopClick = { viewModel.stopAllTts() },
                            modifier = Modifier.weight(1f)
                        )

                        MemorizeLevelPlaybackButton(
                            selectedLevel = selectedLevel,
                            mainViewModel = viewModel,
                            memorizationViewModel = memorizationViewModelInstance,
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
                            viewModel.stopAllTts()
                            viewModel.previousQaItem()
                        },
                        onNextQuestion = {
                            viewModel.stopAllTts()
                            viewModel.nextQaItem()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
}
