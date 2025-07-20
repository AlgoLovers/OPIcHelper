# 주요 컴포넌트 상세 가이드

## 개요

이 문서는 OPIC Helper 앱의 주요 컴포넌트들에 대한 상세한 기술 가이드입니다. 각 컴포넌트의 역할, 책임, 사용법, 그리고 개발자가 잘 모르는 사람도 이해할 수 있도록 자세히 설명합니다.

## 1. TtsService (Text-to-Speech 서비스)

### 개요
TtsService는 영어 텍스트를 음성으로 변환하는 Android 서비스입니다. 사용자가 영어 질문과 답변을 들을 수 있도록 도와주며, 문장별 하이라이트 기능을 제공합니다.

### 위치
`app/src/main/java/com/na982/opichelper/presentation/ui/component/TtsService.kt`

### 주요 책임
- 영어 텍스트를 음성으로 변환
- 문장별 하이라이트 콜백 제공
- 오디오 포커스 관리
- 포그라운드 서비스 실행
- 배터리 최적화 및 Wake Lock 관리

### 시작점과 종료점

#### 시작점
```kotlin
// MainScreen에서 서비스 바인딩
val serviceConnection = remember {
    object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TtsService.TtsBinder
            ttsService = binder?.getService()
        }
    }
}

// 서비스 시작
val intent = Intent(context, TtsService::class.java)
context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
```

#### 종료점
```kotlin
// MainScreen에서 서비스 해제
DisposableEffect(Unit) {
    onDispose {
        ttsService?.setHighlightCallback(null)
        context.unbindService(serviceConnection)
    }
}
```

### 핵심 메서드

#### 1. speakQuestion()
```kotlin
fun speakQuestion(text: String, rate: Float = 0.8f)
```
**목적**: 질문 텍스트를 음성으로 재생
**매개변수**:
- `text`: 영어 질문 텍스트
- `rate`: 재생 속도 (0.8f = 80% 속도)

**사용 예시**:
```kotlin
ttsService?.speakQuestion("What do you like to do in your free time?", 0.8f)
```

#### 2. speakAnswer()
```kotlin
fun speakAnswer(text: String, rate: Float = 0.8f)
```
**목적**: 답변 텍스트를 음성으로 재생
**매개변수**:
- `text`: 영어 답변 텍스트
- `rate`: 재생 속도

**사용 예시**:
```kotlin
ttsService?.speakAnswer("I like to read books and watch movies.", 0.8f)
```

#### 3. speakBySentence()
```kotlin
fun speakBySentence(text: String, repeatCount: Int = 5, pauseRatio: Float = 1.5f, rate: Float = 0.8f)
```
**목적**: 문장별로 반복 재생 (학습용)
**매개변수**:
- `text`: 영어 텍스트
- `repeatCount`: 반복 횟수
- `pauseRatio`: 문장 간 일시정지 비율
- `rate`: 재생 속도

**사용 예시**:
```kotlin
ttsService?.speakBySentence("I like to read books. I also enjoy watching movies.", 3, 2.0f, 0.7f)
```

#### 4. stopTts()
```kotlin
fun stopTts()
```
**목적**: 현재 재생 중인 TTS 중지

#### 5. setHighlightCallback()
```kotlin
fun setHighlightCallback(callback: HighlightCallback?)
```
**목적**: 문장별 하이라이트 콜백 설정

### 하이라이트 콜백 인터페이스
```kotlin
interface HighlightCallback {
    fun onQuestionHighlight(index: Int?)  // 질문 문장 인덱스
    fun onAnswerHighlight(index: Int?)    // 답변 문장 인덱스
}
```

### 내부 동작 과정

#### 1. 초기화
```kotlin
override fun onCreate() {
    super.onCreate()
    tts = TextToSpeech(this, this)  // TTS 엔진 초기화
    createNotificationChannel()      // 알림 채널 생성
}
```

#### 2. TTS 준비 완료
```kotlin
override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.US  // 영어 설정
        isReady = true
    }
}
```

