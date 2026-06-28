# Data Layer — 구현체 & 인프라

Domain 계층의 인터페이스를 구현하고, Android 프레임워크 의존성(Context, SharedPreferences, MediaRecorder 등)을 처리.

## 패키지 구조

패키지 구조: [ARCHITECTURE_DATA.md 섹션 2](.claude/architecture/ARCHITECTURE_DATA.md)

## audio/ — TTS & 오디오 하드웨어

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `BaseTtsPlayer.kt` | Android TTS 추상 베이스 | speakMutex로 직렬화, TtsSpeakResult 반환. CancellationException 시 tts?.stop() |
| `GoogleTtsPlayer.kt` | 영어 TTS (Locale.US) | `@Named("google")` 주입 |
| `SamsungTtsPlayer.kt` | 한국어 TTS (Locale.KOREAN) | `@Named("samsung")` 주입. 한글 TTS 실패 시 폴백은 TtsOrchestrator에서 처리 |
| `AudioRecorderImpl.kt` | MediaRecorder 기반 녹음 (AAC/M4A) | stopRecording 내부에서 release 자동 호출. Context 필요 |
| `AudioPlayerImpl.kt` | MediaPlayer 기반 오디오 재생 | BaseMediaPlayer 상속 |
| `RecordingAudioPlayerImpl.kt` | 녹음 재생 전용 MediaPlayer | BaseMediaPlayer 상속 |
| `BaseMediaPlayer.kt` | MediaPlayer 공통 베이스 추상 클래스 | releasePlayer(), prepareAndStart() 제공. OnCompletionListener/OnErrorListener에서 자동 release |
| `TtsOrchestratorImpl.kt` | 언어 감지→TTS 라우팅 구현체 | GoogleTtsPlayer(영어) + SamsungTtsPlayer(한국어). activeSpeakCount AtomicInteger 참조 카운팅 |
| `TtsPlaybackControllerImpl.kt` | 재생 상태 관리 구현체 | TtsPlaybackController + Closeable. CoroutineScope(SupervisorJob + Main) 자체 관리. isPlaying = combine 파생 |

BaseTtsPlayer.speak() 재생 보장: [ARCHITECTURE_DATA.md 섹션 3](.claude/architecture/ARCHITECTURE_DATA.md)

SDK 버전별 기본 속도: GoogleTtsPlayer/SamsungTtsPlayer 참조

## local/ — Room DB & 데이터 시딩

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `AppDatabase.kt` | Room 데이터베이스 | fallbackToDestructiveMigration() |
| `QaItemDao.kt` | Room DAO | CRUD 쿼리 |
| `QaItemEntity.kt` | Room Entity | @PrimaryKey id, answerJson |
| `QaItemEntityMappers.kt` | Entity↔Domain 매퍼 | QaItemEntityMapper 클래스. Gson 주입 |
| `AssetSeeder.kt` | 초기 데이터 시딩 | @Named("asset") QaDataLoader + QaItemDao + Gson 사용 |

## manager/ — 인프라 매니저

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `AndroidLogger.kt` | AppLogger 구현체 | 유일한 `android.util.Log` 소비자 |
| `WakeLockControllerImpl.kt` | WakeLockController 구현체 | 30분 안전 타임아웃. @Suppress("DEPRECATION") |

## repository/ — 데이터 영속성 & 비즈니스 로직 구현

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `QaDataManagerImpl.kt` | QA 데이터 관리 | Mutex로 상태 직렬화. ConcurrentHashMap 카테고리 캐싱. UserLevel 변경 시 자동 리로드 |
| `LeveledQaDataLoader.kt` | JSON assets → QaItem 파싱 | 새 JSON 추가 시 코드 수정 불필요 |
| `RoomQaDataLoader.kt` | Room DB → QaItem 로딩 | QaItemDao + QaItemEntityMapper |
| `BaseMemorizeTestRepository.kt` | 암기 테스트 공통 베이스 (추상) | SharedFlow 이벤트 버스. playKoreanWithHighlight 템플릿 메서드 |
| `RepeatListeningRepositoryImpl.kt` | 반복듣기: 한국어→영어 N회 반복 | BaseMemorizeTestRepository 상속. AtomicInteger extraRepetitions. calculateAdaptiveDelay |
| `EnglishWritingTestRepositoryImpl.kt` | 영작테스트: 한국어→녹음 반복 | BaseMemorizeTestRepository 상속. CancellationException 시 파일 정리 |
| `ProgressPersistenceServiceImpl.kt` | 진행상황 SharedPreferences 영속화 | NavigationState 저장/로드 포함 |
| `UserPreferencesRepository.kt` | 사용자 설정 관리 | Domain 복합 인터페이스 구현. Dual-write (StateFlow + SharedPreferences.apply) |
| `RecordingFileRepositoryImpl.kt` | 녹음 파일 CRUD, 재생 | Mutex로 동기화 |
| `RecordingTimeManagerImpl.kt` | 문장별 녹음 시간 관리 | Gson 직렬화. 메모리 캐시 도입 |
| `AudioFileManagerImpl.kt` | 오디오 파일 병합/저장/삭제 | mergeAudioFiles() MediaCodec만 사용 (폴백 없음). 실패 시 null 반환, 원본 유지 |
| `ScriptEditRepositoryImpl.kt` | 스크립트 편집 | 문장 수 변경 시 녹음 시간/진행상황 초기화 |
| `StudySessionRepositoryImpl.kt` | 학습 세션 기록 | Gson으로 StudyDailyRecord 직렬화. daily_{date} 키 |

## usecase/ — UseCase 구현체

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `MemorizationModeCoordinatorImpl.kt` | 모드 코디네이터 구현체 | AtomicReference<ModeGroup> + synchronized. MutableSharedFlow(extraBufferCapacity=1) |

SharedPreferences 키 맵: [ARCHITECTURE.md 섹션 6](.claude/architecture/ARCHITECTURE.md)

## 아키텍처 규칙
- Domain 인터페이스와 entity만 import. Domain 구현체(UseCase, QaDataManagerImpl, Manager)를 직접 import하지 않음
- 모든 싱글톤 바인딩은 `di/AppModule.kt`에서 처리
- Android Context가 필요한 클래스는 AppModule의 `@Provides`에서 생성하거나 `@Inject constructor`로 주입
