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
import com.na982.opichelper.presentation.viewmodel.MainUiState
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


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 백버튼 시 녹음 종료 (녹음 상태는 RecordingButton에서 관리)
    BackHandler(enabled = false) {
        // 녹음 중지 로직은 RecordingButton에서 처리
    }

    // 현재 카테고리, 인덱스, 전체 개수 계산
    val currentCategory = uiState.currentCategory
    val currentQaItem = uiState.currentQaItem
    val itemsInCategory = remember(currentCategory) {
        if (currentCategory != null) viewModel.getItemsInCategory(currentCategory) else emptyList()
    }
    val currentIndex = remember(currentCategory to currentQaItem) {
        if (currentCategory != null && currentQaItem != null) {
            val index = itemsInCategory.indexOfFirst { it.id == currentQaItem.id }
            if (index >= 0) index + 1 else 0 // 1-based
        } else 0
    }
    val totalCount = itemsInCategory.size

    Log.d("MainScreen", "Current index: $currentIndex/$totalCount, category: $currentCategory")

    // TTS 서비스 바인딩
    LaunchedEffect(Unit) {
        viewModel.bindTtsService(context) { serviceName ->
            viewModel.updateKoreanTtsServiceName(serviceName)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unbindTtsService(context)
        }
    }

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
                selectedCategory = uiState.currentCategory ?: "",
                categories = listOf(
                    "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷",
                    "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"
                ),
                onCategorySelected = {
                    viewModel.stopAllTts()
                    viewModel.selectCategory(it)
                },
                modifier = Modifier.weight(1f)
            )
            MemorizeLevelSelector(
                levels = uiState.memorizeLevels,
                selectedLevel = uiState.selectedMemorizeLevel,
                onLevelSelected = { viewModel.setMemorizeLevel(it) },
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
            uiState.currentQaItem != null -> {
                // 질문 카드
                QuestionCard(
                    currentQuestion = uiState.currentQaItem!!.questionEn,
                    currentQuestionKo = uiState.currentQaItem!!.questionKo,
                    highlightIndex = uiState.questionHighlightIndex,
                    currentIndex = currentIndex,
                    totalCount = totalCount
                )

                // 질문 카드 바로 아래 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuestionPlayButton(
                        currentQuestion = uiState.currentQaItem!!.questionEn,
                        isPlaying = uiState.isQuestionPlaying,
                        onPlayClick = {
                            viewModel.playQuestion(uiState.currentQaItem!!.questionEn)
                        },
                        onStopClick = {
                            viewModel.stopQuestion()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 암기 테스트 버튼
                    if (uiState.isFullMemorizationMode) {
                        // 통암기 모드일 때는 전용 녹음 버튼 사용
                        FullMemorizationRecordingButton(
                            isRecording = uiState.isFullMemorizationRecording,
                            onStartRecording = {
                                viewModel.startFullMemorizationMode()
                            },
                            onStopRecording = {
                                viewModel.stopFullMemorizationRecording()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 일반 암기 테스트 버튼
                        Button(
                            onClick = {
                                viewModel.onMemorizeTestButtonClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when {
                                    uiState.isMemorizeTestRunning -> "암기 테스트 종료"
                                    uiState.hasProgress -> "암기 테스트 재개"
                                    else -> "암기 테스트"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 답변 카드 (통암기 모드가 아니거나 녹음 중이 아닐 때만 표시)
                if (!uiState.isFullMemorizationMode || !uiState.isFullMemorizationRecording) {
                    AnswerCard(
                        currentAnswer = uiState.currentQaItem!!.answerEn,
                        currentAnswerKo = uiState.currentQaItem!!.answerKo,
                        highlightIndex = if (uiState.isFullMemorizationMode && uiState.isFullMemorizationPlaying) uiState.fullMemorizationHighlightIndex else uiState.answerHighlightIndex,
                        answerKoHighlightIndex = uiState.answerKoHighlightIndex,
                        recordingHighlightIndex = uiState.recordingHighlightIndex,
                        isFlipped = uiState.isAnswerCardFlipped
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 답변 아래 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerPlayButton(
                        currentAnswer = uiState.currentQaItem!!.answerEn,
                        isPlaying = uiState.isAnswerPlaying,
                        onPlayClick = {
                            viewModel.playAnswer(uiState.currentQaItem!!.answerEn)
                        },
                        onStopClick = {
                            viewModel.stopAnswer()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 통암기 모드일 때 녹음 재생 버튼
                    if (uiState.isFullMemorizationMode) {
                        if (uiState.hasFullMemorizationRecording) {
                            Button(
                                onClick = {
                                    viewModel.playFullMemorizationRecording()
                                },
                                enabled = !uiState.isFullMemorizationPlaying,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (uiState.isFullMemorizationPlaying) "재생 중..." else "녹음 재생",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            // 녹음 파일이 없을 때는 빈 공간
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    } else {
                        // 병합된 오디오 파일 재생 버튼 (영작 테스트 레벨이고 녹음 파일이 있을 때만 활성화)
                        Button(
                            onClick = {
                                viewModel.playMergedAudioFile()
                            },
                            enabled = uiState.selectedMemorizeLevel == "영작 테스트" && uiState.hasRecordingFile && !uiState.isMergedAudioPlaying,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (uiState.isMergedAudioPlaying) "재생 중..." else "녹음 재생",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
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

 