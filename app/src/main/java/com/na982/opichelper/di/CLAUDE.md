# DI Module — Hilt 의존성 주입 설정

## 파일
`di/AppModule.kt` — 단일 Hilt 모듈 (`@InstallIn(SingletonComponent::class)`, Kotlin `object`)

**주의**: AppModule은 `object`이므로 `@Binds` 사용 불가. 모든 바인딩은 `@Provides`로 제공됨.

## 바인딩 구성

### TTS
| 바인딩 | 구현체 | 제공 방식 |
|--------|--------|----------|
| `@Named("google") TtsPlayer` | `GoogleTtsPlayer` | @Provides |
| `@Named("samsung") TtsPlayer` | `SamsungTtsPlayer` | @Provides |
| `TtsOrchestrator` | `TtsOrchestratorImpl` (data/audio/) | @Provides @Singleton |
| `TtsPlaybackController` | `TtsPlaybackControllerImpl` (data/audio/) | @Provides @Singleton |
| `HighlightStateHolder` | `@Inject constructor` + `@Singleton` | **Hilt 자동 제공** (하이라이트 상태 단일 진실 공급원) |

### 오디오
| 바인딩 | 구현체 | 제공 방식 |
|--------|--------|----------|
| `AudioRecorder` | `AudioRecorderImpl` | @Provides @Singleton |
| `AudioPlayer` | `AudioPlayerImpl` | @Provides @Singleton |
| `RecordingAudioPlayer` | `RecordingAudioPlayerImpl` | @Provides @Singleton |
| `AudioFileManager` | `AudioFileManagerImpl` | @Provides @Singleton |

### Repository
| 바인딩 | 구현체 | 제공 방식 | 이중 등록 |
|--------|--------|----------|----------|
| `QaDataLoader` | `LeveledQaDataLoader` | @Provides @Singleton | — |
| `QaDataManager` | 직접 제공 | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |
| `EnglishWritingTestRepository` | `EnglishWritingTestRepositoryImpl` | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |
| `RepeatListeningRepository` | `RepeatListeningRepositoryImpl` | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |
| `RecordingFileRepository` | `RecordingFileRepositoryImpl` | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |
| `RecordingTimeManager` | `RecordingTimeManagerImpl` | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |
| `UserPreferencesRepository` | `UserPreferencesRepository` (data 계층 클래스) | @Provides @Singleton | — |
| `ProgressPersistenceService` | `ProgressPersistenceServiceImpl` | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |
| `AuthRepository` | `AuthRepository` (data 계층 클래스) | @Provides @Singleton | — |

### 인프라
| 바인딩 | 제공 방식 | 이중 등록 |
|--------|----------|----------|
| `SharedPreferences` | @Provides @Singleton (opic_prefs) | — |
| `MemorizeTestProgressTracker` | **Hilt 자동 제공** (@Inject constructor + @Singleton) | — |
| `WakeLockManager` | @Provides @Singleton | ⚠️ @Inject constructor도 있음 |

### 이중 등록 설명
⚠️ 표시가 있는 7개 클래스는 `@Singleton @Inject constructor`와 `@Provides @Singleton` 양쪽에 등록됨.
Hilt는 `@Provides`를 우선 사용하므로 `@Inject constructor`의 `@Singleton`은 무시됨. 혼란 방지를 위해 한쪽만 유지 권장.

### 주의사항
- ViewModel은 `@HiltViewModel` + `@Inject constructor`로 자동 주입 (모듈에서 제공하지 않음)
- `SharedPreferences`는 `opic_prefs` 하나만 @Provides로 제공. 다른 prefs (user_prefs, auth_prefs, recording_times)는 각 Repository에서 `Context.getSharedPreferences()`으로 직접 생성
- `QaDataManager`는 `ProgressPersistenceService`와 `MemorizeTestProgressTracker`를 주입받음
- `TtsPlaybackController`는 생성자 주입으로 TtsOrchestrator + HighlightStateHolder를 받음

## 규칙
- 새 Repository 인터페이스 추가 시: domain에 인터페이스 → data에 구현체 → AppModule에 @Provides 바인딩
- 모든 싱글톤 바인딩은 이 파일 하나에서 관리
- AppModule이 `object`이므로 @Binds 불가, 모든 바인딩은 @Provides 사용
- 이중 등록을 피할 것: `@Inject constructor`만 사용하거나 `@Provides`만 사용할 것