#### 3. 음성 재생
```kotlin
fun speak(text: String, rate: Float = 0.8f, mode: String = "question") {
    if (!isReady) return
    
    speakJob?.cancel()  // 이전 재생 중지
    if (requestAudioFocus()) {  // 오디오 포커스 요청
        acquireWakeLock()       // Wake Lock 획득
        registerNoisyReceiver() // 노이즈 리시버 등록
        
        speakJob = CoroutineScope(Dispatchers.Main).launch {
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            sentences.forEachIndexed { index, sentence ->
                highlightCallback?.let { callback ->
                    when (mode) {
                        "question" -> callback.onQuestionHighlight(index)
                        "answer" -> callback.onAnswerHighlight(index)
                    }
                }
                
                // 문장 재생
                val utteranceId = "utterance_$index"
                tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                
                // 일시정지
                delay((sentence.length * 100 * pauseRatio).toLong())
            }
        }
    }
}
```

### 오디오 포커스 관리
```kotlin
private fun requestAudioFocus(): Boolean {
    audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
            
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> stopTts()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseTts()
                    AudioManager.AUDIOFOCUS_GAIN -> resumeTts()
                }
            }
            .build()
            
        return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
        @Suppress("DEPRECATION")
        return audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}
```

### 포그라운드 서비스
```kotlin
private fun startForegroundService() {
    val notification = buildNotification()
    startForeground(NOTIFICATION_ID, notification)
}

private fun buildNotification(): Notification {
    val channelId = CHANNEL_ID
    val channelName = "TTS Service"
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    return NotificationCompat.Builder(this, channelId)
        .setContentTitle("OPIC Helper")
        .setContentText("TTS 재생 중...")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}
```

## 2. SpeechRecognizerHelper (음성 인식 헬퍼)

### 개요
SpeechRecognizerHelper는 사용자의 음성을 텍스트로 변환하는 컴포넌트입니다. 실시간 음성 인식을 제공하며, 부분 결과와 최종 결과를 콜백으로 전달합니다.

### 위치
`app/src/main/java/com/na982/opichelper/presentation/ui/component/SpeechRecognizerHelper.kt`

### 주요 책임
- 실시간 음성 인식
- 부분 결과 및 최종 결과 처리
- 에러 처리 및 사용자 피드백
- RecognitionCallback 인터페이스를 통한 결과 전달

### 시작점과 종료점

#### 시작점
```kotlin
// MainScreen에서 초기화
val speechHelper = remember {
    SpeechRecognizerHelper(context).apply {
        recognitionCallback = object : RecognitionCallback {
            override fun onPartialResult(text: String) { sttText = text }
            override fun onFinalResult(text: String) { sttText = text }
            override fun onError(error: String) { sttText = "음성 인식 오류: $error" }
        }
    }
}
```

#### 종료점
```kotlin
// MainScreen에서 정리
DisposableEffect(Unit) {
    onDispose {
        speechHelper.destroy()
    }
}
```

### 핵심 메서드

#### 1. startListening()
```kotlin
fun startListening()
```
**목적**: 음성 인식 시작
**동작 과정**:
1. 권한 확인
2. SpeechRecognizer 초기화
3. 인텐트 설정 (한국어, 실시간 인식)
4. 음성 인식 시작

#### 2. stopListening()
```kotlin
fun stopListening()
```
**목적**: 음성 인식 중지

#### 3. destroy()
```kotlin
fun destroy()
```
**목적**: 리소스 정리 및 SpeechRecognizer 해제

### RecognitionCallback 인터페이스
```kotlin
interface RecognitionCallback {
    fun onPartialResult(text: String)  // 실시간 부분 결과
    fun onFinalResult(text: String)    // 최종 인식 결과
    fun onError(error: String)         // 에러 메시지
}
```

### 내부 동작 과정

#### 1. 초기화
```kotlin
class SpeechRecognizerHelper(
    private val context: Context,
    var recognitionCallback: RecognitionCallback? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    init {
        initializeSpeechRecognizer()
    }
}
```

#### 2. SpeechRecognizer 초기화
```kotlin
private fun initializeSpeechRecognizer() {
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                recognitionCallback?.onFinalResult(matches?.firstOrNull() ?: "")
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                recognitionCallback?.onPartialResult(matches?.firstOrNull() ?: "")
            }
            
            override fun onError(error: Int) {
                isListening = false
                recognitionCallback?.onError(errorToMessage(error))
            }
            
            // 기타 콜백 메서드들...
        })
    }
}
```

#### 3. 음성 인식 시작
```kotlin
fun startListening() {
    if (isListening) return
    
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")  // 한국어
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)  // 부분 결과 활성화
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    
    speechRecognizer?.startListening(intent)
}
```

#### 4. 에러 처리
```kotlin
private fun errorToMessage(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
        SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
        SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
        SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식할 수 없습니다"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 사용 중입니다"
        SpeechRecognizer.ERROR_SERVER -> "서버 에러"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 시간 초과"
        else -> "알 수 없는 에러"
    }
}
```

