# Architecture

## 개요

OPIc Helper는 Clean Architecture + MVVM 패턴을 기반으로 설계된 Android 앱입니다. 세 개의 계층으로 분리되어 있으며, 의존성은 항상 외부(Data)에서 내부(Domain) 방향으로 향합니다.

## 계층 구조

```
Presentation Layer  →  Domain Layer  ←  Data Layer
(안드로이드 의존)       (순수 Kotlin)      (안드로이드 의존)
```

## Domain Layer

비즈니스 로직과 규칙을 담당하는 핵심 계층. 안드로이드 프레임워크에 의존하지 않습니다.

### Entity (`domain/entity/`)

| 클래스 | 설명 |
|--------|------|
| `QaItem` | QA 데이터 모델 (질문, 레벨별 답변 맵) |
| `UserLevel` | OPIc 레벨 enum (AL, IH, IH_RAW, IM) |
| `PlaybackState` | 오디오 재생 상태 (질문/답변/반복/병합) |
| `RepeatListeningData` | 반복듣기 모드 데이터 |
| `TtsConstants` | TTS 설정 상수 (속도, 피치, 버전별 설정) |

### Repository Interface (`domain/repository/`)

| 인터페이스 | 설명 |
|------------|------|
| `QaDataLoader` | QA JSON 데이터 로딩 |
| `QaDataManager` | QA 데이터 상태 관리 (카테고리, 인덱스, 현재 아이템) |
| `AudioFileManager` | 오디오 파일 생명주기 관리 |
| `ProgressPersistenceService` | 진행 상황 영속화 |
| `RecordingFileRepository` | 녹음 파일 CRUD 및 재생 |
| `RecordingTimeManager` | 문장별 녹음 시간 관리 |
| `EnglishWritingTestRepository` | 영작 테스트 실행 및 진행 관리 |
| `RepeatListeningRepository` | 반복 듣기 실행 및 진행 관리 |
| `FullMemorizationRepository` | 통암기 모드 실행 |
| `UserPreferencesRepository` | 사용자 설정 (레벨, TTS 속도) |

### UseCase (`domain/usecase/`)

| 클래스 | 설명 |
|--------|------|
| `ExecuteRepeatListeningUseCase` | 반복 듣기 실행 (한영 교차 재생) |
| `ExecuteEnglishWritingTestUseCase` | 영작 테스트 실행 |
| `FullMemorizationUseCase` | 통암기 모드 실행 (질문재생→녹음→재생) |
| `RepeatListeningUseCase` (RepeatListeningService) | 반복 듣기 워크플로우 서비스 |
| `EnglishWritingTestUseCase` | 영작 테스트 비즈니스 로직 |
| `MemorizeTestProgressTracker` | 암기 진행 상황 추적 (메모리 + 영속화) |

### Audio (`domain/audio/`)

| 인터페이스/클래스 | 설명 |
|-------------------|------|
| `TtsPlayer` | TTS 플레이어 계약 (speak, stop, pause, resume, highlight) |
| `TtsOrchestrator` | 언어 감지 → 적절한 TTS 라우팅 + 한글 TTS 폴백 |
| `TtsPlaybackController` | TTS 재생 상태 관리 (질문/답변/하이라이트 StateFlow) |
| `AudioPlayer` | 오디오 파일 재생 인터페이스 |
| `AudioRecorder` | 오디오 녹음 인터페이스 |
| `RecordingAudioPlayer` | 녹음 파일 재생 인터페이스 |

## Data Layer

Domain 계층의 인터페이스를 구현하고 실제 데이터 소스와 통신합니다.

### TTS 구현체 (`data/audio/`)

```
BaseTtsPlayer (abstract)
├── GoogleTtsPlayer   (영문, Locale.US, 버전별 속도 최적화)
└── SamsungTtsPlayer  (한글, Locale.KOREAN, 버전별 속도 최적화)
```

- `BaseTtsPlayer`: Android TextToSpeech 공통 로직 (초기화, speak, stop, release)
- 언어 감지는 `TtsOrchestrator`에서 수행 (한글 유니코드 범위: 0xAC00-0xD7AF, 0x3131-0x318E)
- 한글 TTS 폴백: Samsung TTS → (향후 Naver/Kakao 추가 가능)

### Repository 구현체 (`data/repository/`)

모든 구현체는 Hilt `@Singleton`으로 관리됩니다. DI 설정은 `di/AppModule.kt`에 집중되어 있습니다.

### 데이터 소스

