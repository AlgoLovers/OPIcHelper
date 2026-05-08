# OPIC Helper

OPIc 영어 말하기 시험 대비 학습 Android 애플리케이션. Clean Architecture + MVVM 패턴 기반으로 Jetpack Compose를 사용하여 개발되었습니다.

## 주요 기능

- **OPIc 질문/답변 연습**: 레벨별(AL/IH/IM) 카테고리 QA 데이터 제공
- **TTS 재생**: Google TTS(영문) + Samsung TTS(한글) 자동 전환, 문장별 하이라이트
- **반복 듣기 모드**: 한글-영문 교차 반복 재생
- **영작 테스트 모드**: 한글 듣기 → 영작 녹음 → 병합 파일 재생
- **통암기 모드**: 질문 재생 → 녹음 → 재생 비교
- **진행 상황 추적**: 스크립트별 암기 진행도 저장/복원

## 기술 스택

| 분야 | 기술 |
|------|------|
| 언어 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 아키텍처 | Clean Architecture + MVVM |
| DI | Hilt (Dagger) |
| 비동기 | Kotlin Coroutines + Flow |
| TTS | Android TextToSpeech API |
| 오디오 | MediaPlayer + MediaRecorder |
| 데이터 | JSON Assets + SharedPreferences |
| 빌드 | Gradle 8.13, AGP 8.11.1, Kotlin 1.9.22 |

## 프로젝트 구조

