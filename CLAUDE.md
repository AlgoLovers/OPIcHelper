# CLAUDE.md - OPIc Helper 개발 가이드

## 가이드 사용법

이 파일은 프로젝트 전체 개요와 규칙을 담습니다. 모듈별 상세 정보는 각 계층의 CLAUDE.md를, 아키텍처 전체 구조는 아키텍처 문서를 참조:

- **코드 수정 시**: 수정 대상 계층의 CLAUDE.md를 먼저 읽고 해당 모듈의 구조와 규칙을 파악
- **신규 기능 추가 시**: 관련 계층 3개(data/domain/presentation)의 CLAUDE.md를 모두 확인
- **버그 수정 시**: 루트의 "알려진 기술 부채"와 해당 계층의 주의사항 확인
- **DI 변경 시**: `di/CLAUDE.md`에서 바인딩 규칙 확인
- **프로젝트 전체 파악**: [ARCHITECTURE.md](.claude/architecture/ARCHITECTURE.md) → 모듈별 아키텍처 문서 순서로 읽기

| 문서 | 내용 |
|------|------|
| [ARCHITECTURE.md](.claude/architecture/ARCHITECTURE.md) | 전체 아키텍처 개요, 다이어그램, 읽기 순서 |
| [ARCHITECTURE_DATA.md](.claude/architecture/ARCHITECTURE_DATA.md) | Data 계층 아키텍처 상세 |
| [ARCHITECTURE_DOMAIN.md](.claude/architecture/ARCHITECTURE_DOMAIN.md) | Domain 계층 아키텍처 상세 |
| [ARCHITECTURE_PRESENTATION.md](.claude/architecture/ARCHITECTURE_PRESENTATION.md) | Presentation 계층 아키텍처 상세 |
| [ARCHITECTURE_IMPROVEMENT_PLAN.md](.claude/plans/ARCHITECTURE_IMPROVEMENT_PLAN.md) | 구조적 결함 및 개선 계획 |
| [REFACTORING_PLAN.md](.claude/plans/REFACTORING_PLAN.md) | 리팩토링 계획 |
| [SCRIPT_EDIT_PLAN.md](.claude/plans/SCRIPT_EDIT_PLAN.md) | 스크립트 편집 기능 구현 계획 |
| `data/CLAUDE.md` | TTS 플레이어 구현체, Repository 구현체, SharedPreferences 키 맵 |
| `domain/CLAUDE.md` | Entity, UseCase, Repository 인터페이스, TTS 오케스트레이터, 재생 컨트롤러 |
| `presentation/CLAUDE.md` | ViewModel, Compose UI, 네비게이션, 상태 흐름 |
| `di/CLAUDE.md` | Hilt 바인딩 전체 목록, 제공 방식, 주의사항 |

## 프로젝트 개요

OPIc 영어 말하기 시험 대비 Android 앱. Clean Architecture + MVVM, Hilt DI, Jetpack Compose.

- **패키지**: `com.na982.opichelper`
- **minSdk**: 24 / **targetSdk**: 34
- **Kotlin**: 1.9.22 / **Compose BOM**: 2023.08.00

## 아키텍처

```
Presentation (Compose UI + ViewModel) → Domain (Entity + UseCase + Repository Interface) ← Data (Repository Impl + TTS Players + File Manager)
```

의존성은 항상 외부(Data) → 내부(Domain) 방향. Domain은 Data를 직접 참조하지 않음.

### ViewModel 구조
- `QaBrowserViewModel`: QA 데이터 탐색, 카테고리, 암기레벨. 의존성 8개 (QaDataManager, UserLevelPreferences, PlaybackPreferences, MemorizeLevelPreferences, MemorizeTestProgressTracker, SearchQaItemsUseCase, ProgressCleanupUseCase, AppLogger)
- `PlaybackViewModel`: TTS 재생, 병합 파일 재생, 코디네이터 이벤트. 의존성 6개 (TtsPlaybackController, PlayMergedFileUseCase, MemorizationModeCoordinator, PlaybackPreferences, PipStateAggregator, AppLogger). PiP 상태는 PipStateAggregator에 위임
- `RepeatListeningViewModel`: 반복듣기 모드 전담. 의존성 7개 (RepeatListeningRepository, TtsPlaybackController, QaDataManager, MemorizeTestProgressTracker, UserPreferencesRepository, MemorizationModeCoordinator, AppLogger)
- `EnglishWritingTestViewModel`: 영작테스트 모드 전담. 의존성 6개 (EnglishWritingTestRepository, TtsPlaybackController, QaDataManager, MemorizeTestProgressTracker, MemorizationModeCoordinator, AppLogger)
- `FullMemorizationViewModel`: 통암기 모드 전담. 의존성 4개 (FullMemorizationUseCase, QaContentReader, MemorizationModeCoordinator, AppLogger)
- `MemorizationModeCoordinator`: 3개 모드 상호 배제, 상태 머신, Job 관리. @Singleton
- `SettingsViewModel`: 설정 화면 전용. UserPreferencesRepository + TtsOrchestrator
- `OnboardingViewModel`: 온보딩/PiP 가이드 상태. OnboardingPreferences

