# DI Module — Hilt 의존성 주입 설정

## 파일
`di/AppModule.kt` — 단일 Hilt 모듈 (`@InstallIn(SingletonComponent::class)`, Kotlin `object`)

**주의**: AppModule은 `object`이므로 `@Binds` 사용 불가. 모든 바인딩은 `@Provides`로 제공됨.

## 바인딩 구성

### TTS
| 바인딩 | 구현체 | 제공 방식 |
|--------|--------|----------|
| `@Named("google") TtsPlayer` | `GoogleTtsPlayer` | @Provides @Singleton |
| `@Named("samsung") TtsPlayer` | `SamsungTtsPlayer` | @Provides @Singleton |
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
| 바인딩 | 구현체 | 제공 방식 |
|--------|--------|----------|
| `QaDataLoader` | `RoomQaDataLoader` | @Provides @Singleton |
| `@Named("asset") QaDataLoader` | `LeveledQaDataLoader` | @Provides @Singleton |
| `QaDataManager` | `QaDataManagerImpl` | @Provides @Singleton |
| `QaContentReader` | `QaDataManager` (위임) | @Provides @Singleton |
| `QaNavigator` | `QaDataManager` (위임) | @Provides @Singleton |
| `QaSearch` | `QaDataManager` (위임) | @Provides @Singleton |
| `QaDataLifecycle` | `QaDataManager` (위임) | @Provides @Singleton |
| `EnglishWritingTestRepository` | `EnglishWritingTestRepositoryImpl` | @Provides @Singleton |
| `RepeatListeningRepository` | `RepeatListeningRepositoryImpl` | @Provides @Singleton |
| `RecordingFileRepository` | `RecordingFileRepositoryImpl` | @Provides @Singleton |
| `RecordingTimeManager` | `RecordingTimeManagerImpl` | @Provides @Singleton |
| `UserPreferencesRepository` | `UserPreferencesRepository` (data 계층 클래스) | @Provides @Singleton |
| `ProgressPersistenceService` | `ProgressPersistenceServiceImpl` | @Provides @Singleton |
| `ScriptEditRepository` | `ScriptEditRepositoryImpl` | @Provides @Singleton |
| `StudySessionRepository` | `StudySessionRepositoryImpl` | @Provides @Singleton |
| `StudySessionRecorder` | `StudySessionRepository` (위임) | @Provides @Singleton |
| `StudySessionStatisticsReader` | `StudySessionRepository` (위임) | @Provides @Singleton |
| `DataSeeder` | `AssetSeeder` | @Provides @Singleton |

### Preferences 하위 인터페이스 (UserPreferencesRepository 위임)
| 바인딩 | 위임 소스 | 제공 방식 |
|--------|----------|----------|
| `UserLevelPreferences` | `UserPreferencesRepository` | @Provides @Singleton |
| `TtsPreferences` | `UserPreferencesRepository` | @Provides @Singleton |
| `PlaybackPreferences` | `UserPreferencesRepository` | @Provides @Singleton |
| `OnboardingPreferences` | `UserPreferencesRepository` | @Provides @Singleton |
| `MemorizeLevelPreferences` | `UserPreferencesRepository` | @Provides @Singleton |
| `AppDataPreferences` | `UserPreferencesRepository` | @Provides @Singleton |

### 인프라
| 바인딩 | 구현체 | 제공 방식 |
|--------|--------|----------|
| `AppLogger` | `AndroidLogger` | @Provides @Singleton |
| `Gson` | `Gson()` | @Provides @Singleton |
| `WakeLockController` | `WakeLockControllerImpl` | @Provides @Singleton |
| `TtsServiceController` | `TtsServiceControllerImpl` | @Provides @Singleton |
| `MemorizationModeCoordinator` | `MemorizationModeCoordinatorImpl` | @Provides @Singleton |
| `MemorizeTestProgressTracker` | `@Inject constructor` + `@Singleton` | **Hilt 자동 제공** |
| `AppDatabase` | Room databaseBuilder | @Provides @Singleton |
| `QaItemDao` | `db.qaItemDao()` | @Provides |

### 주의사항
- ViewModel은 `@HiltViewModel` + `@Inject constructor`로 자동 주입 (모듈에서 제공하지 않음)
- SharedPreferences는 각 Repository에서 `Context.getSharedPreferences()`으로 직접 생성 (opic_prefs, user_prefs, study_sessions, recording_times)
- `QaDataManager`는 `QaDataLoader`, `UserLevelPreferences`, `ProgressPersistenceService`, `DataSeeder`, `AppLogger`를 주입받음
- `TtsPlaybackController`는 `TtsOrchestrator`, `HighlightStateHolder`, `AppLogger`를 주입받음
- `QaContentReader`/`QaNavigator`/`QaSearch`/`QaDataLifecycle`은 모두 `QaDataManager` 싱글톤 인스턴스를 위임 (ISP 분리)
- `StudySessionRecorder`/`StudySessionStatisticsReader`은 `StudySessionRepository` 싱글톤 인스턴스를 위임 (CQRS 분리)
- `AppDatabase`는 `fallbackToDestructiveMigration()` 사용 — 스키마 변경 시 데이터 손실 주의

## 규칙
- 새 Repository 인터페이스 추가 시: domain에 인터페이스 → data에 구현체 → AppModule에 @Provides 바인딩
- 모든 싱글톤 바인딩은 이 파일 하나에서 관리
- AppModule이 `object`이므로 @Binds 불가, 모든 바인딩은 @Provides 사용
- 이중 등록을 피할 것: `@Inject constructor`만 사용하거나 `@Provides`만 사용할 것
