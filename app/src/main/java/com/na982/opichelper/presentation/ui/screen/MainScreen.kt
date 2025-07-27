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
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.AppState
import androidx.compose.ui.platform.LocalContext
import com.na982.opichelper.domain.entity.QaItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.BackHandler
import android.util.Log
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.entity.PlaybackState
import com.na982.opichelper.domain.entity.PlayType
import com.na982.opichelper.presentation.viewmodel.MemorizationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.na982.opichelper.presentation.viewmodel.CurrentMode


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // ===== 공통 모드 (기본 UI 상태) =====
    val uiState by viewModel.uiState.collectAsState()
    val memorizationViewModel: MemorizationViewModel = hiltViewModel()
    val memorizeLevels by memorizationViewModel.memorizeLevels.collectAsState()
    val selectedLevel = uiState.selectedMemorizeLevel
    val currentQaItemState = uiState.currentQaItem
    val categories = uiState.categories
    val currentCategoryState = uiState.currentCategory
    // ===== 공통 상태 (모든 모드에서 사용) =====
    val currentMode by memorizationViewModel.currentMode.collectAsState()
    val isQuestionCardFlipped by viewModel.isQuestionCardFlipped.collectAsState()

    // ===== MemorizationViewModel UI 상태 =====
    val memorizationUiState by memorizationViewModel.uiState.collectAsState()

    // ===== 반복 듣기 (Repeat Listening) =====
    val isRepeatListeningCardFlipped = memorizationUiState.isRepeatListeningCardFlipped

    // ===== 영작 테스트 (English Writing) =====
    val hasEnglishWritingTestMergedFile = currentMode == CurrentMode.ENGLISH_WRITING_WITH_FILE
    val englishWritingTestCompleted by memorizationViewModel.englishWritingTestCompleted.collectAsState()
    val isEnglishWritingTestMergedFilePlaying = currentMode == CurrentMode.ENGLISH_WRITING_PLAYING
    val englishWritingTestMergedFileHighlightIndex by viewModel.englishWritingTestMergedFileHighlightIndex.collectAsState()
    val stopEnglishWritingTestMergedFilePlaying by memorizationViewModel.stopEnglishWritingTestMergedFilePlaying.collectAsState()
    val isEnglishWritingTestCardFlipped = memorizationUiState.isEnglishWritingTestCardFlipped

    // ===== 통암기 (Full Memorization) =====
    val fullMemorizationHighlightIndex by memorizationViewModel.fullMemorizationHighlightIndex.collectAsState()
    val isFullMemorizationRecording = memorizationUiState.isFullMemorizationRecording
    val isFullMemorizationPlaying = memorizationUiState.isFullMemorizationPlaying
    val hasFullMemorizationRecording = memorizationUiState.hasFullMemorizationRecording
    val isFullMemorizationRecordingPlaying = memorizationUiState.isFullMemorizationRecordingPlaying

    // ===== 편의 변수들 =====
    val isMemorizeTestRunning = memorizationUiState.isMemorizeTestRunning
    val isFullMemorizationMode = memorizationUiState.isFullMemorizationMode
    val isEnglishWritingTestMode = memorizationUiState.isEnglishWritingTestMode

    // 스크립트 변경 시 영작테스트 병합 파일 확인
    LaunchedEffect(uiState.currentQaItem) {
        viewModel.checkEnglishWritingTestMergedFile()
    }

    // 영작테스트 완료 시 병합 파일 확인
    LaunchedEffect(englishWritingTestCompleted) {
        if (englishWritingTestCompleted) {
            Log.d("MainScreen", "영작테스트 완료 이벤트 감지 - 병합 파일 확인 시작")
            viewModel.checkEnglishWritingTestMergedFile()
            memorizationViewModel.resetEnglishWritingTestCompleted()
            Log.d("MainScreen", "영작테스트 완료 이벤트 처리 완료")
        }
    }

    // 영작테스트 모드 종료 시 병합 파일 확인
    LaunchedEffect(isEnglishWritingTestMode) {
        if (!isEnglishWritingTestMode) {
            Log.d("MainScreen", "영작테스트 모드 종료 - 병합 파일 확인")
            viewModel.checkEnglishWritingTestMergedFile()
        }
    }

    // 영작테스트 녹음 파일 재생 중단 이벤트 처리
    LaunchedEffect(stopEnglishWritingTestMergedFilePlaying) {
        if (stopEnglishWritingTestMergedFilePlaying) {
            Log.d("MainScreen", "영작테스트 녹음 파일 재생 중단 이벤트 감지")
            viewModel.stopEnglishWritingTestMergedFile()
            memorizationViewModel.resetStopEnglishWritingTestMergedFilePlaying()
            Log.d("MainScreen", "영작테스트 녹음 파일 재생 중단 처리 완료")
        }
    }



    // 암기레벨 변경 감지 및 상태 초기화
    LaunchedEffect(selectedLevel) {
        Log.d("MainScreen", "암기레벨 변경 감지: $selectedLevel")
        memorizationViewModel.onMemorizeLevelChanged()
    }

    // 백버튼 시 녹음 종료 (녹음 상태는 RecordingButton에서 관리)
    BackHandler(enabled = false) {
        // 녹음 중지 로직은 RecordingButton에서 처리
    }

    // 현재 카테고리, 인덱스, 전체 개수 계산
    val qaItem = currentQaItemState
    val category = currentCategoryState
    val itemsInCategory = remember(category) {
        category?.let { viewModel.getItemsInCategory(it) } ?: emptyList()
    }
    val currentIndex = remember(category to qaItem) {
        if (category != null && qaItem != null) {
            val index = itemsInCategory.indexOfFirst { it.id == qaItem.id }
            if (index >= 0) index + 1 else 0 // 1-based
        } else 0
    }
    val totalCount = itemsInCategory.size

    Log.d("MainScreen", "Current index: $currentIndex/$totalCount, category: $category")

    // TTS 서비스 바인딩 제거
    // LaunchedEffect(Unit) { ... }
    // DisposableEffect(Unit) { ... }

    // MainViewModel의 암기 테스트 상태 변화를 감지하여 MemorizationViewModel에 알림 (제거)
    // LaunchedEffect(uiState.isMemorizeTestRunning) { ... }
    // MemorizationViewModel의 상태 변화를 MainViewModel에 동기화 (제거)
    // LaunchedEffect(isMemorizeTestRunning) { ... }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 한글 TTS 서비스 정보 표시
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

        // 앱 제목
        AppTitle()

        // 카테고리/암기레벨 선택 영역 (1:1 비율 Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategorySelector(
                selectedCategory = category ?: "",
                categories = categories,
                onCategorySelected = {
                    viewModel.stopAllTts()
                    viewModel.selectCategory(it)
                },
                modifier = Modifier.weight(1f)
            )
            MemorizeLevelSelector(
                levels = memorizeLevels,
                selectedLevel = selectedLevel,
                onLevelSelected = { viewModel.setSelectedMemorizeLevel(it) },
                modifier = Modifier.weight(1f)
            )
        }

        // 질문과 답변 표시
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
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
                // 질문 카드
                QuestionCard(
                    currentQuestion = qaItem.questionEn,
                    currentQuestionKo = qaItem.questionKo,
                    highlightIndex = uiState.questionHighlightIndex,
                    currentIndex = currentIndex,
                    totalCount = totalCount,
                    isFlipped = isQuestionCardFlipped
                )
                
                Log.d("MainScreen", "질문 카드 상태: isFlipped=$isQuestionCardFlipped, isFullMemorizationMode=$isFullMemorizationMode")

                // 질문 카드 바로 아래 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 질문 재생 버튼
                    QuestionPlayButton(
                        currentQuestion = qaItem.questionEn,
                        isPlaying = uiState.isQuestionPlaying,
                        onPlayClick = {
                            // 통암기 모드일 때는 통암기 서비스 시작
                            if (isFullMemorizationMode) {
                                Log.d("MainScreen", "통암기 답변 녹음 버튼 클릭: 현재 질문 카드 상태=$isQuestionCardFlipped")
                                // 질문 카드를 영어로 뒤집기 (TTS 시작 전에 확실히)
                                viewModel.setQuestionCardFlipped(false)
                                Log.d("MainScreen", "통암기 답변 녹음: 질문 카드를 영어로 설정")
                                // 약간의 지연 후 TTS 시작 (카드 뒤집기 애니메이션 완료 대기)
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(300L) // 카드 뒤집기 애니메이션 대기
                                    Log.d("MainScreen", "통암기: TTS 시작")
                                    memorizationViewModel.startFullMemorizationMode()
                                }
                            } else {
                                // 기존 질문 재생 로직
                                memorizationViewModel.stopMemorization()
                                viewModel.playQuestion(qaItem.questionEn)
                            }
                        },
                        onStopClick = {
                            viewModel.stopAllTts()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 암기 테스트 버튼
                    if (isFullMemorizationMode) {
                        // 통암기 모드일 때는 전용 녹음 버튼 사용
                        FullMemorizationRecordingButton(
                            isRecording = isFullMemorizationRecording,
                            onStartRecording = {
                                memorizationViewModel.startFullMemorizationMode()
                            },
                            onStopRecording = {
                                memorizationViewModel.stopFullMemorizationRecording()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 일반 암기 테스트 버튼 (선택된 레벨에 따라 텍스트 변경)
                        Button(
                            onClick = {
                                Log.d("MainScreen", "암기 테스트 버튼 클릭 - selectedLevel: '$selectedLevel'")
                                // 반복듣기 시작 전에 현재 TTS를 중지하고 하이라이트를 제거
                                viewModel.stopAllTts()
                                memorizationViewModel.onMemorizeTestButtonClick(selectedLevel)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when {
                                    isMemorizeTestRunning -> "${selectedLevel} 종료"
                                    selectedLevel == "영작 테스트" -> "부분암기 테스트"
                                    selectedLevel == "통암기" -> "통암기"
                                    else -> selectedLevel.ifEmpty { "암기 테스트" }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 답변 카드 (통암기 모드가 아니거나 녹음 중이 아닐 때만 표시)
                if (!isFullMemorizationMode || !isFullMemorizationRecording) {
                    AnswerCard(
                        currentAnswer = qaItem.answerEn,
                        currentAnswerKo = qaItem.answerKo,
                        highlightIndex = when {
                            isFullMemorizationMode && isFullMemorizationPlaying -> fullMemorizationHighlightIndex
                            isFullMemorizationRecordingPlaying -> fullMemorizationHighlightIndex
                            isEnglishWritingTestMergedFilePlaying -> englishWritingTestMergedFileHighlightIndex
                            else -> uiState.answerHighlightIndex
                        },
                        answerKoHighlightIndex = uiState.answerKoHighlightIndex,
                        recordingHighlightIndex = uiState.recordingHighlightIndex,
                        isFlipped = when {
                            isEnglishWritingTestMode -> isEnglishWritingTestCardFlipped
                            isEnglishWritingTestMergedFilePlaying -> false // 영작테스트 녹음 재생 시에는 영문 카드
                            isRepeatListeningCardFlipped -> isRepeatListeningCardFlipped // 반복듣기 카드 상태
                            else -> uiState.isAnswerCardFlipped
                        },
                        isRepeatListeningCardFlipped = isRepeatListeningCardFlipped
                    )
                } else {
                    // 통암기 모드에서 녹음 중일 때 녹음 애니메이션 표시
                    RecordingAnimation(
                        isRecording = isFullMemorizationRecording,
                        onStopRecording = {
                            memorizationViewModel.stopFullMemorizationRecording()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 답변 아래 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 답변 1회 재생 버튼 (항상 표시)
                    AnswerPlayButton(
                        currentAnswer = qaItem?.answerEn ?: "",
                        isPlaying = uiState.isAnswerPlaying,
                        onPlayClick = {
                            // 반복듣기 등 중단 후 답변 재생
                            memorizationViewModel.stopMemorization()
                            qaItem?.let { viewModel.playAnswer(it.answerEn) }
                        },
                        onStopClick = {
                            viewModel.stopAllTts()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 암기 레벨별 조건부 녹음 재생 버튼
                    Log.d("MainScreen", "암기 레벨별 재생 버튼 조건 확인: selectedLevel='$selectedLevel', hasFullMemorizationRecording=$hasFullMemorizationRecording, hasEnglishWritingTestMergedFile=$hasEnglishWritingTestMergedFile")
                    
                    when (selectedLevel) {
                        "반복 듣기" -> {
                            // 반복듣기는 녹음 재생 버튼 없음 - 답변 버튼이 전체 차지
                            Log.d("MainScreen", "반복듣기 모드 - 녹음 재생 버튼 없음")
                        }
                        "영작 테스트" -> {
                            // 영작테스트 모드에서 병합 파일이 있으면 재생 버튼
                            if (hasEnglishWritingTestMergedFile) {
                                Log.d("MainScreen", "영작테스트 재생 버튼 표시")
                                Button(
                                    onClick = {
                                        if (isEnglishWritingTestMergedFilePlaying) {
                                            viewModel.stopEnglishWritingTestMergedFile()
                                        } else {
                                            viewModel.playEnglishWritingTestMergedFile()
                                        }
                                    },
                                    enabled = hasEnglishWritingTestMergedFile,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isEnglishWritingTestMergedFilePlaying) "재생 중..." else "영작테스트 녹음 재생",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Log.d("MainScreen", "영작테스트 모드이지만 녹음 파일 없음")
                            }
                        }
                        "통암기" -> {
                            // 통암기 모드에서 녹음 파일이 있으면 재생 버튼
                            if (hasFullMemorizationRecording) {
                                Log.d("MainScreen", "통암기 재생 버튼 표시")
                                Button(
                                    onClick = {
                                        if (isFullMemorizationRecordingPlaying) {
                                            memorizationViewModel.stopFullMemorizationPlaying()
                                        } else {
                                            memorizationViewModel.playFullMemorizationRecording()
                                        }
                                    },
                                    enabled = hasFullMemorizationRecording,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isFullMemorizationRecordingPlaying) "재생 중..." else "통암기 녹음 재생",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Log.d("MainScreen", "통암기 모드이지만 녹음 파일 없음")
                            }
                        }
                        else -> {
                            Log.d("MainScreen", "알 수 없는 암기 레벨: '$selectedLevel'")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 네비게이션 섹션
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

 