```
app/src/main/java/com/na982/opichelper/
├── MainActivity.kt                    # 앱 진입점, Compose 설정
├── SplashActivity.kt                  # 스플래시 화면
├── OPicHelperApplication.kt           # Hilt Application
│
├── data/                              # Data Layer
│   ├── audio/
│   │   ├── BaseTtsPlayer.kt           # TTS 플레이어 공통 로직
│   │   ├── GoogleTtsPlayer.kt         # 영문 TTS (Google)
│   │   ├── SamsungTtsPlayer.kt        # 한글 TTS (Samsung)
│   │   ├── AudioPlayerImpl.kt         # 오디오 재생 구현
│   │   ├── AudioRecorderImpl.kt       # 오디오 녹음 구현
│   │   └── RecordingAudioPlayerImpl.kt # 녹음 파일 재생 구현
│   └── repository/
│       ├── QaDataLoaderImpl.kt        # QA JSON 로더
│       ├── LeveledQaDataLoader.kt     # 레벨별 QA 로더
│       ├── AudioFileManagerImpl.kt    # 오디오 파일 관리
│       ├── AuthRepository.kt          # 인증 상태 관리
│       ├── UserPreferencesRepository.kt # 사용자 설정
│       ├── RecordingFileRepositoryImpl.kt # 녹음 파일 관리
│       ├── RecordingTimeManagerImpl.kt # 녹음 시간 관리
│       ├── EnglishWritingTestRepositoryImpl.kt
│       ├── RepeatListeningRepositoryImpl.kt
│       └── FullMemorizationRepositoryImpl.kt
│
├── di/
│   └── AppModule.kt                   # Hilt DI 모듈
│
├── domain/                            # Domain Layer
│   ├── audio/
│   │   ├── TtsPlayer.kt               # TTS 플레이어 인터페이스
│   │   ├── TtsOrchestrator.kt         # TTS 오케스트레이터 (언어별 라우팅/폴백)
│   │   ├── TtsPlaybackController.kt   # TTS 재생 상태 제어
│   │   ├── AudioPlayer.kt             # 오디오 재생 인터페이스
│   │   ├── AudioRecorder.kt           # 오디오 녹음 인터페이스
│   │   ├── RecordingAudioPlayer.kt    # 녹음 재생 인터페이스
│   │   └── RepeatListeningUiCallback.kt # 반복듣기 UI 콜백
│   ├── entity/
│   │   ├── QaItem.kt                  # QA 데이터 모델
│   │   ├── QuestionCategory.kt        # 카테고리 enum (미사용)
│   │   ├── PlaybackState.kt           # 재생 상태
│   │   ├── RepeatListeningData.kt     # 반복듣기 데이터
│   │   ├── Result.kt                  # 결과 래퍼
│   │   ├── TtsConstants.kt            # TTS 상수
│   │   └── UserLevel.kt               # 사용자 레벨 enum
│   ├── manager/
│   │   └── WakeLockManager.kt         # 화면 켜짐 유지
│   ├── repository/                    # 리포지토리 인터페이스
│   │   ├── QaDataLoader.kt
│   │   ├── QaDataManager.kt
│   │   ├── AudioFileManager.kt
│   │   ├── ProgressPersistenceService.kt
│   │   ├── RecordingFileRepository.kt
│   │   ├── RecordingTimeManager.kt
│   │   ├── EnglishWritingTestRepository.kt
│   │   ├── RepeatListeningRepository.kt
│   │   ├── FullMemorizationRepository.kt
│   │   ├── UserPreferencesRepository.kt
│   │   ├── AppExitState.kt
│   │   └── ScriptProgress.kt
│   └── usecase/
│       ├── ExecuteRepeatListeningUseCase.kt
│       ├── ExecuteEnglishWritingTestUseCase.kt
│       ├── FullMemorizationUseCase.kt
│       ├── RepeatListeningUseCase.kt
│       ├── EnglishWritingTestUseCase.kt
│       ├── MemorizeTestProgressTracker.kt
│       ├── LoadQaItemsUseCase.kt
│       ├── GetCategoriesUseCase.kt
│       └── SelectCategoryUseCase.kt
│
└── presentation/                      # Presentation Layer
    ├── ui/
    │   ├── navigation/
    │   │   └── AppNavigation.kt       # Compose Navigation
    │   ├── screen/
    │   │   ├── MainScreen.kt          # 메인 화면
    │   │   ├── LoginScreen.kt         # 로그인 화면
    │   │   ├── SettingsScreen.kt      # 설정 화면
    │   │   └── MainScreenComponentsUI/ # 메인 화면 UI 컴포넌트들
    │   │       ├── AnswerCard.kt
    │   │       ├── AnswerPlayButton.kt
    │   │       ├── AppTitle.kt
    │   │       ├── CategorySelector.kt
    │   │       ├── MemorizeLevelSelector.kt
    │   │       ├── NavigationSection.kt
    │   │       ├── NextQuestionButton.kt
    │   │       ├── PreviousQuestionButton.kt
    │   │       ├── QuestionAnswerSection.kt
    │   │       ├── QuestionCard.kt
    │   │       ├── QuestionPlayButton.kt
    │   │       ├── RecordingAnimation.kt
    │   │       ├── RecordingButton.kt
    │   │       └── RecordingSection.kt
    │   └── component/
    │       ├── FlipCard.kt            # 카드 뒤집기 애니메이션
    │       └── HighlightText.kt       # 하이라이트 텍스트
    └── viewmodel/
        ├── MainViewModel.kt           # 메인 상태 관리
        ├── MemorizationViewModel.kt   # 암기 테스트 상태 관리
        └── TtsViewModel.kt            # TTS 재생 상태 관리
```

## 아키텍처

```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  Compose UI ← ViewModel ← TtsPlaybackController │
├─────────────────────────────────────────────────┤
│               Domain Layer                       │
│  Entity / UseCase / Repository Interface         │
├─────────────────────────────────────────────────┤
│                Data Layer                        │
│  Repository Impl / TTS Players / File Manager    │
└─────────────────────────────────────────────────┘
```

### 데이터 흐름
- **QA 데이터**: JSON Assets → QaDataLoader → QaDataManager → ViewModel → UI
- **TTS 재생**: UI → ViewModel → TtsPlaybackController → TtsOrchestrator → GoogleTtsPlayer/SamsungTtsPlayer
- **녹음**: UI → ViewModel → UseCase → AudioRecorder → AudioFileManager

## 빌드 및 실행

```bash
# 프로젝트 클론
git clone <repository-url>
cd OPIcHelper

# 단위 테스트
./gradlew testDebugUnitTest

# 계측 테스트
./gradlew connectedDebugAndroidTest

# 빌드
./gradlew assembleDebug
```

### 요구사항
- Android Studio Hedgehog 이상
- Kotlin 1.9.22, JDK 11+
- minSdk 24, targetSdk 34

## 문서

자세한 내용은 [docs/](./docs/) 디렉토리를 참조하세요.
