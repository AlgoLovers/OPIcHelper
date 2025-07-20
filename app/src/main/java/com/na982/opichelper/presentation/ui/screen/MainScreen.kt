package com.na982.opichelper.presentation.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.na982.opichelper.domain.entity.QuestionCategory
import com.na982.opichelper.presentation.viewmodel.MainViewModel
import com.na982.opichelper.presentation.viewmodel.MainUiState
import androidx.compose.ui.platform.LocalContext
import com.na982.opichelper.presentation.ui.component.FlipCard
import com.na982.opichelper.domain.entity.QaItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.BackHandler
import android.media.MediaPlayer
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.ServiceConnection
import android.os.IBinder
import com.na982.opichelper.presentation.ui.component.TtsService
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI.*
import com.na982.opichelper.domain.audio.TtsPlayer

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories

    var isQuestionPlaying by remember { mutableStateOf(false) }
    var isAnswerPlaying by remember { mutableStateOf(false) }
    var isAnswerRepeatPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // TtsService 바인딩 관련
    var ttsService by remember { mutableStateOf<TtsService?>(null) }
    var ttsPlayer by remember { mutableStateOf<TtsPlayer?>(null) }
    // 하이라이트 인덱스 상태 관리
    val questionHighlightIndex by viewModel.questionHighlightIndex.collectAsState()
    val answerHighlightIndex by viewModel.answerHighlightIndex.collectAsState()
    // BroadcastReceiver 등록/해제 (전체 제거)
    // val appContext = context.applicationContext
    // DisposableEffect(Unit) { ... }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
                val binder = service as? TtsService.TtsBinder
                ttsService = binder?.getService()
                ttsPlayer = ttsService // TtsService는 TtsPlayer를 구현함
                ttsService?.setHighlightCallback(object : TtsService.HighlightCallback {
                    override fun onQuestionHighlight(index: Int?) {
                        Log.d("MainScreen", "Question highlight changed to: $index")
                        viewModel.setQuestionHighlightIndex(index)
                        if (index == null) {
                            // 질문 재생이 완료되면 상태 초기화
                            isQuestionPlaying = false
                        }
                    }
                    override fun onAnswerHighlight(index: Int?) {
                        Log.d("MainScreen", "Answer highlight changed to: $index")
                        viewModel.setAnswerHighlightIndex(index)
                        if (index == null) {
                            // 답변 재생이 완료되면 상태 초기화
                            isAnswerPlaying = false
                            isAnswerRepeatPlaying = false
                        }
                    }
                })
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                ttsService?.setHighlightCallback(null)
                ttsService = null
                ttsPlayer = null
            }
        }
    }
    DisposableEffect(Unit) {
        val intent = android.content.Intent(context, TtsService::class.java)
        context.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        onDispose {
            ttsService?.setHighlightCallback(null)
            context.unbindService(serviceConnection)
        }
    }
    // 권한 요청 런처 (녹음 파일 저장용)
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

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

    // 모든 재생 상태를 초기화하는 함수
    fun resetAllPlayStates() {
        Log.d("MainScreen", "Resetting all play states")
        isQuestionPlaying = false
        isAnswerPlaying = false
        isAnswerRepeatPlaying = false
        viewModel.setQuestionHighlightIndex(null)
        viewModel.setAnswerHighlightIndex(null)
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
        // 앱 제목
        AppTitle()
        
        // 카테고리 선택
        CategorySelector(
            selectedCategory = uiState.currentCategory ?: "",
            onCategorySelected = { category ->
                viewModel.selectCategory(category)
            }
        )

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
                    highlightIndex = questionHighlightIndex,
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
                        ttsPlayer = ttsPlayer,
                        onStateChange = { isPlaying ->
                            Log.d("MainScreen", "Question play state changed to: $isPlaying")
                            if (isPlaying) {
                                // 질문 재생 시작 시 다른 모든 상태 초기화
                                resetAllPlayStates()
                                isQuestionPlaying = true
                            } else {
                                isQuestionPlaying = false
                            }
                        },
                        isPlaying = isQuestionPlaying,
                        modifier = Modifier.weight(1f)
                    )
                    
                    RecordingButton(
                        context = context,
                        onStartRecording = {
                            Log.d("MainScreen", "Recording started")
                            viewModel.startRecording()
                        },
                        onStopRecording = {
                            Log.d("MainScreen", "Recording stopped")
                            viewModel.stopRecording()
                            Toast.makeText(context, "녹음을 중지합니다.", Toast.LENGTH_SHORT).show()
                        },
                        onPermissionRequest = {
                            Log.d("MainScreen", "Requesting recording permission")
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 답변 카드
                AnswerCard(
                    currentAnswer = uiState.currentQaItem!!.answerEn,
                    currentAnswerKo = uiState.currentQaItem!!.answerKo,
                    highlightIndex = answerHighlightIndex
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 답변 아래 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerPlayButton(
                        currentAnswer = uiState.currentQaItem!!.answerEn,
                        ttsPlayer = ttsPlayer,
                        onStateChange = { isPlaying ->
                            Log.d("MainScreen", "Answer play state changed to: $isPlaying")
                            if (isPlaying) {
                                // 답변 재생 시작 시 다른 모든 상태 초기화
                                resetAllPlayStates()
                                isAnswerPlaying = true
                            } else {
                                isAnswerPlaying = false
                            }
                        },
                        isPlaying = isAnswerPlaying,
                        modifier = Modifier.weight(1f)
                    )
                    
                    AnswerRepeatPlayButton(
                        currentAnswer = uiState.currentQaItem!!.answerEn,
                        ttsPlayer = ttsPlayer,
                        onStateChange = { isPlaying ->
                            Log.d("MainScreen", "Answer repeat play state changed to: $isPlaying")
                            if (isPlaying) {
                                // 답변 반복 재생 시작 시 다른 모든 상태 초기화
                                resetAllPlayStates()
                                isAnswerRepeatPlaying = true
                            } else {
                                isAnswerRepeatPlaying = false
                            }
                        },
                        isPlaying = isAnswerRepeatPlaying,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 네비게이션 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviousQuestionButton(
                        onPreviousQuestion = {
                            Log.d("MainScreen", "Previous question button clicked")
                            // TTS 중지
                            ttsService?.stopTts()
                            // 모든 상태 초기화
                            resetAllPlayStates()
                            // 이전 질문으로 이동
                            viewModel.previousQaItem()
                            Log.d("MainScreen", "Moved to previous question, all states reset")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    NextQuestionButton(
                        onNextQuestion = {
                            Log.d("MainScreen", "Next question button clicked")
                            // TTS 중지
                            ttsService?.stopTts()
                            // 모든 상태 초기화
                            resetAllPlayStates()
                            // 다음 질문으로 이동
                            viewModel.nextQaItem()
                            Log.d("MainScreen", "Moved to next question, all states reset")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }


    }
    // 리소스 해제
    DisposableEffect(Unit) {
        onDispose {
            Log.d("MainScreen", "Disposing MainScreen resources")
            ttsService?.stopTts()
        }
    }
}

 