# 3단계 Presentation Layer 리팩토링

## 📋 개요

Presentation Layer의 상태 관리 구조를 개선하여 불변성을 강화하고, UI 컴포넌트 간의 책임을 명확히 분리했습니다.

## 🎯 목표

1. **상태 관리 단순화**: 여러 StateFlow를 MainUiState로 통합
2. **불변성 강화**: UI 상태의 불변 데이터 구조 보장
3. **책임 분리**: UI 컴포넌트의 명확한 역할 정의
4. **성능 최적화**: 불필요한 상태 구독 제거

## 🔧 주요 변경사항

### 1. MainUiState 단일 진입점 도입

#### 기존 구조
```kotlin
// 여러 StateFlow 노출
val questionHighlightIndex: StateFlow<Int?>
val answerHighlightIndex: StateFlow<Int?>
val isPlaying: StateFlow<Boolean>
val isQuestionPlaying: StateFlow<Boolean>
val isAnswerPlaying: StateFlow<Boolean>
val isAnswerCardFlipped: StateFlow<Boolean>
val hasRecordingFile: StateFlow<Boolean>
val currentKoreanTtsService: StateFlow<String>
val isMemorizeTestRunning: StateFlow<Boolean>
val isFullMemorizationMode: StateFlow<Boolean>
// ... 등등
```

#### 개선된 구조
```kotlin
data class MainUiState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val memorizeLevels: List<String> = emptyList(),
    val selectedMemorizeLevel: String = "",
    val hasMergedAudioFile: Boolean = false,
    val isMergedAudioPlaying: Boolean = false,
    val isAnswerCardFlipped: Boolean = false,
    val hasRecordingFile: Boolean = false,
    val currentKoreanTtsService: String = "",
    val isMemorizeTestRunning: Boolean = false,
    val isFullMemorizationMode: Boolean = false,
    val fullMemorizationHighlightIndex: Int? = null,
    val isFullMemorizationRecording: Boolean = false,
    val isFullMemorizationPlaying: Boolean = false,
    val hasFullMemorizationRecording: Boolean = false,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,
    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val hasProgress: Boolean = false
)

// 단일 StateFlow만 외부에 노출
val uiState: StateFlow<MainUiState>
```

### 2. ViewModel 상태 동기화 구조

```kotlin
init {
    // 각 StateFlow의 collect를 통해 _uiState를 자동 갱신
    viewModelScope.launch {
        ttsPlaybackController.questionHighlightIndex.collect { idx ->
            _uiState.value = _uiState.value.copy(questionHighlightIndex = idx)
        }
    }
    viewModelScope.launch {
        ttsPlaybackController.isPlaying.collect { playing ->
            _uiState.value = _uiState.value.copy(isPlaying = playing)
        }
    }
    // ... 기타 상태들도 동일하게 처리
}
```

### 3. MainScreen 구조 개선

#### 기존 구조
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // 여러 StateFlow 구독
    val questionHighlightIndex by viewModel.questionHighlightIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isQuestionPlaying by viewModel.isQuestionPlaying.collectAsState()
    val isAnswerPlaying by viewModel.isAnswerPlaying.collectAsState()
    val isAnswerCardFlipped by viewModel.isAnswerCardFlipped.collectAsState()
    val hasRecordingFile by viewModel.hasRecordingFile.collectAsState()
    val currentKoreanTtsService by viewModel.currentKoreanTtsService.collectAsState()
    // ... 등등
    
    // UI 구성
    QuestionCard(
        highlightIndex = questionHighlightIndex,
        // ...
    )
}
```

#### 개선된 구조
```kotlin
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // 단일 StateFlow만 구독
    val uiState by viewModel.uiState.collectAsState()
    
    // UI 구성
    QuestionCard(
        highlightIndex = uiState.questionHighlightIndex,
        // ...
    )
}
```

### 4. UI 컴포넌트 책임 명확화

#### CategorySelector 개선
```kotlin
// 기존: 불필요한 파라미터
@Composable
fun CategorySelector(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    playbackState: PlaybackState,        // 불필요
    onHighlightReset: () -> Unit,        // 불필요
    modifier: Modifier = Modifier
)

// 개선: 필요한 파라미터만
@Composable
fun CategorySelector(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

#### NavigationSection 개선
```kotlin
// 기존: 불필요한 파라미터
@Composable
fun NavigationSection(
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    playbackState: PlaybackState,        // 불필요
    onHighlightReset: () -> Unit,        // 불필요
    modifier: Modifier = Modifier
)

// 개선: 필요한 파라미터만
@Composable
fun NavigationSection(
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
)
```

## 📊 리팩토링 효과

### 1. 상태 관리 단순화
- **기존**: 15+ 개의 개별 StateFlow
- **개선**: 1개의 MainUiState StateFlow
- **효과**: UI에서 상태 구독 복잡도 대폭 감소

### 2. 불변성 보장
- **MainUiState**: 불변 데이터 구조
- **상태 변경**: `_uiState.value = _uiState.value.copy(...)` 패턴
- **효과**: 예측 가능한 상태 변경, 디버깅 용이

### 3. 성능 최적화
- **불필요한 구독 제거**: UI 컴포넌트가 필요한 상태만 구독
- **상태 동기화**: 자동화된 상태 동기화로 수동 관리 불필요
- **효과**: 메모리 사용량 감소, 재렌더링 최적화

### 4. 테스트 용이성
- **단일 상태 객체**: MainUiState만 테스트하면 모든 UI 상태 검증 가능
- **상태 변경 추적**: 명확한 상태 변경 지점
- **효과**: 테스트 코드 작성 및 유지보수 간소화

## 🔍 코드 품질 개선

### 1. 타입 안전성
```kotlin
// null 체크 추가
val currentProgress = progressTracker.getScriptProgress(
    currentCategory ?: "", 
    currentIndex ?: 0
)
```

### 2. 불필요한 코드 제거
```kotlin
// 사용하지 않는 변수 제거
// var ttsPlayer by remember { mutableStateOf<TtsPlayer?>(null) }

// 불필요한 파라미터 제거
// playbackState, onHighlightReset 등
```

### 3. 네이밍 일관성
```kotlin
// 변수명 통일
val app = getApplication<Application>() as OPicHelperApplication
// (기존: application -> app로 변경하여 shadowing 방지)
```

## 🚀 다음 단계

이제 4단계 통합 테스트 및 최종 검증을 통해 리팩토링의 완성도를 확인할 수 있습니다.

## 📝 변경된 파일 목록

### 핵심 변경 파일
- `app/src/main/java/com/na982/opichelper/presentation/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/na982/opichelper/presentation/ui/screen/MainScreen.kt`
- `app/src/main/java/com/na982/opichelper/presentation/ui/screen/MainScreenComponentsUI/CategorySelector.kt`
- `app/src/main/java/com/na982/opichelper/presentation/ui/screen/MainScreenComponentsUI/NavigationSection.kt`

### 수정된 파일
- `app/src/main/java/com/na982/opichelper/data/repository/AudioFileManagerImpl.kt` (lint 오류 수정)

## ✅ 검증 완료

- [x] 컴파일 성공
- [x] lint 오류 해결
- [x] 타입 안전성 확보
- [x] 불필요한 코드 제거
- [x] 성능 최적화 적용

---

**3단계 Presentation Layer 리팩토링이 성공적으로 완료되었습니다!** 🎉 