### 권한 처리
```kotlin
// MainScreen에서 권한 확인
val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (!isGranted) {
        Toast.makeText(context, "음성 인식 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }
}

// 권한 확인 후 음성 인식 시작
Button(
    onClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return@Button
        }
        
        if (!isRecording) {
            speechHelper.startListening()
        } else {
            speechHelper.stopListening()
        }
        isRecording = !isRecording
    }
) {
    Text(if (isRecording) "녹음중지" else "녹음시작")
}
```

## 3. FlipCard (카드 뒤집기 컴포넌트)

### 개요
FlipCard는 질문과 답변을 3D 카드 뒤집기 애니메이션으로 표시하는 재사용 가능한 Compose 컴포넌트입니다.

### 위치
`app/src/main/java/com/na982/opichelper/presentation/ui/component/FlipCard.kt`

### 주요 책임
- 3D 카드 뒤집기 애니메이션
- 질문/답변 전환
- 사용자 터치 인터랙션 처리
- 하이라이트 표시

### 시작점과 종료점

#### 시작점
```kotlin
// MainScreen에서 사용
FlipCard(
    modifier = Modifier.fillMaxWidth(),
    frontContent = { QuestionCard(...) },
    backContent = { AnswerCard(...) }
)
```

#### 종료점
- 사용자가 카드를 터치하여 뒤집을 때까지 유지
- 화면이 사라질 때 자동으로 정리됨

### 핵심 기능

#### 1. 3D 회전 애니메이션
```kotlin
@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500)
    )
    
    Box(
        modifier = modifier
            .clickable { isFlipped = !isFlipped }
    ) {
        // 앞면
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 8 * density
                }
        ) {
            if (rotation < 90f) {
                frontContent()
            }
        }
        
        // 뒷면
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationY = rotation - 180f
                    cameraDistance = 8 * density
                }
        ) {
            if (rotation >= 90f) {
                backContent()
            }
        }
    }
}
```

#### 2. 하이라이트 표시
```kotlin
@Composable
fun QuestionCard(
    questionEn: String,
    questionKo: String,
    currentSentenceIndex: Int?,
    scriptIndex: Int = 0,
    scriptTotal: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 영어 질문 (하이라이트 적용)
            val sentences = questionEn.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            sentences.forEachIndexed { idx, sentence ->
                val isHighlighted = currentSentenceIndex == idx
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isHighlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.background(
                        if (isHighlighted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                )
            }
            
            // 한국어 질문
            Text(
                text = questionKo,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

### 사용 예시

#### 질문 카드
```kotlin
QuestionCard(
    questionEn = "What do you like to do in your free time?",
    questionKo = "여가 시간에 무엇을 하시나요?",
    currentSentenceIndex = questionHighlightIndex,
    scriptIndex = currentIndex,
    scriptTotal = totalCount
)
```

#### 답변 카드
```kotlin
AnswerCard(
    answerEn = "I like to read books and watch movies.",
    answerKo = "책을 읽고 영화를 보는 것을 좋아합니다.",
    currentSentenceIndex = answerHighlightIndex,
    scriptIndex = currentIndex,
    scriptTotal = totalCount
)
```

## 4. MainViewModel

### 개요
MainViewModel은 메인 화면의 상태 관리와 비즈니스 로직을 담당하는 ViewModel입니다. MVVM 패턴의 핵심 컴포넌트로, UI와 비즈니스 로직을 분리합니다.

### 위치
`app/src/main/java/com/na982/opichelper/presentation/viewmodel/MainViewModel.kt`

### 주요 책임
- 질문 데이터 로딩 및 관리
- 현재 질문 인덱스 관리
- 카테고리 변경 처리
- UI 상태 관리 (로딩, 에러, 성공)
- 하이라이트 인덱스 관리

### 시작점과 종료점

#### 시작점
```kotlin
// MainActivity에서 초기화
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            MainScreen(viewModel = viewModel)
        }
    }
}
```

#### 종료점
- Activity가 종료될 때 자동으로 정리됨
- ViewModel의 생명주기는 Activity와 연결됨

### 상태 관리

#### UI 상태
```kotlin
data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<QuestionCategory> = emptyList(),
    val currentCategory: QuestionCategory? = null,
    val currentQaItem: Question? = null,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null
)
```

#### 상태 흐름
```kotlin
private val _uiState = MutableStateFlow(MainUiState())
val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex.asStateFlow()

