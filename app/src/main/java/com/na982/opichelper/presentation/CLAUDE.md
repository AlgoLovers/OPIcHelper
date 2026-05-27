# Presentation Layer — UI, ViewModel, 네비게이션

사용자 인터페이스와 상태 관리. Compose UI + MVVM.
ViewModel은 Domain 계층만 참조, UI는 ViewModel의 StateFlow만 구독.

## 패키지 구조

```
presentation/
  ui/
    component/               — 재사용 컴포저블
    navigation/              — 네비게이션 그래프
    screen/                  — 화면 단위 컴포저블
      MainScreenComponentsUI/ — MainScreen 하위 컴포넌트
  viewmodel/                 — ViewModel & 상태 클래스
```

## viewmodel/ — 상태 관리

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `QaBrowserViewModel.kt` | **@HiltViewModel ViewModel** QA 데이터 탐색 | QaDataManager + UserPreferencesRepository + MemorizeTestProgressTracker |
| `PlaybackViewModel.kt` | **@HiltViewModel ViewModel** TTS 재생, 병합 파일, 생명주기 | TtsPlaybackController + PlayMergedFileUseCase + TtsOrchestrator |
| `MemorizationViewModel.kt` | **@HiltViewModel ViewModel** 암기 테스트 3모드 로직 | SRP 위반 (3모드 통합). CurrentMode 상태 머신 관리. 6개 개별 MutableStateFlow + 통합 UiState. TtsPlaybackController + QaDataManager + 3개 UseCase + MemorizeTestProgressTracker |
| `SettingsViewModel.kt` | **@HiltViewModel ViewModel** 설정 화면 전용 | UserPreferencesRepository + TtsOrchestrator만 의존 |
| `MemorizationUiState.kt` | 암기 테스트 UI 상태 데이터 클래스 | 반복듣기/영작/통암기 상태 모두 포함. hasFullMemorizationRecording(모드 기반) + hasFullMemorizationRecordingFile(파일 존재 기반) 분리 |
| `CurrentMode.kt` | 암기 테스트 모드 Enum | 아래 전체 목록 참조 |

### CurrentMode 전체 목록
```
NONE
QUESTION_PLAY
ANSWER_PLAY
REPEAT_LISTENING
ENGLISH_WRITING
  ENGLISH_WRITING_RECORDING
  ENGLISH_WRITING_PLAYING
  ENGLISH_WRITING_WITH_FILE
FULL_MEMORIZATION
  FULL_MEMORIZATION_QUESTION_PLAYING
  FULL_MEMORIZATION_RECORDING
  FULL_MEMORIZATION_PLAYING
  FULL_MEMORIZATION_WITH_FILE
```

### QaBrowserState 핵심 필드 (QaBrowserViewModel)
```kotlin
data class QaBrowserState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val selectedMemorizeLevel: String = ...,
    val currentUserLevel: String = ""
)
```

### PlaybackState 핵심 필드 (PlaybackViewModel)
```kotlin
data class PlaybackState(
    val hasEnglishWritingTestMergedFile: Boolean = false,
    val isEnglishWritingTestMergedFilePlaying: Boolean = false,
    val englishWritingTestMergedFileHighlightIndex: Int? = null,
    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val questionHighlight: HighlightInfo = HighlightInfo(),
    val answerHighlight: HighlightInfo = HighlightInfo(),
    val answerKoHighlight: HighlightInfo = HighlightInfo(),
    val recordingHighlight: HighlightInfo = HighlightInfo(),
    val isAnswerCardFlipped: Boolean = false,
    val hasProgress: Boolean = false
)
```

**참고**: PlaybackViewModel은 병합 파일 관련 3개 StateFlow를 개별 노출(hasEnglishWritingTestMergedFile, isEnglishWritingTestMergedFilePlaying, englishWritingTestMergedFileHighlightIndex)과 PlaybackState 내부 포함으로 이중 노출함.

### 상태 흐름
```
QaBrowserViewModel:
  블록1: QaDataManager (5-way combine) → currentQaItem, currentCategory, categories, isLoading, error
  블록2: UserPreferencesRepository.userLevel → currentUserLevel

PlaybackViewModel:
  블록3: TtsPlaybackController (7-way combine) → isPlaying, isQuestionPlaying, isAnswerPlaying,
         questionHighlight, answerHighlight, answerKoHighlight, recordingHighlight
  블록4: PlayMergedFileUseCase (3-way combine) → hasEnglishWritingTestMergedFile, isEnglishWritingTestMergedFilePlaying, englishWritingTestMergedFileHighlightIndex

MemorizationViewModel.fullMemorizationHighlightIndex ──→ MainScreen에서 직접 collect
MemorizationViewModel.isQuestionCardFlipped ──→ MainScreen에서 직접 collect
MemorizationViewModel.englishWritingTestCompleted ──→ MainScreen에서 직접 collect
MemorizationViewModel.stopEnglishWritingTestMergedFilePlaying ──→ MainScreen에서 직접 collect
MemorizationViewModel.memorizeLevels ──→ MainScreen에서 직접 collect
MemorizationViewModel.uiState ──→ MainScreen에서 직접 collect
SettingsViewModel.uiState ──→ SettingsScreen에서 직접 collect
```

