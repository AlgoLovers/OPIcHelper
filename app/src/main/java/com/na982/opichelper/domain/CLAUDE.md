# Domain Layer — 엔티티, 유스케이스, 인터페이스

비즈니스 로직의 핵심. Data 계층을 직접 참조하지 않고 인터페이스로 의존성 역전.

## 패키지 구조

패키지 구조: [ARCHITECTURE_DOMAIN.md 섹션 2](.claude/architecture/ARCHITECTURE_DOMAIN.md)

## entity/ — 데이터 모델

QaItem.kt, UserLevel.kt, MemorizeLevel.kt, RepeatListeningData.kt, ScriptProgress.kt, CurrentMode.kt, StudyDailyRecord.kt, StudyStatistics.kt

ER 다이어그램: [ARCHITECTURE_DOMAIN.md 섹션 6](.claude/architecture/ARCHITECTURE_DOMAIN.md)

## audio/ — 오디오 추상화 & 제어

TtsPlayer.kt, TtsSpeakResult.kt, HighlightStateHolder.kt, AudioPlayer.kt, AudioRecorder.kt, RecordingAudioPlayer.kt, MemorizeTestEvent.kt, TtsOrchestrator.kt, TtsPlaybackController.kt, TtsHighlightController.kt, TtsPauseController.kt, PipState.kt, PipStateAggregator.kt, PlaybackActionListener.kt, RepeatListeningProgress.kt, SentenceSplitter.kt

클래스 다이어그램: [ARCHITECTURE_DOMAIN.md 섹션 3](.claude/architecture/ARCHITECTURE_DOMAIN.md)

TtsPlaybackController API: [ARCHITECTURE_DOMAIN.md 섹션 3](.claude/architecture/ARCHITECTURE_DOMAIN.md)

TtsOrchestrator API: [ARCHITECTURE_DOMAIN.md 섹션 3](.claude/architecture/ARCHITECTURE_DOMAIN.md)

## manager/

AppLogger (로깅), WakeLockController (화면 꺼짐 방지)

## repository/ — 인터페이스 정의

### 핵심 데이터
- `QaDataLoader` — loadQaItemsForLevel
- `QaDataManager` — 복합 인터페이스 (QaContentReader + QaNavigator + QaSearch + QaDataLifecycle)
- `QaContentReader` — getCurrentQaItem, getCurrentIndex, getCurrentCategory, getCategories
- `QaNavigator` — selectCategory, next/previous/navigateToIndex
- `QaSearch` — searchItems
- `QaDataLifecycle` — init, reload, reset, release
- `DataSeeder` — seedIfNeeded

### Preferences
- `UserPreferencesRepository` — 복합 인터페이스 (6개 하위 인터페이스 상속)
- `UserLevelPreferences` — getUserLevel/setUserLevel, userLevel StateFlow
- `TtsPreferences` — getEnglishTtsRate/setEnglishTtsRate, englishTtsRate StateFlow
- `PlaybackPreferences` — repeatListeningCount, answerPlayCount, isAutoAdvance
- `OnboardingPreferences` — isOnboardingCompleted, isPipGuideCompleted
- `MemorizeLevelPreferences` — getMemorizeLevel/setMemorizeLevel
- `AppDataPreferences` — getSeedVersion/setSeedVersion

### 진행상황 & 녹음
- `ProgressPersistenceService` — NavigationState 저장/로드, CategoryProgress CRUD
- `RecordingFileRepository` — 녹음 파일 CRUD, 재생
- `RecordingTimeManager` — saveRecordingTime, getRecordingTime, getAllRecordingTimes
- `RepeatListeningRepository` — events SharedFlow, executeRepeatListening, repeatProgress StateFlow
- `EnglishWritingTestRepository` — events SharedFlow, executeEnglishWritingTest
- `AudioFileManager` — getRecordingFilePath, saveRecordingFile, mergeAudioFiles, getEnglishWritingTestMergedFile
- `ScriptEditRepository` — updateQaItem, restoreOriginal

### 학습 통계
- `StudySessionRepository` — 복합 인터페이스 (StudySessionRecorder + StudySessionStatisticsReader)
- `StudySessionRecorder` — recordSession
- `StudySessionStatisticsReader` — getDailyRecords, getStreak, getLongestStreak, getTotalStudyDurationMs

### 서비스
- `TtsServiceController` — startForegroundService, stopForegroundService, updateNotificationSentence

## usecase/ — 비즈니스 로직

| 파일 | 역할 | 의존성 | 주의사항 |
|------|------|--------|----------|
| `FullMemorizationUseCase.kt` | 통암기 모드 실행 | RecordingFileRepository, TtsOrchestrator, AudioRecorder, QaContentReader, RecordingTimeManager, AppLogger | @Singleton. highlightIndex + FullMemorizationState StateFlow. CoroutineScope(SupervisorJob + IO). Closeable 구현. 메서드: startFullMemorization, stopRecording, playRecordingWithHighlight, hasRecording, cancelPlayback, reset, close. FullMemorizationState: Idle, QuestionPlaying, Recording, Playing, WithFile |
| `PlayMergedFileUseCase.kt` | 영작테스트 병합 파일 재생 + 하이라이트 | AudioPlayer, AudioFileManager, QaContentReader, RecordingTimeManager | @Singleton. StateFlow: isPlaying, highlightIndex, hasFile. 타이밍 추정: `(문자수 * 50ms).coerceAtLeast(1000ms)`. 정확한 타이밍: playWithExactHighlight(). checkFile() 3회 재시도. Closeable 구현. 메서드: play, stop, checkFile, reset, close |
| `MemorizeTestProgressTracker.kt` | 진행상황 메모리 관리 + 영속화 | ProgressPersistenceService, AppLogger | @Singleton. progressMap: StateFlow<Map<String, ScriptProgress>>. 메서드: restoreAllProgress, getScriptProgress, updateProgress, persistChangedProgress. 변경된 항목만 저장 (needsSave 플래그) |
| `MemorizationModeCoordinator.kt` | 3개 모드 상호 배제 코디네이터 | 없음 (인터페이스) | currentMode, isRunning (StateFlow). requestMode, updateMode, releaseMode. registerJob, registerEventJob, cancelEventJob. events: SharedFlow<CoordinatorEvent>. CoordinatorEvent: EnglishWritingCompleted, EnglishWritingStopped, RecordingStateChanged |
| `ProgressCleanupUseCase.kt` | 앱 종료 시 진행상황 정리 | QaContentReader, MemorizeLevelPreferences, MemorizeTestProgressTracker, AppLogger | cleanupOnExit (NonCancellable), restoreProgress |
| `RecordStudySessionUseCase.kt` | 학습 세션 기록 | StudySessionRecorder | @Singleton. startSession, endSession. @Volatile sessionStartTimeMs |
| `StudyStatisticsCalculator.kt` | 학습 통계 계산 | StudySessionStatisticsReader, MemorizeTestProgressTracker, QaContentReader | @Singleton. calculate() → StudyStatistics |
| `ValidateScriptEditUseCase.kt` | 스크립트 편집 유효성 검사 | 없음 | 순수 도메인 로직 |

## 아키텍처 규칙
- Data 계층을 직접 import하지 않음 (인터페이스로만 의존)
- Entity는 data class 위주. getKey()/toPersistable()/isCompleted() 등 경미한 로직만 허용
- UseCase는 단일 책임, Repository 인터페이스만 참조

### 알려진 규칙 위반
현재 Domain 계층에 Android 프레임워크 import 없음. 모든 로깅은 AppLogger 인터페이스 사용.