private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex.asStateFlow()
```

### 핵심 메서드

#### 1. loadQuestions()
```kotlin
fun loadQuestions(category: QuestionCategory? = null) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        try {
            val questions = questionRepository.getQuestions(category)
            val currentCategory = category ?: QuestionCategory.ALL
            
            _uiState.value = _uiState.value.copy(
                categories = QuestionCategory.values().toList(),
                currentCategory = currentCategory,
                currentQaItem = questions.firstOrNull(),
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = e.message,
                isLoading = false
            )
        }
    }
}
```

#### 2. nextQaItem()
```kotlin
fun nextQaItem() {
    val currentQuestions = getCurrentQuestions()
    val currentIndex = currentQuestions.indexOf(_uiState.value.currentQaItem)
    
    if (currentIndex < currentQuestions.size - 1) {
        val nextQuestion = currentQuestions[currentIndex + 1]
        _uiState.value = _uiState.value.copy(currentQaItem = nextQuestion)
    }
}
```

#### 3. setQuestionHighlightIndex()
```kotlin
fun setQuestionHighlightIndex(index: Int?) {
    _questionHighlightIndex.value = index
}
```

#### 4. setAnswerHighlightIndex()
```kotlin
fun setAnswerHighlightIndex(index: Int?) {
    _answerHighlightIndex.value = index
}
```

### 데이터 흐름

#### 1. 앱 시작 시
```
MainActivity → MainViewModel.loadQuestions() → QuestionRepository → UI 업데이트
```

#### 2. 카테고리 변경 시
```
UI 선택 → MainViewModel.loadQuestions(category) → QuestionRepository → UI 업데이트
```

#### 3. 다음 질문으로 이동 시
```
UI 버튼 클릭 → MainViewModel.nextQaItem() → 상태 업데이트 → UI 업데이트
```

#### 4. TTS 하이라이트 시
```
TtsService 콜백 → MainViewModel.setQuestionHighlightIndex() → UI 업데이트
```

### 테스트 전략

#### 단위 테스트
```kotlin
@Test
fun `loadQuestions는 성공 시 UI 상태를 업데이트함`() = runTest {
    val mockRepository = mock<QuestionRepository>()
    val questions = listOf(Question(...))
    whenever(mockRepository.getQuestions(any())).thenReturn(questions)
    
    val viewModel = MainViewModel(mockRepository)
    viewModel.loadQuestions()
    
    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertNull(uiState.error)
    assertEquals(questions.first(), uiState.currentQaItem)
}
```

## 5. MainScreen

### 개요
MainScreen은 앱의 메인 화면을 구성하는 Compose 컴포넌트입니다. 사용자 인터페이스와 사용자 상호작용을 담당합니다.

### 위치
`app/src/main/java/com/na982/opichelper/presentation/ui/screen/MainScreen.kt`

### 주요 책임
- 질문/답변 카드 표시
- TTS 재생 버튼
- 음성 인식 버튼
- 카테고리 선택
- 하이라이트 표시
- 사용자 인터랙션 처리

### 시작점과 종료점

#### 시작점
```kotlin
// MainActivity에서 호출
MainScreen(
    viewModel = viewModel,
    modifier = Modifier.fillMaxSize()
)
```

#### 종료점
- Activity가 종료될 때 자동으로 정리됨
- DisposableEffect를 통한 리소스 정리

### UI 구성 요소

#### 1. 카테고리 선택
```kotlin
ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
) {
    OutlinedTextField(
        value = selectedCategory?.name ?: "전체",
        onValueChange = {},
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.menuAnchor()
    )
    
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        categories.forEach { category ->
            DropdownMenuItem(
                text = { Text(category.name) },
                onClick = {
                    selectedCategory = category
                    expanded = false
                    viewModel.loadQuestions(category)
                }
            )
        }
    }
}
```

#### 2. 질문/답변 카드
```kotlin
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
                text = "에러: ${uiState.error}",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
    uiState.currentQaItem != null -> {
        QuestionCard(
            questionEn = uiState.currentQaItem!!.questionEn,
            questionKo = uiState.currentQaItem!!.questionKo,
            currentSentenceIndex = questionHighlightIndex,
            scriptIndex = currentIndex,
            scriptTotal = totalCount
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AnswerCard(
            answerEn = uiState.currentQaItem!!.answerEn,
            answerKo = uiState.currentQaItem!!.answerKo,
            currentSentenceIndex = answerHighlightIndex,
            scriptIndex = currentIndex,
            scriptTotal = totalCount
        )
    }
}
```

#### 3. TTS 재생 버튼
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Button(
        onClick = {
            viewModel.setAnswerHighlightIndex(null)
            ttsService?.speakQuestion(uiState.currentQaItem!!.questionEn, rate = 0.8f)
        },
        modifier = Modifier.weight(1f)
    ) {
        Text("질문재생")
    }
    
    Button(
        onClick = {
            viewModel.setQuestionHighlightIndex(null)
            ttsService?.speakAnswer(uiState.currentQaItem!!.answerEn, rate = 0.8f)
        },
        modifier = Modifier.weight(1f)
    ) {
        Text("전체 재생")
    }
    
    Button(
        onClick = {
            viewModel.setQuestionHighlightIndex(null)
            ttsService?.speakBySentence(uiState.currentQaItem!!.answerEn, repeatCount = 5, pauseRatio = 1.5f)
        },
        modifier = Modifier.weight(1f)
    ) {
        Text("5회 반복")
    }
}
```

