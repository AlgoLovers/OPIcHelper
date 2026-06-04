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

| 파일 | 역할 | 의존성 |
|------|------|--------|
| `QaBrowserViewModel.kt` | QA 데이터 탐색 | QaDataManager, UserLevelPreferences, PlaybackPreferences, MemorizeLevelPreferences, MemorizeTestProgressTracker, SearchQaItemsUseCase, AppLogger |
| `PlaybackViewModel.kt` | TTS 재생, 병합 파일, PiP 제어 | TtsPlaybackController, PlayMergedFileUseCase, TtsOrchestrator, PlaybackPreferences, MemorizationModeCoordinator, TtsServiceController, AppLogger |
| `RepeatListeningViewModel.kt` | 반복듣기 모드 | RepeatListeningRepository, TtsPlaybackController, QaContentReader, QaNavigator, MemorizeTestProgressTracker, PlaybackPreferences, MemorizationModeCoordinator, AppLogger |
| `EnglishWritingTestViewModel.kt` | 영작테스트 모드 | EnglishWritingTestRepository, TtsPlaybackController, QaContentReader, MemorizeTestProgressTracker, MemorizationModeCoordinator, AppLogger |
| `FullMemorizationViewModel.kt` | 통암기 모드 | FullMemorizationUseCase, QaContentReader, MemorizationModeCoordinator, AppLogger |
| `SettingsViewModel.kt` | 설정 화면 | UserLevelPreferences, TtsPreferences, PlaybackPreferences, TtsOrchestrator |
| `OnboardingViewModel.kt` | 온보딩/PiP 가이드 상태 | OnboardingPreferences |
| `BaseMemorizationViewModel.kt` | 암기 모드 공통 베이스 | MemorizationModeCoordinator, TtsPlaybackController?, MemorizeTestProgressTracker?, AppLogger, QaContentReader |
| `PlaybackActionListener.kt` | PiP/재생 액션 인터페이스 | onRepeatQuestion, onRepeatAnswer, onNext, onRepeatMemorization, onNextAndRestart, onStopMemorization |

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

### 상태 흐름
```
QaBrowserViewModel:
  QaDataManager (5-way combine) → currentQaItem, currentCategory, categories, isLoading, error
  UserLevelPreferences → currentUserLevel
  PlaybackPreferences → answerPlayCount

PlaybackViewModel:
  TtsPlaybackController (7-way combine) → isPlaying, isQuestionPlaying, isAnswerPlaying, highlights
  PlayMergedFileUseCase (3-way combine) → hasFile, isPlaying, highlightIndex
  PipState (10-way combine) → PiP 상태

SettingsViewModel.uiState → SettingsScreen
```

## ui/component/ — 재사용 컴포저블

| 파일 | 역할 |
|------|------|
| `FlipCard.kt` | 3D 플립 애니메이션 카드. 영문/한문 전환 |
| `PipOverlay.kt` | PiP 모드 오버레이 (문장 표시 전용) |
| `PlayStopToggleButton.kt` | 재생/정지 공통 토글 버튼 |

## ui/navigation/

| 파일 | 역할 |
|------|------|
| `AppNavigation.kt` | NavHost. Main/Settings 2개 라우트 |

## ui/screen/ — 화면

| 파일 | 역할 | 비고 |
|------|------|------|
| `MainScreen.kt` | 메인 화면. 다중 ViewModel 상태 결합 | QuestionCard/AnswerCard 하이라이트: when 분기로 소스 선택 |
| `SettingsScreen.kt` | 설정: 학습 레벨, TTS 속도, 앱 정보 | 그라디언트 헤더 |

## ui/screen/MainScreenComponentsUI/ — MainScreen 하위 컴포넌트

| 파일 | 역할 |
|------|------|
| `AppTitle.kt` | 앱 타이틀바 (그라디언트 배경, 설정 버튼) |
| `CategorySelector.kt` | 카테고리 드롭다운 |
| `MemorizeLevelSelector.kt` | 암기 모드 선택 |
| `QuestionCard.kt` | 질문 카드 (FlipCard + HighlightText) |
| `AnswerCard.kt` | 답변 카드 (FlipCard + HighlightText, 녹음 하이라이트) |
| `QuestionPlayButton.kt` | 질문 재생/정지 버튼 |
| `AnswerPlayButton.kt` | 답변 재생/정지 버튼 |
| `MemorizeLevelPlaybackButton.kt` | 모드별 동적 재생 버튼 (콜백 기반) |
| `FullMemorizationRecordingButton.kt` | 통암기 녹음 시작/정지 |
| `RecordingAnimation.kt` | 녹음 중 표시 애니메이션 |
| `NavigationSection.kt` | 이전/다음 질문 네비게이션 |
| `NextQuestionButton.kt` | 다음 질문 버튼 |
| `PreviousQuestionButton.kt` | 이전 질문 버튼 |

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
```

## 아키텍처 규칙
- UI는 ViewModel의 StateFlow만 구독 (`collectAsState()`)
- ViewModel은 Domain 계층만 참조 (Data 직접 import 금지)
- Compose 컴포넌트는 상태 비저장, ViewModel에서 상태 관리
- 재사용 컴포넌트는 `component/`, 화면 전용은 `MainScreenComponentsUI/`
- 로깅은 `AppLogger` 인터페이스 사용 (`android.util.Log` 직접 호출 금지)
