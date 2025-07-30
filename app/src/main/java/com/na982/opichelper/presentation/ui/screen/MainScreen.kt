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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.na982.opichelper.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    memorizationViewModel: MemorizationViewModel? = null,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    // ===== 공통 모드 (기본 UI 상태) =====
    val uiState by viewModel.uiState.collectAsState()
    val memorizationViewModelInstance = memorizationViewModel ?: hiltViewModel<MemorizationViewModel>()
    val memorizeLevels by memorizationViewModelInstance.memorizeLevels.collectAsState()
    val selectedLevel = uiState.selectedMemorizeLevel
    val currentQaItemState = uiState.currentQaItem
    val categories = uiState.categories
    val currentCategoryState = uiState.currentCategory

    // ===== 공통 상태 (모든 모드에서 사용) =====
    val currentMode by memorizationViewModelInstance.currentMode.collectAsState()
    val isQuestionCardFlipped by viewModel.isQuestionCardFlipped.collectAsState()

    // ===== MemorizationViewModel UI 상태 =====
    val memorizationUiState by memorizationViewModelInstance.uiState.collectAsState()

    // ===== 반복 듣기 (Repeat Listening) =====
    val isRepeatListeningCardFlipped = memorizationUiState.isRepeatListeningCardFlipped

    // ===== 영작 테스트 (English Writing) =====
    val hasEnglishWritingTestMergedFile by viewModel.hasEnglishWritingTestMergedFile.collectAsState()
    val englishWritingTestCompleted by memorizationViewModelInstance.englishWritingTestCompleted.collectAsState()
    val isEnglishWritingTestMergedFilePlaying by viewModel.isEnglishWritingTestMergedFilePlaying.collectAsState()
    val englishWritingTestMergedFileHighlightIndex by viewModel.englishWritingTestMergedFileHighlightIndex.collectAsState()
    val stopEnglishWritingTestMergedFilePlaying by memorizationViewModelInstance.stopEnglishWritingTestMergedFilePlaying.collectAsState()
    val isEnglishWritingTestCardFlipped = memorizationUiState.isEnglishWritingTestCardFlipped

    // ===== 통암기 (Full Memorization) =====
    val fullMemorizationHighlightIndex by memorizationViewModelInstance.fullMemorizationHighlightIndex.collectAsState()
    val isFullMemorizationQuestionPlaying = memorizationUiState.isFullMemorizationQuestionPlaying
    val isFullMemorizationRecording = memorizationUiState.isFullMemorizationRecording
    val isFullMemorizationPlaying = memorizationUiState.isFullMemorizationPlaying
    val hasFullMemorizationRecording = memorizationUiState.hasFullMemorizationRecording
    val isFullMemorizationRecordingPlaying = memorizationUiState.isFullMemorizationRecordingPlaying

    // ===== 편의 변수들 =====
    val isMemorizeTestRunning = memorizationUiState.isMemorizeTestRunning
    val isFullMemorizationMode = memorizationUiState.isFullMemorizationMode
    val isEnglishWritingTestMode = memorizationUiState.isEnglishWritingTestMode

    // 앱 재시작 시 MemorizationViewModel 상태 초기화
    LaunchedEffect(Unit) {
        Log.d("MainScreen", "MainScreen 시작 - MemorizationViewModel 상태 초기화")
        memorizationViewModelInstance.resetStateOnAppRestart()
    }
    
    // 스크립트 변경 시 영작테스트 병합 파일 확인
    LaunchedEffect(uiState.currentQaItem) {
        Log.d("MainScreen", "currentQaItem 변경 감지: ${uiState.currentQaItem?.category}")
        viewModel.checkEnglishWritingTestMergedFile()
    }

    // 영작테스트 완료 시 병합 파일 확인
    LaunchedEffect(englishWritingTestCompleted) {
        if (englishWritingTestCompleted) {
            Log.d("MainScreen", "영작테스트 완료 이벤트 감지 - 병합 파일 확인 시작")
            delay(500L) // 파일 시스템 동기화 추가 대기
            viewModel.checkEnglishWritingTestMergedFile()
            memorizationViewModelInstance.resetEnglishWritingTestCompleted()
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
            memorizationViewModelInstance.resetStopEnglishWritingTestMergedFilePlaying()
            Log.d("MainScreen", "영작테스트 녹음 파일 재생 중단 처리 완료")
        }
    }



    // 암기레벨 변경 감지 및 상태 초기화
    LaunchedEffect(selectedLevel) {
        Log.d("MainScreen", "암기레벨 변경 감지: $selectedLevel")
        memorizationViewModelInstance.onMemorizeLevelChanged()
    }

    // 다크 테마 감지
    val isDarkTheme = isSystemInDarkTheme()

    // 암기레벨별 동적 테마 적용
    OPicHelperThemeWithMemorizeLevel(
        darkTheme = isDarkTheme,
        memorizeLevel = selectedLevel
    ) {
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

            // 앱 제목 (설정 버튼 포함)
            AppTitle(
                currentLevel = uiState.currentUserLevel,
                onSettingsClick = {
                    Log.d("MainScreen", "AppTitle 내 설정 버튼 클릭됨")
                    onSettingsClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 카테고리/암기레벨 선택 영역 (1:1 비율 Row)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 카테고리 선택기 + 설명
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 암기레벨 선택기 + 설명
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
                        highlightIndex = fullMemorizationHighlightIndex,
                        currentIndex = currentIndex,
                        totalCount = totalCount,
                        isFlipped = isQuestionCardFlipped,
                        currentCategory = category ?: "",
                        modifier = Modifier.fillMaxWidth()
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
                                        memorizationViewModelInstance.startFullMemorizationMode()
                                    }
                                } else {
                                    // 기존 질문 재생 로직
                                    memorizationViewModelInstance.stopMemorization()
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
                                isQuestionPlaying = isFullMemorizationQuestionPlaying,
                                isRecording = isFullMemorizationRecording,
                                onStartRecording = {
                                    memorizationViewModelInstance.startFullMemorizationMode()
                                },
                                onStopRecording = {
                                    memorizationViewModelInstance.stopFullMemorizationRecording()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // 일반 암기 테스트 버튼 (선택된 레벨에 따라 텍스트 변경)
                            Button(
                                onClick = {
                                    Log.d("MainScreen", "암기 테스트 버튼 클릭 - selectedLevel: '$selectedLevel'")
                                    // 통암기 모드가 아닐 때만 TTS 중지
                                    if (selectedLevel != "통암기") {
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
                                        selectedLevel == "영작 테스트" -> "부분암기 테스트"
                                        selectedLevel == "통암기" -> "통암기"
                                        else -> selectedLevel.ifEmpty { "암기 테스트" }
                                    },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 답변 카드 (통암기 모드에서 질문 재생 중이거나 녹음 중일 때는 숨김)
                    if (!isFullMemorizationMode || (!isFullMemorizationQuestionPlaying && !isFullMemorizationRecording)) {
                        AnswerCard(
                            currentAnswer = viewModel.getCurrentAnswer(qaItem),
                            currentAnswerKo = viewModel.getCurrentAnswerKo(qaItem),
                            highlightIndex = when {
                                isFullMemorizationMode && isFullMemorizationPlaying -> {
                                    Log.d("MainScreen", "통암기 TTS 하이라이트: $fullMemorizationHighlightIndex")
                                    fullMemorizationHighlightIndex
                                }
                                isFullMemorizationRecordingPlaying -> {
                                    Log.d("MainScreen", "통암기 녹음 재생 하이라이트: $fullMemorizationHighlightIndex")
                                    fullMemorizationHighlightIndex
                                }
                                isEnglishWritingTestMergedFilePlaying -> {
                                    Log.d("MainScreen", "영작테스트 녹음 재생 하이라이트: $englishWritingTestMergedFileHighlightIndex")
                                    englishWritingTestMergedFileHighlightIndex
                                }
                                else -> {
                                    Log.d("MainScreen", "기본 하이라이트: ${uiState.answerHighlightIndex}")
                                    uiState.answerHighlightIndex
                                }
                            },
                            answerKoHighlightIndex = uiState.answerKoHighlightIndex,
                            recordingHighlightIndex = uiState.recordingHighlightIndex,
                            isFlipped = when {
                                isEnglishWritingTestMode -> isEnglishWritingTestCardFlipped
                                isEnglishWritingTestMergedFilePlaying -> false // 영작테스트 녹음 재생 시에는 영문 카드
                                isRepeatListeningCardFlipped -> isRepeatListeningCardFlipped // 반복듣기 카드 상태
                                else -> uiState.isAnswerCardFlipped
                            },
                            isRepeatListeningCardFlipped = isRepeatListeningCardFlipped,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // 통암기 모드에서 녹음 중일 때 녹음 애니메이션 표시
                        RecordingAnimation(
                            isRecording = isFullMemorizationRecording,
                            onStopRecording = {
                                memorizationViewModelInstance.stopFullMemorizationRecording()
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
                            currentAnswer = viewModel.getCurrentAnswer(qaItem),
                            isPlaying = uiState.isAnswerPlaying,
                            onPlayClick = {
                                // 반복듣기 등 중단 후 답변 재생
                                memorizationViewModelInstance.stopMemorization()
                                qaItem?.let { viewModel.playAnswer(viewModel.getCurrentAnswer(it)) }
                            },
                            onStopClick = {
                                viewModel.stopAllTts()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 암기 레벨별 조건부 녹음 재생 버튼
                        Log.d("MainScreen", "암기 레벨별 재생 버튼 조건 확인: selectedLevel='$selectedLevel', hasFullMemorizationRecording=$hasFullMemorizationRecording, hasEnglishWritingTestMergedFile=$hasEnglishWritingTestMergedFile, isEnglishWritingTestMergedFilePlaying=$isEnglishWritingTestMergedFilePlaying, englishWritingTestMergedFileHighlightIndex=$englishWritingTestMergedFileHighlightIndex")

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
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEnglishWritingTestMergedFilePlaying)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        ),
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
                                                memorizationViewModelInstance.stopFullMemorizationPlaying()
                                            } else {
                                                memorizationViewModelInstance.playFullMemorizationRecording()
                                            }
                                        },
                                        enabled = hasFullMemorizationRecording,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFullMemorizationRecordingPlaying)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        ),
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
}
}
