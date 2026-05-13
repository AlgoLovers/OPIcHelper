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
| `TtsOrchestrator` | 직접 제공 (Application context 주입) | @Provides @Singleton |
| `TtsPlaybackController` | `@Inject constructor` + `@Singleton` | **Hilt 자동 제공** (AppModule에 명시 없음) |

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
| `QaDataLoader` | `LeveledQaDataLoader` | @Provides @Singleton |
| `QaDataManager` | 직접 제공 (ProgressPersistenceService 주입) | @Provides @Singleton |
| `EnglishWritingTestRepository` | `EnglishWritingTestRepositoryImpl` | @Provides @Singleton |
| `RepeatListeningRepository` | `RepeatListeningRepositoryImpl` | @Provides @Singleton |
| `RecordingFileRepository` | `RecordingFileRepositoryImpl` | @Provides @Singleton |
| `RecordingTimeManager` | `RecordingTimeManagerImpl` | @Provides @Singleton |
| `UserPreferencesRepository` | `UserPreferencesRepository` (data 계층 클래스) | @Provides @Singleton |
| `ProgressPersistenceService` | `ProgressPersistenceServiceImpl` | @Provides @Singleton |
| `AuthRepository` | `AuthRepository` (data 계층 클래스) | @Provides @Singleton |

### 인프라
| 바인딩 | 제공 방식 |
|--------|----------|
| `SharedPreferences` | @Provides (파일명: `opic_prefs`, `@Named` 없음) |
| `MemorizeTestProgressTracker` | @Provides @Singleton |
| `WakeLockManager` | @Provides @Singleton |

### 주의사항
- ViewModel은 `@HiltViewModel` + `@Inject constructor`로 자동 주입 (모듈에서 제공하지 않음)
- `SharedPreferences`는 `@Named` 없이 제공됨. 다른 prefs (user_prefs, auth_prefs, recording_times)는 각 Repository에서 `Context.getSharedPreferences()`으로 직접 생성
- `QaDataManager`는 `ProgressPersistenceService`를 주입받아 Android 의존성 분리 완료

## 규칙
- 새 Repository 인터페이스 추가 시: domain에 인터페이스 → data에 구현체 → AppModule에 @Provides 바인딩
- 모든 싱글톤 바인딩은 이 파일 하나에서 관리
- AppModule이 `object`이므로 @Binds 불가, 모든 바인딩은 @Provides 사용