#### 4. 음성 인식 버튼
```kotlin
Button(
    onClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
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
```

### 서비스 바인딩

#### TtsService 바인딩
```kotlin
var ttsService by remember { mutableStateOf<TtsService?>(null) }

val serviceConnection = remember {
    object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TtsService.TtsBinder
            ttsService = binder?.getService()
            ttsService?.setHighlightCallback(object : TtsService.HighlightCallback {
                override fun onQuestionHighlight(index: Int?) {
                    viewModel.setQuestionHighlightIndex(index)
                }
                override fun onAnswerHighlight(index: Int?) {
                    viewModel.setAnswerHighlightIndex(index)
                }
            })
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            ttsService?.setHighlightCallback(null)
            ttsService = null
        }
    }
}

DisposableEffect(Unit) {
    val intent = Intent(context, TtsService::class.java)
    context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    onDispose {
        ttsService?.setHighlightCallback(null)
        context.unbindService(serviceConnection)
    }
}
```

### 권한 처리

#### 음성 인식 권한
```kotlin
val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (!isGranted) {
        Toast.makeText(context, "음성 인식 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }
}
```

### 리소스 정리

#### DisposableEffect를 통한 정리
```kotlin
DisposableEffect(Unit) {
    onDispose {
        if (isRecording) {
            speechHelper.stopListening()
        }
        speechHelper.destroy()
    }
}
```

## 컴포넌트 간 상호작용

### 1. TTS 흐름
```
사용자 버튼 클릭 → MainScreen → TtsService → 하이라이트 콜백 → MainViewModel → UI 업데이트
```

### 2. 음성 인식 흐름
```
사용자 버튼 클릭 → MainScreen → SpeechRecognizerHelper → RecognitionCallback → UI 업데이트
```

### 3. 데이터 로딩 흐름
```
앱 시작 → MainViewModel → QuestionRepository → Data Layer → UI 업데이트
```

## 개발 시 주의사항

### 1. 메모리 누수 방지
- DisposableEffect를 사용한 리소스 정리
- 서비스 바인딩 해제
- SpeechRecognizer 정리

### 2. 권한 처리
- 런타임 권한 요청
- 권한 거부 시 적절한 안내
- 권한 상태 확인

### 3. 에러 처리
- 네트워크 오류 처리
- TTS 초기화 실패 처리
- 음성 인식 오류 처리

### 4. 생명주기 관리
- Activity 생명주기에 맞춘 리소스 관리
- 백그라운드 처리 시 적절한 정리
- 포그라운드 서비스 사용

## 확장 가이드

### 새로운 기능 추가
1. Domain Layer에 UseCase 추가
2. Data Layer에 Repository 구현
3. Presentation Layer에 UI 컴포넌트 추가
4. ViewModel에 상태 관리 로직 추가

### 새로운 화면 추가
1. `screen/` 디렉토리에 새 화면 컴포넌트 생성
2. `viewmodel/` 디렉토리에 해당 ViewModel 생성
3. 필요한 UI 컴포넌트를 `component/`에 추가
4. 네비게이션 설정

### 새로운 서비스 추가
1. Android Service 클래스 생성
2. 바인딩 인터페이스 정의
3. MainScreen에서 서비스 바인딩
4. 적절한 리소스 정리 