- **QA 데이터**: `assets/{al,ih,ih_raw,im}/`의 JSON 파일 (레벨별 15개 카테고리, 총 60개)
- **사용자 설정**: SharedPreferences (`opic_prefs`, `auth_prefs`)
- **녹음 파일**: 내부 저장소 (`app/files/`)

## Presentation Layer

### ViewModel 구조

```
MainViewModel (QA 탐색 + 상태 조합)
└── MemorizationViewModel (암기 테스트 상태 — UseCase 실행)
```

- `MainViewModel`: AppState 단일 StateFlow로 QA 데이터 + TTS + UI 상태를 조합. 영작테스트 병합 파일 재생은 PlayMergedFileUseCase에 위임
- `MemorizationViewModel`: CurrentMode enum으로 반복듣기/영작테스트/통암기 상태 관리

### Navigation

```
SplashActivity → MainActivity
                  ├── MainScreen (startDestination)
                  ├── SettingsScreen
                  └── LoginScreen
```

### UI 컴포넌트 분리

`MainScreenComponentsUI/`에 메인 화면의 14개 UI 컴포넌트가 분리되어 있습니다.

## DI 구조

Hilt를 사용한 의존성 주입. 모든 싱글톤 바인딩은 `AppModule.kt`에 정의되어 있습니다.

### 주요 DI 그래프

```
TtsOrchestrator ← @Named("google") GoogleTtsPlayer
                ← @Named("samsung") SamsungTtsPlayer

MainViewModel ← QaDataManager, TtsPlaybackController
              ← MemorizeTestProgressTracker, PlayMergedFileUseCase
              ← RepeatListeningUseCase, UserPreferencesRepository

MemorizationViewModel ← TtsPlaybackController, QaDataManager
                      ← ExecuteRepeatListeningUseCase
                      ← ExecuteEnglishWritingTestUseCase
                      ← FullMemorizationUseCase
                      ← MemorizeTestProgressTracker
```

## 해결된 아키텍처 이슈

| 이슈 | 해결 내용 |
|------|-----------|
| ProgressPersistenceService 계층 위반 | domain에 인터페이스 생성, data에 구현체 이동 |
| AppExitState/CategoryProgress 중복 | domain/entity/ProgressData.kt로 통합 이동 |
| UserPreferencesRepository 인터페이스 미구현 | data 클래스가 domain 인터페이스 구현하도록 수정 |
| CoroutineScope 누수 (QaDataManager, TtsPlaybackController, FullMemorizationUseCase) | SupervisorJob + 취소 가능한 스코프로 수정 |
| TtsPlaybackController 레이스 컨디션 | stopTts를 동기화, Job 추적으로 수정 |
| MediaPlayer 누수 (getDuration) | try-finally로 release 보장 |
| clearRecording 파일 미삭제 | 실제 파일 삭제 로직 추가 |
| 파일명/클래스명 불일치 | EnglishWritingTestUseCase→ExecuteEnglishWritingTestUseCase, RepeatListeningService→RepeatListeningUseCase |
| 미사용 코드 | ApiKeys, QuestionCategory, Result, PlaybackState, TtsConstants, GetCategoriesUseCase, LoadQaItemsUseCase, SelectCategoryUseCase 삭제 |
| MainViewModel God Class | 영작테스트 병합 파일 재생을 PlayMergedFileUseCase로 분리, 의존성 9→6개, 760→247라인 |
| TtsPlaybackController 데드코드 | bindTtsService/unbindTtsService, onStopOtherMemorizationMode, stopQuestion/stopAnswer, playAudioFile/stopAudio, audioPlayer 의존성 제거. 459→187라인 |
| TtsViewModel 미사용 | 프로덕션에서 미사용으로 삭제 |
| MemorizationViewModel 인라인 정의 | CurrentMode, MemorizationUiState를 별도 파일로 분리 |

## 남은 아키텍처 이슈

| 이슈 | 설명 | 심각도 |
|------|------|--------|
| Repository 인터페이스 UI 결합 | 여러 Repository 인터페이스가 UI 콜백을 받음 | 높음 |
| QaDataManager data 계층 참조 | LeveledQaDataLoader 직접 import | 중간 |
| MemorizationViewModel SRP 위반 | 3개 모드를 단일 ViewModel에서 관리 | 중간 |
| ScriptProgress 위치 | domain/repository에 있으나 entity에 가까움 | 낮음 |
| WakeLockManager deprecated | SCREEN_BRIGHT_WAKE_LOCK 대신 FLAG_KEEP_SCREEN_ON 권장 | 낮음 |