**주의**: MainScreen이 11개 StateFlow를 개별 collect하는 구조. "UI는 ViewModel의 StateFlow만 구독" 규칙에 어긋남.

### MemorizationViewModel 주의사항

- **init 블록**: `qaDataManager.currentQaItem.collect`에서 첫 번째 방출만 스킵. 두 번째 방출부터 `updateFullMemorizationRecordingStatus()` 호출
- **updateFullMemorizationRecordingStatus()**: 현재 모드가 FULL_MEMORIZATION 계열일 때만 `_currentMode` 업데이트. 항상 `hasFullMemorizationRecordingFile` 업데이트
- **resetStateOnAppRestart()**: currentMode를 NONE으로 리셋. init의 첫 스킵 로직과 함께 초기 진입 시 항상 반복듣기 모드 보장
- **isFullMemorizationMode**: MainScreen에서 `selectedLevel` 기반으로 파생 (MemorizationUiState의 currentMode 기반과 다름)

## ui/component/ — 재사용 컴포저블

| 파일 | 역할 |
|------|------|
| `FlipCard.kt` | 3D 플립 애니메이션 카드. 영문/한문 전환. 내부 상태가 외부 isFlipped를 섀도잉하는 패턴 |

## ui/navigation/

| 파일 | 역할 |
|------|------|
| `AppNavigation.kt` | NavHost. Main/Settings 2개 라우트. sealed class Screen |

## ui/screen/ — 화면

| 파일 | 역할 | 비고 |
|------|------|------|
| `MainScreen.kt` | 메인 화면. 세 ViewModel 상태 결합 | QuestionCard/AnswerCard 하이라이트: when 분기로 소스 선택 |
| `SettingsScreen.kt` | 설정: 학습 레벨 선택, 앱 정보(버전, 한글 TTS 서비스명) | 그라디언트 헤더 + 헤더 배지로 메인 화면과 룩앤필 통일 |

## ui/screen/MainScreenComponentsUI/ — MainScreen 하위 컴포넌트

| 파일 | 역할 |
|------|------|
| `AppTitle.kt` | 앱 타이틀바 (그라디언트 배경, 설정 버튼) |
| `CategorySelector.kt` | 카테고리 드롭다운 (헤더 배지 + ExposedDropdownMenuBox) |
| `MemorizeLevelSelector.kt` | 암기 모드 선택 (카테고리 셀렉터와 동일 스타일) |
| `QuestionCard.kt` | 질문 카드 (FlipCard + HighlightText) |
| `AnswerCard.kt` | 답변 카드 (FlipCard + HighlightText, 녹음 하이라이트) |
| `QuestionPlayButton.kt` | 질문 재생/정지 버튼 |
| `AnswerPlayButton.kt` | 답변 재생/정지 버튼 |
| `MemorizeLevelPlaybackButton.kt` | 모드별 동적 재생 버튼 (ViewModel 인스턴스를 직접 받음 — Compose 규칙 위반) |
| `FullMemorizationRecordingButton.kt` | 통암기 녹음 시작/정지 ("답변 녹음" 텍스트) |
| `RecordingAnimation.kt` | 녹음 중 표시 애니메이션 (isRecording, onStopRecording 미사용) |
| `NavigationSection.kt` | 이전/다음 질문 네비게이션 |
| `NextQuestionButton.kt` | 다음 질문 버튼 |
| `PreviousQuestionButton.kt` | 이전 질문 버튼 |

### 미사용 컴포넌트 (레거시)
- `RecordingButton.kt` — FullMemorizationRecordingButton으로 대체됨
- `RecordingSection.kt` — MainScreen에서 사용하지 않음
- `QuestionAnswerSection.kt` — 별도 QuestionCard/AnswerCard로 대체됨

### 하이라이트 연동 패턴 (MainScreen.kt)
```kotlin
// QuestionCard — 모드별 하이라이트 소스 분기
highlightIndex = when {
    (isFullMemorizationMode && isFullMemorizationPlaying) -> fullMemorizationHighlightIndex
    else -> playbackState.questionHighlight.index
}

// AnswerCard — 3소스 분기
highlightIndex = when {
    (isFullMemorizationMode && isFullMemorizationPlaying) || isFullMemorizationRecordingPlaying -> fullMemorizationHighlightIndex
    isEnglishWritingTestMergedFilePlaying -> englishWritingTestMergedFileHighlightIndex
    else -> playbackState.answerHighlight.index
}

// isFullMemorizationMode는 selectedLevel에서 파생:
val isFullMemorizationMode = MemorizeLevel.fromDisplayName(selectedLevel) == MemorizeLevel.FULL_MEMORIZATION
```

## 아키텍처 규칙
- UI는 ViewModel의 StateFlow만 구독 (`collectAsState()`)
- ViewModel은 Domain 계층만 참조 (Data 직접 import 금지)
- Compose 컴포넌트는 상태 비저장, ViewModel에서 상태 관리
- 재사용 컴포넌트는 `component/`, 화면 전용은 `MainScreenComponentsUI/`

### 현재 규칙 위반
- MainScreen이 11개 StateFlow를 개별 collect (단일 통합 상태 미사용)
- MemorizeLevelPlaybackButton이 ViewModel 인스턴스를 직접 수신 (콜백 전달 방식 권장)
- MemorizationViewModel이 6개 개별 MutableStateFlow 보유 (통합 상태 미사용)
