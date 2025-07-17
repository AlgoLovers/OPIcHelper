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
import com.na982.opichelper.presentation.ui.component.SpeechRecognizerHelper
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories

    var isRecording by remember { mutableStateOf(false) }
    var sttText by remember { mutableStateOf("") }
    val context = LocalContext.current
    // TtsService 바인딩 관련
    var ttsService by remember { mutableStateOf<TtsService?>(null) }
    // 하이라이트 인덱스 상태 관리
    var questionHighlightIndex by remember { mutableStateOf<Int?>(null) }
    var answerHighlightIndex by remember { mutableStateOf<Int?>(null) }
    // BroadcastReceiver 등록/해제 (전체 제거)
    // val appContext = context.applicationContext
    // DisposableEffect(Unit) { ... }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
                val binder = service as? TtsService.TtsBinder
                ttsService = binder?.getService()
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                ttsService = null
            }
        }
    }
    DisposableEffect(Unit) {
        val intent = android.content.Intent(context, TtsService::class.java)
        context.bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
        onDispose {
            context.unbindService(serviceConnection)
        }
    }
    // AudioRecorderHelper, MediaPlayer, recordedFilePath 등 제거
    // 권한 요청 런처는 STT에도 필요하므로 유지
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "음성 인식 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
    // SpeechRecognizerHelper를 Compose에서 remember로 관리
    val speechHelper = remember {
        SpeechRecognizerHelper(
            context = context,
            onPartialResult = { sttText = it },
            onFinalResult = { sttText = it },
            onError = { sttText = "음성 인식 오류: $it" }
        )
    }

    // 백버튼 시 녹음 종료
    BackHandler(enabled = isRecording) {
        if (isRecording) {
            speechHelper.stopListening()
            // val file = audioRecorder.stopRecording() // 녹음 파일 저장 로직 제거
            // recordedFilePath = file
            isRecording = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OPic Helper",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.4f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Category Selection
        Text(
            text = "카테고리 선택",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        var expanded by remember { mutableStateOf(false) }
        val selectedCategory = uiState.currentCategory ?: "카테고리 선택"

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            TextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("카테고리") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            viewModel.selectCategory(category)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        // Question & Answer Display
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
                // 질문 카드에 하이라이트 적용
                QuestionCard(
                    qaItem = uiState.currentQaItem!!,
                    currentSentenceIndex = questionHighlightIndex
                )
                Log.d("QuestionCard", "currentSentenceIndex=" + questionHighlightIndex)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ttsService?.speakQuestion(uiState.currentQaItem!!.questionEn, rate = 0.8f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("질문재생")
                    }
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                Toast.makeText(context, "음성 인식 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isRecording) {
                                sttText = ""
                                try {
                                    speechHelper.startListening()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "음성 인식 시작에 실패했습니다: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            } else {
                                speechHelper.stopListening()
                            }
                            isRecording = !isRecording
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (isRecording) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(if (isRecording) "녹음중지" else "녹음시작")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 답변 카드에 하이라이트 적용
                AnswerCard(
                    answerEn = uiState.currentQaItem!!.answerEn,
                    answerKo = uiState.currentQaItem!!.answerKo,
                    currentSentenceIndex = answerHighlightIndex
                )
                Log.d("AnswerCard", "currentSentenceIndex=" + answerHighlightIndex)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ttsService?.speak(uiState.currentQaItem!!.answerEn, rate = 0.8f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("전체 재생")
                    }
                    Button(
                        onClick = {
                            ttsService?.speakBySentence(uiState.currentQaItem!!.answerEn, repeatCount = 5, pauseRatio = 1.5f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("5회 반복")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                ttsService?.stopTts()
                if (isRecording) {
                    speechHelper.stopListening()
                    // val file = audioRecorder.stopRecording() // 녹음 파일 저장 로직 제거
                    // recordedFilePath = file
                    isRecording = false
                }
                // if (isPlaying) { // 녹음 파일 재생 버튼 제거
                //     mediaPlayer.stop()
                //     mediaPlayer.reset()
                //     isPlaying = false
                // }
                viewModel.nextQaItem()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.currentCategory != null
        ) {
            Text("다음 질문")
        }
    }
    // SpeechRecognizerHelper/AudioRecorderHelper/MediaPlayer 리소스 해제 및 앱 종료/이동 시 녹음 종료
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                speechHelper.stopListening()
                // audioRecorder.stopRecording() // 녹음 파일 저장 로직 제거
            }
            // if (isPlaying) { // 녹음 파일 재생 버튼 제거
            //     mediaPlayer.stop()
            //     mediaPlayer.reset()
            // }
            speechHelper.destroy()
            // audioRecorder.release() // 녹음 파일 저장 로직 제거
            // mediaPlayer.release() // 녹음 파일 재생 버튼 제거
        }
    }
}

@Composable
fun QuestionCard(
    qaItem: QaItem,
    currentSentenceIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    Log.d("QuestionCard", "currentSentenceIndex=" + currentSentenceIndex)
    FlipCard(
        modifier = modifier,
        frontContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "질문",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val sentences = qaItem.questionEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                    sentences.forEachIndexed { idx, sentence ->
                        val isHighlighted = currentSentenceIndex == idx
                        Text(
                            text = sentence,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isHighlighted) 24.sp else 18.sp
                            ),
                            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .then(if (isHighlighted) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        backContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "질문",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = qaItem.questionKo,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    )
}

@Composable
fun AnswerCard(
    answerEn: String,
    answerKo: String,
    currentSentenceIndex: Int?,
    modifier: Modifier = Modifier
) {
    Log.d("AnswerCard", "currentSentenceIndex=" + currentSentenceIndex)
    if (answerEn.isNotEmpty() || answerKo.isNotEmpty()) {
        FlipCard(
            modifier = modifier,
            frontContent = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "샘플 답변",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val sentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                        sentences.forEachIndexed { idx, sentence ->
                            val isHighlighted = currentSentenceIndex == idx
                            Text(
                                text = sentence,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isHighlighted) 24.sp else 18.sp
                                ),
                                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .then(if (isHighlighted) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier)
                            )
                        }
                    }
                }
            },
            backContent = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "샘플 답변",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = answerKo,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        )
    }
} 