### TTS 재생 흐름 (핵심 경로)
```
UI 버튼 클릭
  → PlaybackViewModel.playQuestion() / playAnswer()
    → TtsPlaybackController.playQuestion() / playAnswer()
      → stopCurrentAndPrepare() (기존 Job 취소 + 상태 리셋)
      → 코루틴 시작 → TtsOrchestrator.speakWithHighlight()
        → 문장 분할 후 루프: onHighlight(idx) → speakKorean/speakEnglish()
          → BaseTtsPlayer.speak()
            → isSpeaking 폴링 대기 → tts.speak() → onStart 확인 → 재생 완료 대기
```

**주의**: `forceStopTts()` 대신 `stopCurrentAndPrepare()` 사용. 이중 stop 호출은 TTS 엔진을 불안정하게 만듦. 자세한 내용은 `domain/CLAUDE.md`의 TtsPlaybackController 섹션 참조.

### DI
- 싱글톤 바인딩: `di/AppModule.kt` (@Provides @Singleton)
- 자동 제공: `@Singleton` + `@Inject constructor` (TtsPlaybackController, MemorizeTestProgressTracker 등)
- **이중 등록 주의**: 일부 클래스가 @Inject constructor + @Provides 양쪽에 등록됨 (Hilt는 @Provides 우선 사용)
- ViewModel: `@HiltViewModel` + `@Inject constructor`

### 네비게이션
```
SplashActivity → MainActivity
                  ├── MainScreen (startDestination)
                  └── SettingsScreen
```

## 앱 진입점 & 루트 파일

| 파일 | 역할 |
|------|------|
| `OPicHelperApplication.kt` | `@HiltAndroidApp`. TtsOrchestrator, TtsPlaybackController, FullMemorizationUseCase, PlayMergedFileUseCase 싱글톤 보유 |
| `SplashActivity.kt` | 런처 Activity. 2초 스플래시 후 MainActivity 이동 |
| `MainActivity.kt` | `@AndroidEntryPoint`. RECORD_AUDIO 권한, WakeLock 수명주기, 백버튼 리소스 정리 |

## Assets 구조

```
assets/
  al/           — Advanced Low 레벨 (15개 JSON)
  ih/           — Intermediate High 레벨 (16개 JSON, qa_roleplay 포함)
  ih_raw/       — IH Raw 레벨 (15개 JSON, qa_roleplay 없음)
  im/           — Intermediate Mid 레벨 (15개 JSON)
```

JSON 포맷: `{ "title": "한글 카테고리명", "items": [{ id, question_en, question_ko, answers: { "AL": { answer_en, answer_ko, vocabulary, grammar, tips }, ... } }] }`
`theme` 필드는 일부 JSON에만 존재하며 파싱 시 무시됨.
새 JSON 추가 시 코드 수정 없이 assets에 넣기만 하면 자동 인식 (동적 카테고리 로딩).

## 코딩 컨벤션

### SOLID 원칙
- **SRP**: 각 클래스는 하나의 변경 이유만 가져야 함. ViewModel이 비대해지면 새 ViewModel로 분리
- **OCP**: TTS 플레이어 추가 시 `TtsPlayer` 인터페이스 구현체만 추가 (Orchestrator 수정 불필요)
- **LSP**: BaseTtsPlayer 하위 클래스는 치환 가능해야 함
- **ISP**: Repository 인터페이스는 UI 콜백을 받지 않아야 함 (SharedFlow 이벤트로 대체 완료)
- **DIP**: Domain 계층은 Data 계층을 직접 참조하지 않아야 함

### 네이밍
- Repository 인터페이스: `domain/repository/`에 정의
- Repository 구현체: `data/repository/`에 `~Impl` 접미사
- UseCase: `domain/usecase/`에 `~UseCase` 접미사
- StateFlow: `_` 접두사는 private, 공개는 읽기 전용

### Compose
- UI 컴포넌트: `presentation/ui/screen/MainScreenComponentsUI/`
- 재사용 컴포넌트: `presentation/ui/component/`
- 상태 구독: ViewModel의 StateFlow를 `collectAsState()`로 구독

### 로깅
- `AppLogger` 인터페이스(`domain/manager/AppLogger`) 사용. `android.util.Log` 직접 호출 금지
- 구현체: `AndroidLogger`(`data/manager/AndroidLogger`) — 유일한 `android.util.Log` 소비자
- AppLogger 메서드: `e(tag, msg, t?)`, `w(tag, msg)`

## 코드 리뷰 체크리스트

### 필수 확인
- [ ] Domain 계층이 Data 계층을 직접 import하지 않는지
- [ ] Data 계층이 Domain 구현체(UseCase, QaDataManager)를 직접 import하지 않는지
- [ ] Repository 인터페이스에 UI 콜백이 없는지
- [ ] CoroutineScope가 적절히 취소되는지 (메모리 누수 방지)
- [ ] StateFlow 업데이트가 스레드 안전한지
- [ ] TTS stop() 후 speak() 호출 시 경쟁 상태가 없는지

