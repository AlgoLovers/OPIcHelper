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
- `QaBrowserViewModel`: QA 데이터 탐색, 카테고리, 암기레벨, 앱 종료 정리. 의존성 3개 (QaDataManager, UserPreferencesRepository, MemorizeTestProgressTracker)
- `PlaybackViewModel`: TTS 재생, 병합 파일 재생, 생명주기. 의존성 3개 (TtsPlaybackController, PlayMergedFileUseCase, TtsOrchestrator)
- `RepeatListeningViewModel`: 반복듣기 모드 전담. 의존성 6개 (ExecuteRepeatListeningUseCase, TtsPlaybackController, QaDataManager, MemorizeTestProgressTracker, UserPreferencesRepository, MemorizationModeCoordinator)
- `EnglishWritingTestViewModel`: 영작테스트 모드 전담. 의존성 5개 (ExecuteEnglishWritingTestUseCase, TtsPlaybackController, QaDataManager, MemorizeTestProgressTracker, MemorizationModeCoordinator)
- `FullMemorizationViewModel`: 통암기 모드 전담. 의존성 3개 (FullMemorizationUseCase, QaDataManager, MemorizationModeCoordinator)
- `MemorizationModeCoordinator`: 3개 모드 상호 배제, 상태 머신, Job 관리. @Singleton
- `SettingsViewModel`: 설정 화면 전용. UserPreferencesRepository + TtsOrchestrator

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
- Tag: 클래스명 사용
- Log.d (디버그) 제거, Log.w (경고)와 Log.e (오류)만 유지
- **현재 위반**: Data 계층 일부 파일에 Log.d 잔존

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
| Domain 계층 Android import (Log, Context, PowerManager) | 미해결 | 낮음 |
| Data 계층 Log.d 잔존 (RecordingFileRepositoryImpl 등) | 미해결 | 낮음 |
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
