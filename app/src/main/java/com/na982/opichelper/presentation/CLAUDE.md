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
| `MemorizationViewModel.kt` | **@HiltViewModel ViewModel** 암기 테스트 3모드 로직 | SRP 위반 (3모드 통합). CurrentMode 상태 머신 관리. isQuestionCardFlipped 상태 포함. SharedFlow 이벤트 구독 |
| `SettingsViewModel.kt` | **@HiltViewModel ViewModel** 설정 화면 전용 | UserPreferencesRepository + TtsOrchestrator만 의존 |
| `MemorizationUiState.kt` | 암기 테스트 UI 상태 데이터 클래스 | 반복듣기/영작/통암기 상태 모두 포함 |
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
data class AppState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val memorizeLevels: List<String> = MemorizeLevel.allDisplayNames,
    val selectedMemorizeLevel: String = MemorizeLevel.REPEAT_LISTENING.displayName,
    val hasEnglishWritingTestMergedFile: Boolean = false,
    val isEnglishWritingTestMergedFilePlaying: Boolean = false,
    val englishWritingTestMergedFileHighlightIndex: Int? = null,
    val isPlaying: Boolean = false,
    val isQuestionPlaying: Boolean = false,
    val isAnswerPlaying: Boolean = false,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null,
    val answerKoHighlightIndex: Int? = null,
    val recordingHighlightIndex: Int? = null,
    val isAnswerCardFlipped: Boolean = false,
    val hasProgress: Boolean = false,
    val currentUserLevel: String = ""
)
```

### 상태 흐름
```
QaBrowserViewModel:
  블록1: QaDataManager (5-way combine) → currentQaItem, currentCategory, categories, isLoading, error
  블록2: UserPreferencesRepository.userLevel → currentUserLevel

PlaybackViewModel:
  블록3: TtsPlaybackController (7-way combine) → isPlaying, isQuestionPlaying, isAnswerPlaying,
         questionHighlightIndex, answerHighlightIndex, answerKoHighlightIndex, recordingHighlightIndex
  블록4: PlayMergedFileUseCase (3-way combine) → hasMergedFile, isMergedFilePlaying, mergedFileHighlightIndex

MemorizationViewModel.fullMemorizationHighlightIndex ──→ MainScreen에서 직접 collect
MemorizationViewModel.isQuestionCardFlipped ──→ MainScreen에서 직접 collect
SettingsViewModel.uiState ──→ SettingsScreen에서 직접 collect
```

### MemorizationViewModel 주의사항

- **init 블록**: `qaDataManager.currentQaItem.collect`에서 첫 아이템 수신 시 `updateFullMemorizationRecordingStatus()`를 스킵. 이것은 앱 재시작 시 통암기 모드로 자동 진입하는 것을 방지하기 위함
- **resetStateOnAppRestart()**: currentMode를 NONE으로 리셋. init의 첫 스킵 로직과 함께 초기 진입 시 항상 반복듣기 모드 보장

## ui/component/ — 재사용 컴포저블

| 파일 | 역할 |
|------|------|
| `FlipCard.kt` | 3D 플립 애니메이션 카드. 영문/한문 전환 |
| `HighlightText.kt` | 문장별 하이라이트 텍스트. 현재 문장 색상+크기 강조 |

## ui/navigation/

| 파일 | 역할 |
|------|------|
| `AppNavigation.kt` | NavHost. Main/Settings 2개 라우트. sealed class Screen |

## ui/screen/ — 화면

| 파일 | 역할 | 비고 |
|------|------|------|
| `MainScreen.kt` | 메인 화면. 두 ViewModel 상태 결합 | QuestionCard/AnswerCard 하이라이트: when 분기로 소스 선택 |
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
| `MemorizeLevelPlaybackButton.kt` | 모드별 동적 재생 버튼 |
| `RecordingButton.kt` | 통암기 녹음 시작/정지 |
| `RecordingAnimation.kt` | 녹음 중 표시 애니메이션 |
| `RecordingSection.kt` | 녹음 컨트롤 영역 |
| `NavigationSection.kt` | 이전/다음 질문 네비게이션 |
| `NextQuestionButton.kt` | 다음 질문 버튼 |
| `PreviousQuestionButton.kt` | 이전 질문 버튼 |
| `QuestionAnswerSection.kt` | 질문+답변 카드 결합 영역 |

### 하이라이트 연동 패턴 (MainScreen.kt)
```kotlin
// QuestionCard — 모드별 하이라이트 소스 분기
highlightIndex = when {
    (isFullMemorizationMode && isFullMemorizationPlaying) -> fullMemorizationHighlightIndex
    else -> uiState.questionHighlightIndex
}

// AnswerCard — 3소스 분기
highlightIndex = when {
    (isFullMemorizationMode && isFullMemorizationPlaying) || isFullMemorizationRecordingPlaying -> fullMemorizationHighlightIndex
    isEnglishWritingTestMergedFilePlaying -> englishWritingTestMergedFileHighlightIndex
    else -> uiState.answerHighlightIndex
}
```

## 아키텍처 규칙
- UI는 ViewModel의 StateFlow만 구독 (`collectAsState()`)
- ViewModel은 Domain 계층만 참조 (Data 직접 import 금지)
- Compose 컴포넌트는 상태 비저장, ViewModel에서 상태 관리
- 재사용 컴포넌트는 `component/`, 화면 전용은 `MainScreenComponentsUI/`