### 보안 확인
- [ ] API 키, 비밀번호 등이 코드에 하드코딩되지 않았는지
- [ ] SharedPreferences에 민감 정보를 평문 저장하지 않는지
- [ ] 권한이 최소한으로 선언되었는지

## 알려진 기술 부채

| 항목 | 상태 | 우선순위 |
|------|------|----------|
| WakeLock deprecated API (@Suppress("DEPRECATION") 처리) | 완화 | 낮음 |

상세 개선 계획은 [ARCHITECTURE_IMPROVEMENT_PLAN.md](ARCHITECTURE_IMPROVEMENT_PLAN.md) 참조.

### 완료된 리팩토링

| 항목 | 해결 커밋 |
|------|----------|
| MemorizeTestProgressTracker 경쟁 상태 | Mutex 도입 (0012) |
| BaseTtsPlayer CompletableDeferred 이중 완료 | AtomicBoolean 가드 (0013) |
| RecordingAudioPlayerImpl MediaPlayer 누수 | OnCompletionListener 추가 (0014) |
| AudioRecorderImpl 해제 누락 | stop/release 분리 try-catch (0014) |
| MainViewModel runBlocking ANR | suspend fun 전환 (0015) |
| 싱글톤 CoroutineScope 미해지 | Closeable 패턴 도입 (0016) |
| ExecuteFullMemorizationUseCase 중복 | 삭제 (0017) |
| QaDataManager Android 의존성 | ProgressPersistenceService 분리 (0018) |
| 과도한 Log.d | 경고/오류 로그만 유지 (0021, 0022) |
| 미사용 코드 (QaDataLoaderImpl, PlaybackEvent 등) | 일괄 삭제 (0020) |
| SettingsScreen의 MainViewModel 의존 | SettingsViewModel 분리 (0023) |
| Repository ISP 위반 (UI 콜백) | SharedFlow 이벤트로 전환 (0026) |
| Data→Domain 직접 참조 (MemorizeTestProgressTracker) | ProgressPersistenceService 사용 (0026) |
| MainViewModel God Class | QaBrowserViewModel + PlaybackViewModel 분리 (0027, 0028) |
| 고아 테스트 (MainViewModelTest, TtsViewModelTest) | 삭제 (0028) |
| 반복듣기 모드에서 답변 녹음 버튼 표시 버그 | isFullMemorizationMode 파생 소스 변경 (0030) |
| 컴파일 경고 24개 | unused param/deprecated API 수정 (0030) |
| NavigationState 스크립트/문장 인덱스 오염 | scriptIndex 필드 추가 (0046) |
| RepeatListeningUseCase 중복 로직 | 삭제, ExecuteRepeatListeningUseCase만 사용 (0047) |
| EnglishWritingTestRepoImpl QaDataManager 미사용 의존성 | 제거 (0047) |
| FullMemorizationUseCase 콜백 기반 상태 전달 | FullMemorizationState StateFlow 전환 (0047) |
| MemorizationUiState 15개 파생 불리언 | 확장 프로퍼티로 전환, updateUiState() 삭제 (0048) |
| EnglishWritingTestRepositoryImpl QaDataManager 직접 참조 | 미사용 의존성 제거 (0047) |
| Dual DI 등록 7개 클래스 | @Provides만 사용하도록 정리 (0037) |
| TtsPlaybackController setter 주입 | 생성자 주입으로 이미 전환됨 (0036) |
| ScriptProgress domain/repository 위치 | domain/entity로 이동 (0039) |
| AudioPlayerImpl/AudioRecorderImpl 경쟁 상태 | synchronized(lock)/@Synchronized 적용 (0055) |
| RecordingFileRepositoryImpl AtomicReference TOCTOU | Mutex 교체, !! 제거 (0055) |
| FullMemorizationUseCase 복합 상태 경쟁 | Mutex 적용, _isRecording/_isPlaying 제거 (0055) |
| TtsPlaybackController currentPlayJob 가시성 | @Volatile 추가, stopCurrentAndPrepare에 stop 추가 (0055) |
| MemorizationViewModel SRP 위반 (3모드 통합) | 3개 ViewModel 분리 + MemorizationModeCoordinator (0056) |
| MainScreen 다중 StateFlow 구독 | derivedStateOf 도입, LaunchedEffect 정리 (0057) |
| PlayStopToggleButton 중복 버튼 패턴 | 공통 컴포넌트 추출, 4개 버튼 위임 (0061) |
| QaDataManager navigate 패턴 중복 | navigateTo() 추출, saveCurrentProgress 데드 코드 제거 (0061) |
| PlaybackViewModel 이중 노출 (setMergedAudioPlaying) | 미사용 메서드 제거 (0061) |
| SharedPreferences 키 인라인 리터럴 | 키 상수화, 미사용 DI 바인딩 제거 (0061) |
| ViewModel stop/onLevelChanged 중복 | BaseMemorizationViewModel 추출 (0062) |
| Repository TTS 재생 패턴 중복 | BaseMemorizeTestRepository 추출 (0062) |
| MainScreen LaunchedEffect 크로스 VM 릴레이 | 코디네이터 이벤트 시스템으로 이관 (0062) |
| TTS 콜백-코루틴 변환 3단계 중복 | TtsSpeakResult 도입, onComplete 콜백 제거, CompletableDeferred 단일화 (0096) |
| TtsOrchestrator 중첩 CompletableDeferred 타임아웃 충돌 | speak() 직접 await로 간소화, speakAndGetDuration 제거 (0096) |
| TtsPlaybackController 이중 stop 경쟁 상태 | stopAndReset() 통합, Job 취소 1차 + orchestrator.stop 안전망 (0096) |
| TtsPlaybackController SRP 위반 (10개 StateFlow) | HighlightStateHolder 분리, 하이라이트 상태 단일 진실 공급원 (0096) |
| TtsPlayer 인터페이스 미사용 메서드 | speakWithHighlight, speakAndGetDuration 제거 (0096) |
| 영어 TTS 속도 제어 | 설정 슬라이더, UserPreferencesRepository 연동, speakAndWaitForCompletion 정리 (0100) |
| TtsOrchestrator DI 이중 등록 | @Inject constructor 제거, @Provides만 사용 (0102) |
| TtsOrchestrator _isSpeaking 경쟁 상태 | AtomicInteger 참조 카운터 도입 (0102) |
| 한글 문장 분할 미지원 | SentenceSplitter 공백 없는 마침표 분할 지원 (0103) |
| isPlaying TOCTOU | tts?.isSpeaking 제거, _isPlaying 단일 소스 (0103) |
| FullMemorizationUseCase Mutex 미보호 | hasRecording/clearRecording Mutex 적용 (0103) |
| TtsPlayer pause/resume 오해 유발 | 인터페이스에서 제거 (0104) |
| GoogleTtsPlayer/SamsungTtsPlayer 데드 코드 | destroy(), release() 중복 오버라이드 제거 (0104) |
| EnglishWritingTestRepositoryImpl Completed 누락 | 이벤트 발행 추가 (0104) |
| SettingsViewModel StateFlow 경쟁 | MutableStateFlow.update 사용 (0104) |
| MemorizationModeCoordinator 순환 의존성 | CurrentMode/ModeGroup을 domain 계층으로 이동 (0109) |
| FullMemorizationUseCase 즉시 상태 전환 | playRecordingWithHighlight() premature WithFile(true) 제거 (0109) |
| FullMemorizationViewModel onCleared() 취소된 scope | viewModelScope.launch 제거, close() 직접 호출 (0109) |
| BaseTtsPlayer 타임아웃 stale 콜백 | 리스너 해제 + tts.stop() 추가 (0109) |
| FullMemorizationUseCase mutex 점유 | playRecordingSimple() mutex는 상태체크에만 사용 (0110) |
| StateFlow .value = .value.copy() 경쟁 상태 | 전 ViewModel .update { it.copy() } 전환 (0110) |
| AudioFileManagerImpl 하드코딩 1024바이트 스킵 | mergeWithHeaderAnalysis() 폴백 제거 (0110) |
| AudioFileManagerImpl MediaExtractor 누수 | try/finally로 extractor.release 보장 (0110) |
| AudioFileManagerImpl 병합 실패 시 소스 삭제 | 검증 후에만 삭제, 실패 시 원본 유지 (0110) |
| RecordingFileRepositoryImpl 스레드 미안전 | @Volatile 추가 (0110) |
| TtsPlaybackController pauseTts/resumeTts 오해 유발 | stopAndMarkPaused/clearPausedState로 이름 변경 (0111) |
| PlayMergedFileUseCase 분할 정규식 불일치 | SentenceSplitter.split() 사용 (0111) |
| PlayMergedFileUseCase release() 누수 | checkFileJob 취소 추가 (0111) |
| MemorizeTestProgressTracker TOCTOU | persistChangedProgress() 단일 mutex.withLock 래핑 (0111) |
| UserLevel.valueOf 크래시 | try-catch 기본값 폴백 (0111) |
| LeveledQaDataLoader e.printStackTrace() | Log.e() 변경 (0111) |
| RecordingTimeManagerImpl 스레드 미안전 | @Synchronized 추가 (0111) |
| EnglishWritingTestRepositoryImpl 취소 시 파일 잔존 | CancellationException catch에서 파일 정리 (0111) |
| RepeatListeningRepositoryImpl TTS 실패 미처리 | 반환값 0L 체크, 실패 시 문장 스킵 (0112) |
| BaseMemorizationViewModel onLevelChanged 진행상황 미저장 | persistChangedProgress() 추가 (0112) |
| FullMemorizationViewModel startMode() 코루틴 누수 | modeJob 컨텍스트에서 launch (0112) |
| RepeatListeningViewModel handleAutoAdvance eventJob 누적 | cancelEventJob() 추가 (0112) |
| MainScreen permissionDenied 재생성 | remember { MutableStateFlow(false) } (0112) |
| GoogleTtsPlayer 중복 SDK 분기 | TIRAMISU/UPSIDE_DOWN_CAKE 통합 (0113) |
| GoogleTtsPlayer/SamsungTtsPlayer 불필요 getPitch() | 제거 (0113) |
| PlayMergedFileUseCase 미사용 currentItem | 파라미터 제거 (0113) |
| QuestionPlayButton/AnswerPlayButton 미사용 파라미터 | currentQuestion/currentAnswer 제거 (0113) |
| QuestionCard/AnswerCard 미사용 contentColor | 파라미터 제거 (0113) |
| SettingsScreen 하드코딩 버전 | PackageInfo에서 동적 읽기 (0113) |
| ProgressPersistenceServiceImpl 하드코딩 "opic_prefs" | 상수화 (0113) |
| BaseTtsPlayer 매직넘버 | IS_SPEAKING_POLL_INTERVAL 등 상수화 (0113) |
| TtsPlaybackController 하드코딩 500ms | MERGED_AUDIO_DELAY_MS 상수화 (0113) |
| AudioRecorderImpl SimpleDateFormat 매번 생성 | 캐시 (0113) |
| RecordingAudioPlayerImpl getDuration() 매번 생성 | 결과 캐시 (0113) |
| ViewModel 6개 android.util.Log 직접 사용 | AppLogger 주입 전환 (0126) |
| 미사용 메서드 4개 (playRecordingFile, getQaItemsByCategory, restoreAllOriginal, isModified) | 제거 (0126) |
| TtsPlaybackController 구체 클래스 | 인터페이스 + TtsPlaybackControllerImpl 분리 (0127) |
| TtsOrchestratorImpl domain/audio/ 위치 | data/audio/ 이동 (0127) |
| PlaybackViewModel 콜백 6개 (volatile var) | PlaybackActionListener 인터페이스 통합 (0127) |
| _stopMemorizationCallback onCleared 누수 | null 처리 추가 (0127) |
| ExecuteRepeatListeningUseCase / ExecuteEnglishWritingTestUseCase 순수 위임 | 제거, Repository 직접 주입 (0127) |
| Data 계층 12개 파일 + MainActivity android.util.Log | AppLogger 전환 (0128) |
| Domain 계층 Android import (Log, Context, PowerManager) | AppLogger + WakeLockController 분리로 해결 (0119, 0126, 0128) |
| UserPreferencesRepository ISP 위반 (15+ 메서드) | 6개 하위 인터페이스 분리 + 복합 인터페이스 유지 (0129) |
| ScriptEditRepositoryImpl 미사용 의존성 | UserPreferencesRepository 파라미터 제거 (0129) |
| 미사용 메서드 16개 (AudioFileManager 8, AudioPlayer 3, RecordingAudioPlayer 2, FullMemorizationUseCase 1, QaItemDao 1, LeveledQaDataLoader 1) | 인터페이스+구현체 제거 (0133) |
| !! 강제 언랩 3곳 | 안전한 패턴 교체 (0135) |
| 매직넘버 20+ | companion object 상수화 (0135) |
| getSentenceFromAnswer() 3곳 중복 | BaseMemorizationViewModel로 이동 (0135) |
| 매직넘버 15+ (Unicode 범위, TTS 속도, 단어수 임계값, 버퍼 크기, 검색어 길이) | companion object 상수화 (0136) |
| 파일명 접두어 3개 파일 분산 ("통암기_", "영작테스트_", "english_writing_") | FULL_MEMORIZATION_PREFIX, ENGLISH_WRITING_PREFIX 등 상수화 (0136) |
| BaseTtsPlayer deprecated onError @Suppress 누락 | @Suppress("DEPRECATION") 추가 (0136) |
| TtsPlaybackController ISP 위반 (21멤버) | TtsHighlightController 하위 인터페이스 분리 + 복합 인터페이스 유지 (0137) |
| QaDataManager ISP 위반 (17멤버) | QaContentReader, QaNavigator, QaSearch, QaDataLifecycle 하위 인터페이스 분리 + 복합 인터페이스 유지 (0137) |
| MemorizationModeCoordinator ISP 위반 (12멤버) | MemorizationStateObserver 하위 인터페이스 분리 + 복합 인터페이스 유지 (0137) |
| FullMemorizationUseCase/PlayMergedFileUseCase QaDataManager 전체 의존 | QaContentReader로 축소 (0137) |
| SearchQaItemsUseCase QaDataManager 전체 의존 | QaSearch로 축소 (0137) |
| PlaybackViewModel dead code 3개 (setAnswerCardFlipped, isCoordinatorRunning, isMergedFilePlaying) | 삭제 (0138) |
| PlaybackViewModel checkEnglishWritingTestMergedFile public | private 변경 (0138) |
| PlaybackState.isAnswerCardFlipped 미사용 필드 | 삭제 (0138) |
| QaBrowserViewModel onboarding SRP 위반 | OnboardingViewModel 추출 (0138) |
| TtsPlaybackControllerImpl stopAndMarkPaused spurious emission | resetPlayState() 대신 개별 필드 리셋 (0139) |
| AudioFileManagerImpl MediaMuxer stop() 누락 | muxer.stop() 추가 (0139) |
| AudioFileManagerImpl 무효 M4A raw byte 폴백 | 폴백 제거 (0139) |
| PlaybackViewModel.onCleared PlayMergedFileUseCase scope 누수 | release() → close() 변경 (0139) |
| FullMemorizationViewModel startMode() 코루틴 누적 | Job(modeJob)로 자식 코루틴 관리 (0139) |
| BaseMemorizationViewModel modeJob private | protected 변경 (0139) |
| SplashActivity delay(2000L) 매직넘버 | SPLASH_DELAY_MS 상수화 (0139) |
| MutableStateFlow .value = 경쟁 상태 (4개 파일) | .update {} 패턴으로 전환 (0140) |
| BaseMemorizationViewModel _uiState.value = → .update {} | 일관성 통일 (0141) |
| PlaybackViewModel dead code 3개 + PlaybackState 미사용 필드 | 삭제 (0142) |
| QaBrowserViewModel onboarding SRP 위반 | OnboardingViewModel 추출 (0142) |
| TtsPlaybackControllerImpl stopAndMarkPaused spurious emission | 개별 필드 리셋 (0143) |
| AudioFileManagerImpl MediaMuxer stop() 누락 | muxer.stop() 추가 (0143) |
| AudioFileManagerImpl 무효 M4A raw byte 폴백 | 폴백 제거 (0143) |
| PlaybackViewModel.onCleared PlayMergedFileUseCase scope 누수 | release() → close() 변경 (0143) |
| FullMemorizationViewModel startMode() 코루틴 누적 | Job(modeJob) 자식 관리 (0143) |
| BaseMemorizationViewModel modeJob private | protected 변경 (0143) |
| SplashActivity delay(2000L) 매직넘버 | SPLASH_DELAY_MS 상수화 (0143) |
| MutableStateFlow .value = 경쟁 상태 (6개 파일 추가) | .update {} 패턴 전환 (0144) |
| FullMemorizationUseCase DEFAULT_RECORDING_TIMES 하드코딩 | 실제 문장 수로 동적 계산 (0144) |
| RecordingFileRepositoryImpl 비결정적 파일 선택 | maxByOrNull { lastModified() } (0144) |
| BaseMemorizationViewModel QaDataManager 전체 의존 | QaContentReader + QaNavigator 분리 (0144) |
| MainActivity onBackPressed() deprecated | OnBackPressedDispatcher 전환 (0144) |
| UserPreferencesRepository .value = 경쟁 상태 | .update {} 패턴 전환 (0144) |
| MemorizationModeCoordinatorImpl .value = 경쟁 상태 | .update {} 패턴 전환 (0144) |
| data/CLAUDE.md EnglishWritingTestRepositoryImpl 위반 주석 | 실제 위반 아님, 제거 (0144) |
| BaseMemorizationViewModel.modeJob @Volatile 누락 | @Volatile 추가 (0145) |
| PlayMergedFileUseCase playJob/checkFileJob @Volatile 누락 | @Volatile 추가 (0145) |
| QaDataManagerImpl userLevelJob @Volatile 누락 | @Volatile 추가 (0145) |
| WakeLockControllerImpl wakeLock @Volatile 누락 | @Volatile 추가 (0145) |
| TtsOrchestratorImpl.stop() AtomicInteger invariant 위반 | activeSpeakCount.set(0) 제거, 자연 감소 허용 (0145) |
| TtsPlaybackControllerImpl .value = 경쟁 상태 (20건) | .update {} 패턴 전환 (0145) |
| TtsOrchestratorImpl .value = 경쟁 상태 (3건) | .update {} 패턴 전환 (0145) |
| HighlightStateHolder .value = 경쟁 상태 (8건) | .update {} 패턴 전환 (0145) |
| MemorizeTestProgressTracker .value = 경쟁 상태 (11건) | .update {} 패턴 전환 (0145) |
| MainActivity _permissionDenied .value = | .update {} 패턴 전환 (0145) |
| EditScriptViewModel QaDataManager 전체 의존 | QaDataLifecycle로 축소 (0145) |
| SettingsViewModel UserPreferencesRepository 전체 의존 | UserLevelPreferences + TtsPreferences + PlaybackPreferences (0145) |
| CurrentMode.group else catch-all | exhaustive when 전환 (0145) |
| OCP duplicated when dispatch 4곳 | MemorizationController 중앙화 (0146) |
| QaBrowserViewModel cleanupOnAppExit SRP 위반 | ProgressCleanupUseCase 추출 (0147) |
| PlaybackViewModel SRP 위반 (7 deps, 5 responsibilities) | PipStateAggregator 추출, 6 deps 3 responsibilities (0148) |
| PlaybackActionListener domain→presentation import 위반 | domain/audio/ 이동 (0149) |
| PipStateAggregator CoroutineScope 누수 | release()에 scope.cancel() 추가 (0149) |
| ProgressCleanupUseCase 취소 시 미저장 | withContext(NonCancellable) 추가 (0149) |
| TtsPlayer.isPlaying() 미사용 | 인터페이스+구현체 제거 (0149) |
| RecordingAudioPlayer.isPlaying 미사용 | 인터페이스+구현체 제거 (0149) |
| AudioPlayer.isPlaying 미사용 | 인터페이스+구현체 제거 (0149) |
| RecordingFileRepository.stopPlayingRecording() 미사용 | 인터페이스+구현체 제거 (0149) |
| TtsPlaybackController playMergedAudio/setQuestionHighlightIndex 미사용 | 인터페이스+구현체 제거 (0149) |
| FullMemorizationUseCase isRecording/isPlaying 미사용 | 제거 (0149) |
| FullMemorizationViewModel deleteRecording 미사용 | 제거 (0149) |
| MemorizeTestProgressTracker hasScriptProgress/clearScriptProgress 미사용 | 제거 (0149) |
| MainActivity isFinishing 미사용, 빈 onStop, 미사용 import | 제거 (0149) |
| FullMemorizationUseCase.clearRecording() 미사용 | 제거 (0151) |
| MemorizationModeCoordinator.cancelJobs()/isActive() 미사용 | 제거 (0151) |
| MemorizationStateObserver 인터페이스 미사용 | 제거, 속성을 MemorizationModeCoordinator에 직접 선언 (0151) |
| TtsHighlightController DI 바인딩 미사용 | 제거 (0151) |
| AudioFileManager.mergeAndSaveAudioFiles()/getFullMemorizationRecording() 미사용 | 제거 (0151) |
| ProgressPersistenceService.saveAppExitState()/loadAppExitState() 미사용 | 제거 (0151) |
| AppExitState 데이터 클래스 미사용 | 제거 (0151) |
| RepeatListeningRepository/EnglishWritingTestRepository getCurrentProgress/updateProgress/clearProgress 미사용 | 제거 (0151) |
| TestProgressData 데이터 클래스 미사용 | 제거 (0151) |
| QaDataManagerImpl ConcurrentHashMap 경쟁 상태 | Mutex로 직렬화 (0152) |
| CategoryProgress/AppExitState/TestProgressData 중복 | ScriptProgress로 통합 (0153) |
| QaDataManagerImpl observer 경쟁 상태 | setupUserLevelObserver에 mutex.withLock 추가 (0155) |
| AudioFileManager 병합 실패 시 소스 파일 삭제 | mergeAudioFiles 반환 타입 File→File?, null 체크 후 삭제 (0156) |
| CurrentMode/ModeGroup entity→usecase DIP 위반 | domain/entity/로 이동 (0157) |
| FullMemorizationUseCase 녹음 리소스 누수 | close()/cancelPlayback()에 stopRecording 추가 (0159) |
| QaDataManagerImpl 레벨 변경 시 stale data | loadQaItemsFromAssets()에서 맵 clear 후 로드 (0160) |
| PipStateAggregator.lastMemorizationGroup 캡슐화 | internal var → private var + 공개 읽기 전용 (0161) |
| isFullMemorizationRecordingPlaying 오해 유발 이름 | isFullMemorizationPlaying으로 변경 (0161) |
| SearchQaItemsUseCase 순수 위임 | 제거, QaBrowserViewModel→QaSearch 직접 의존 (0163) |
| QaDataManagerImpl O(N*C) filter 루프 | groupBy O(N) 전환, allItems 캐시, search 최적화 (0163) |
| AudioFileManagerImpl has+get 중복 메서드 | findEnglishWritingTestMergedFile() 통합 (0163) |
| AudioFileManagerImpl Regex 오버헤드 | startsWith 교체, getRecordingsDirectory() 추출 (0163) |
| RecordingTimeManagerImpl SharedPreferences 매번 읽기 | 메모리 캐시 도입 (0163) |
| TtsPlaybackControllerImpl _isPlaying 수동 동기화 | combine(_isQuestionPlaying, _isAnswerPlaying) 파생 StateFlow (0163) |
| MemorizeTestProgressTracker _hasProgress 수동 동기화 | progressMap.map {} 파생 StateFlow (0163) |
| MainActivity PiP 상태 busy-wait | LaunchedEffect 수집, remember+SideEffect 초기화 (0163) |
| BaseMemorizationViewModel KoreanHighlight 중복 | handleKoreanHighlight() 공통 메서드 추출 (0163) |
| OPicHelperApplication @Inject 필드 + onTerminate | 제거 (프로덕션 미호출) (0163) |
| Gson 매번 생성 | @Provides @Singleton 주입으로 전환 (0163) |
| LeveledQaDataLoader TypeToken 매번 생성 | companion val 캐시 (0163) |
| PlayMergedFileUseCase has+get 이중 호출 | findEnglishWritingTestMergedFile() 단일 호출 (0163) |
| EnglishWritingTestViewModel SimpleDateFormat 매번 생성 | companion val 캐시 (0163) |
| RepeatListeningViewModel Regex 매번 컴파일 | companion val 캐시 (0163) |
| AudioFileManager.hasEnglishWritingTestMergedFile 미사용 | 제거, getEnglishWritingTestMergedFile null 체크로 대체 (0166) |
| AudioFileManager.hasFullMemorizationRecording 미사용 | 제거 (0166) |
| RecordingFileRepository.deleteRecordingFile 미사용 | 제거 (0166) |
| ProgressPersistenceService.loadCategoryProgress 미사용 | 제거, loadAllCategoryProgress 사용 (0166) |
| AudioPlayer.play(File, onCompletion) 인터페이스 미사용 | 인터페이스에서 제거, 구현체는 private 유지 (0166) |
| BaseTtsPlayer _isPlaying AtomicBoolean 쓰기 전용 | 제거 (0168) |
| TtsOrchestrator.speak() speakAndWaitForCompletion과 동일 | 인터페이스+구현체에서 제거 (0168) |
| QaItemDao.getSeededLevels() 미사용 | 제거 (0169) |
| AudioFileManagerImpl 미사용 상수 2개 | 제거 (0169) |
| clearAllProgress() 미사용 체인 | 전체 제거 (0170) |
| ProgressPersistenceServiceImpl KEY_APP_EXIT_STATE 미사용 | 제거 (0170) |
| AssetSeeder Gson() 루프 내 3회 생성 | 싱글톤 주입 전환 (0171) |
| AudioFileManagerImpl saveRecordingFile 중복 디렉토리 생성 | getRecordingsDirectory() 위임 (0171) |
| MemorizeTestProgressTracker hasProgress 미사용 + CoroutineScope 누수 | 제거 (0172) |
| PlaybackState.hasProgress 미사용 필드 | 제거 (0173) |
| EditScriptViewModel isModified 미사용 StateFlow | 제거 (0173) |
| QuestionCard isFlipped 항상 false | FlipCard 제거, 직접 렌더링 (0174) |
| AnswerCard isRepeatListeningCardFlipped 중복 파라미터 | 제거 (0174) |
| MainScreen AnswerCard 하이라이트 중복 OR 조건 | 단순화 (0174) |
| QaBrowserViewModel.clearError() 미사용 | 제거 (0174) |
| AudioPlayerImpl/RecordingAudioPlayerImpl MediaPlayer 해제 로직 중복 | BaseMediaPlayer 추출 (0176) |
| TtsPlaybackController.close() 미호출 + coroutineScope 누수 | PlaybackViewModel.onCleared() cleanupTts → close() 변경 (0178) |
| PlaybackPreferences.setAutoAdvance() 미사용 | 제거 (0178) |
| QaNavigator.clearError() 미사용 | 제거 (0178) |
| TtsOrchestrator.isSpeaking 외부 읽기 없음 | 인터페이스에서 제거 (0178) |
| CurrentMode 미사용 enum 4개 (QUESTION_PLAY, ANSWER_PLAY, ENGLISH_WRITING_PLAYING, ENGLISH_WRITING_WITH_FILE) | 제거 (0179) |
| CoordinatorEvent.LevelChanged 미발행 | 제거 (0179) |
| FullMemorizationViewModel.refreshRecordingStatus() 외부 호출 없음 | private 전환 (0179) |
| AndroidLogger/TtsServiceControllerImpl 이중 DI 등록 | @Inject constructor 제거 (0179) |
| QaItemEntityMappers 독립 Gson 인스턴스 | QaItemEntityMapper 클래스 + Gson 주입 (0180) |
| RepeatListening/EnglishWritingTest Repository 한글 TTS+하이라이트 중복 | BaseMemorizeTestRepository.playKoreanWithHighlight() 추출 (0180) |
| MainScreen 5개 중복 Snackbar LaunchedEffect | merge + 단일 collect로 통합 (0182) |
| MainScreen 536줄 과도한 길이 | 10단계 분해로 306줄로 축소 (0183-0192) |
| TtsPlaybackControllerImpl/MemorizationModeCoordinatorImpl 이중 DI 등록 | @Inject constructor 제거, @Provides에서 직접 생성 (0193) |
| domain/CLAUDE.md ScriptProgress 잘못된 분류 | entity/로 이동 (0193) |

## Git 커밋 규칙

- 커밋 메시지는 한글로 작성
- 커밋 메시지에 AI 코딩 관련 내용(Co-Authored-By 등)을 포함하지 않음
- 커밋 생성 후 반드시 패치 파일을 생성: `git format-patch -1 HEAD`
- 커밋 메시지 마지막에 패치 파일명을 포함: `Patch: 000X-xxx.patch`
- 패치 파일명 형식: `NNNN-간단한설명.patch` (N은 0패딩 순번)

### 커밋 메시지 형식
```
<type>: <한글 설명>

Patch: 000X-xxx.patch
```

## 빌드 명령어

```bash
./gradlew assembleDebug          # 디버그 빌드
./gradlew testDebugUnitTest      # 단위 테스트
./gradlew connectedDebugAndroidTest  # 계측 테스트
./gradlew lint                   # 정적 분석
```
