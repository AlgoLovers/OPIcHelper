# Presentation Layer — UI, ViewModel, 네비게이션

## 역할
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
| `MainViewModel.kt` | **@HiltViewModel AndroidViewModel** 통합 상태 관리. AppState 단일 StateFlow | 의존성 6개 (QaDataManager, TtsPlaybackController, MemorizeTestProgressTracker, UserPreferencesRepository, PlayMergedFileUseCase, Application). 진입 시 항상 반복듣기 모드 |
| `MemorizationViewModel.kt` | **@HiltViewModel ViewModel** 암기 테스트 3모드 로직 | SRP 위반 (3모드 통합). CurrentMode 상태 머신 관리 |
| `MemorizationUiState.kt` | 암기 테스트 UI 상태 데이터 클래스 | 반복듣기/영작/통암기 상태 모두 포함 |
| `CurrentMode.kt` | 암기 테스트 모드 Enum | NONE, QUESTION_PLAY, ANSWER_PLAY, REPEAT_LISTENING, ENGLISH_WRITING(+substates), FULL_MEMORIZATION(+substates) |

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

### MainViewModel의 AppState 핵심 필드
```kotlin
data class AppState(
    val currentQaItem: QaItem?,
    val categories: List<String>,
    val currentCategory: String?,
    val selectedMemorizeLevel: String,           // 항상 "반복 듣기"로 시작
    val memorizeLevels: List<String>,
    val currentUserLevel: String,
    val currentKoreanTtsService: String,
    val isLoading: Boolean,
    val error: String?,
    val isQuestionPlaying: Boolean,
    val isAnswerPlaying: Boolean,
    val isPlaying: Boolean,
    val questionHighlightIndex: Int?,
    val answerHighlightIndex: Int?,
    val answerKoHighlightIndex: Int?,
    val recordingHighlightIndex: Int?,
    val isQuestionCardFlipped: Boolean,
    val isAnswerCardFlipped: Boolean,
    val hasProgress: Boolean,
    val hasEnglishWritingTestMergedFile: Boolean,
    val isEnglishWritingTestMergedFilePlaying: Boolean,
    val englishWritingTestMergedFileHighlightIndex: Int?,
)
```

### 상태 흐름 (MainViewModel 내 5개 구독 블록)
```
블록1: QaDataManager (5-way combine) → currentQaItem, currentCategory, categories, isLoading, error
블록2: UserPreferencesRepository.userLevel → currentUserLevel, currentKoreanTtsService
블록3: TtsPlaybackController (7-way combine) → isPlaying, isQuestionPlaying, isAnswerPlaying,
       questionHighlightIndex, answerHighlightIndex, answerKoHighlightIndex, recordingHighlightIndex
블록4: MemorizeTestProgressTracker.hasProgress → hasProgress
블록5: PlayMergedFileUseCase (3-way combine) → hasMergedFile, isMergedFilePlaying, mergedFileHighlightIndex

MemorizationViewModel.fullMemorizationHighlightIndex ──→ MainScreen에서 직접 collect
```

## ui/component/ — 재사용 컴포저블

| 파일 | 역할 |
|------|------|
| `FlipCard.kt` | 3D 플립 애니메이션 카드. 영문/한문 전환 |
| `HighlightText.kt` | 문장별 하이라이트 텍스트. 현재 문장 색상+크기 강조 |

## ui/navigation/

| 파일 | 역할 |
|------|------|
| `AppNavigation.kt` | NavHost. Main/Settings/Login 3개 라우트. sealed class Screen |

## ui/screen/ — 화면

| 파일 | 역할 | 비고 |
|------|------|------|
| `MainScreen.kt` | 메인 화면. 두 ViewModel 상태 결합 | QuestionCard/AnswerCard 하이라이트: when 분기로 소스 선택 |
| `SettingsScreen.kt` | 설정: 레벨 선택, TTS 속도, 다크모드 | MainViewModel 통해 설정 변경 (UserPreferencesRepository 직접 참조 아님) |
| `LoginScreen.kt` | 로그인: 게스트/Google | Google Sign-In 비활성 상태 |

## ui/screen/MainScreenComponentsUI/ — MainScreen 하위 컴포넌트

| 파일 | 역할 |
|------|------|
| `AppTitle.kt` | 앱 타이틀바 (그라디언트 배경) |
| `CategorySelector.kt` | 카테고리 드롭다운 |
| `MemorizeLevelSelector.kt` | 암기 모드 선택 (반복듣기/영작/통암기) |
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
