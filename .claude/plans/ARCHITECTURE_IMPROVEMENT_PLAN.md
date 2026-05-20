# 아키텍처 개선 계획서

> 코드에서 발견된 구조적 결함과 개선 방향을 정리합니다.
> 각 항목은 우선순위와 난이도를 표시합니다.

## 읽는 방법

- **심각도**: 🔴 높음 (버그/크래시 위험), 🟡 중간 (확장성/유지보수 저하), 🟢 낮음 (정리/개선)
- **난이도**: ⭐ 쉬움 (1파일), ⭐⭐ 보통 (2-3파일), ⭐⭐⭐ 어려움 (다수 파일 + 구조 변경)

---

## 1. 🔴 스레드 안전성 문제

### 1.1 RecordingFileRepositoryImpl — 동기화 없는 mutable 상태

**파일**: `data/repository/RecordingFileRepositoryImpl.kt` (lines 22-23)

```kotlin
private var currentRecordingPath: String? = null  // ← 여러 코루틴에서 접근
private var currentPlayingPath: String? = null    // ← 보호 없음
```

**문제**: `@Singleton` 클래스인데 mutable 상태가 보호되지 않음. `hasRecordingFile()`에서 쓰고 `playRecordingFile()`에서 읽는 경쟁 가능.

**해결 방법**: `AtomicReference<String?>` 사용 또는 `Mutex`로 보호.

**난이도**: ⭐

---

### 1.2 BaseTtsPlayer — 동시 speak() 호출 시 리스너 덮어쓰기

**파일**: `data/audio/BaseTtsPlayer.kt` (lines 77-100)

**문제**: `speak()`가 `UtteranceProgressListener`를 설정하는데, 동시에 두 코루틴이 `speak()`를 호출하면 첫 번째 리스너가 덮어씌워져 완료 콜백이 영영 오지 않음 (30초 타임아웃까지 대기).

**해결 방법**: 현재 TtsPlaybackController의 Job 취소로 보호되고 있으나, 근본적으로는 `speak()` 자체에 `Mutex` 또는 큐잉 메커니즘 필요.

**난이도**: ⭐⭐⭐

---

### 1.3 AudioPlayerImpl / AudioRecorderImpl — 동기화 없는 mutable 상태

**파일**: `data/audio/AudioPlayerImpl.kt` (line 9), `AudioRecorderImpl.kt` (lines 16-17)

**문제**: `player: MediaPlayer?`와 `recorder: MediaRecorder?`가 `@Volatile`이나 동기화 없이 접근됨.

**해결 방법**: `@Volatile` 추가 또는 `AtomicReference` 사용.

**난이도**: ⭐

---

## 2. 🟡 아키텍처 위반

### 2.1 Dual DI 등록 (7개 클래스)

**파일**: `di/AppModule.kt` + 각 구현체

**문제**: 다음 7개 클래스가 `@Inject constructor`와 `@Provides` 양쪽에 등록됨:
- QaDataManager, ProgressPersistenceServiceImpl, RecordingTimeManagerImpl
- RecordingFileRepositoryImpl, RepeatListeningRepositoryImpl
- EnglishWritingTestRepositoryImpl, WakeLockManager

Hilt는 `@Provides`를 우선 사용하므로 `@Inject constructor`의 `@Singleton`은 의미 없음. 혼란 유발.

**해결 방법**: `@Inject constructor`에서 `@Singleton` 제거. `@Provides`에서만 수명 관리. 또는 반대로 `@Provides`를 제거하고 `@Inject constructor`만 사용 (권장).

**난이도**: ⭐⭐

---

### 2.2 EnglishWritingTestRepositoryImpl — QaDataManager 직접 참조

**파일**: `data/repository/EnglishWritingTestRepositoryImpl.kt` (line 9)

**문제**: Data 계층이 Domain의 구현체(QaDataManager)를 직접 import. Clean Architecture 위반.

**해결 방법**: QaDataManager를 인터페이스 + 구현체로 분리하거나, EnglishWritingTestRepoImpl에서 필요한 데이터를 UseCase에서 전달받도록 변경.

**난이도**: ⭐⭐⭐

---

### 2.3 QaDataManager가 MemorizeTestProgressTracker(UseCase) 참조

**파일**: `domain/repository/QaDataManager.kt` (line 5)

**문제**: Repository 계층이 UseCase 계층을 참조 (역의존성).

**해결 방법**: `saveCurrentProgress()`에서 ProgressPersistenceService만 사용하고, MemorizeTestProgressTracker 의존성 제거.

**난이도**: ⭐⭐

---

### 2.4 TtsPlaybackController — TtsOrchestrator를 setter로 주입

**파일**: `domain/audio/TtsPlaybackController.kt` (line 41-46)

**문제**: `@Inject constructor()`에 매개변수가 없고, PlaybackViewModel.init에서 `setTtsOrchestrator()`로 설정. DI 보장이 깨짐 (null 가능).

**해결 방법**: `@Inject constructor(private val ttsOrchestrator: TtsOrchestrator)`로 생성자 주입.

**난이도**: ⭐

---

## 3. 🟡 MemorizationViewModel 분리

### 3.1 SRP 위반 — 3개 모드 통합

**파일**: `presentation/viewmodel/MemorizationViewModel.kt` (537행)

**문제**: 반복듣기, 영작테스트, 통암기를 하나의 ViewModel이 모두 관리. 모드 추가 시 계속 비대해짐.

**해결 방법**:
```
MemorizationViewModel (공통 상태)
├── RepeatListeningViewModel (반복듣기 전용)
├── EnglishWritingTestViewModel (영작테스트 전용)
└── FullMemorizationViewModel (통암기 전용)
```

또는 전략 패턴으로 모드별 로직을 분리:

```kotlin
sealed class MemorizationStrategy {
    abstract fun start()
    abstract fun stop()
    abstract fun handleEvent(event: MemorizeTestEvent)
}

class RepeatListeningStrategy(...) : MemorizationStrategy()
class EnglishWritingTestStrategy(...) : MemorizationStrategy()
class FullMemorizationStrategy(...) : MemorizationStrategy()
```

**난이도**: ⭐⭐⭐

---

### 3.2 분산된 StateFlow — 6개 개별 MutableStateFlow

**문제**: `_uiState` 외에 `_isQuestionCardFlipped`, `_englishWritingTestCompleted`, `_stopEnglishWritingTestMergedFilePlaying`, `_isRunning`, `_currentMode`, `memorizeLevels`가 각각 별도의 StateFlow.

**해결 방법**: 모든 상태를 `MemorizationUiState`로 통합. MainScreen이 단일 `uiState`만 collect.

**난이도**: ⭐⭐

---

## 4. 🟡 MainScreen 상태 관리 개선

### 4.1 11개 StateFlow 개별 collect

**파일**: `presentation/ui/screen/MainScreen.kt` (lines 35-57)

**문제**: MainScreen이 11개의 StateFlow를 개별로 collect. "UI는 ViewModel의 StateFlow만 구독" 규칙 위반.

**해결 방법**:
1. 각 ViewModel이 단일 통합 `uiState` 제공
2. MainScreen은 3개의 `uiState`만 collect
3. 파생 값은 `remember` + `derivedStateOf`로 계산

**난이도**: ⭐⭐⭐ (3.1, 3.2와 함께 진행 필요)

---

### 4.2 MemorizeLevelPlaybackButton — ViewModel 직접 전달

**파일**: `presentation/ui/screen/MainScreenComponentsUI/MemorizeLevelPlaybackButton.kt`

**문제**: Composable이 ViewModel 인스턴스를 직접 수신. 메서드를 직접 호출.

**해결 방법**: 데이터와 콜백만 전달:
```kotlin
@Composable
fun MemorizeLevelPlaybackButton(
    selectedLevel: String,
    onPlayEnglishWritingMergedFile: () -> Unit,
    onStopEnglishWritingMergedFile: () -> Unit,
    onPlayFullMemorizationRecording: () -> Unit,
    onStopFullMemorizationRecording: () -> Unit,
    // ... 상태 값들
)
```

**난이도**: ⭐

---

## 5. 🟢 코드 정리

### 5.1 미사용 컴포넌트 삭제

**파일**: `RecordingButton.kt`, `RecordingSection.kt`, `QuestionAnswerSection.kt`

**문제**: MainScreen에서 사용하지 않는 레거시 컴포넌트.

**해결 방법**: 삭제.

**난이도**: ⭐

---

### 5.2 미사용 파라미터 정리

**파일**: `QuestionPlayButton.kt`, `AnswerPlayButton.kt` (currentQuestion/currentAnswer), `RecordingAnimation.kt` (isRecording/onStopRecording)

**문제**: `@Suppress("UNUSED_PARAMETER")`로 경고를 숨기고 있음. 실제로 사용하지 않는 파라미터.

**해결 방법**: 파라미터 제거 + 호출부 업데이트. 또는 향후 사용 계획이 있으면 유지.

**난이도**: ⭐

---

### 5.3 Log.d 잔존 제거

**파일**: `RecordingFileRepositoryImpl.kt`, `RecordingTimeManagerImpl.kt`, `GoogleTtsPlayer.kt`, `SamsungTtsPlayer.kt`

**문제**: 코딩 컨벤션에서 "Log.d 제거"를 규정하지만 아직 잔존.

**해결 방법**: Log.d → Log.w 또는 삭제.

**난이도**: ⭐

---

### 5.4 PlaybackState 이중 노출 정리

**파일**: `presentation/viewmodel/PlaybackViewModel.kt` (lines 45-47 + 77-89)

**문제**: `hasEnglishWritingTestMergedFile`, `isEnglishWritingTestMergedFilePlaying`, `englishWritingTestMergedFileHighlightIndex`가 개별 StateFlow와 PlaybackState 내부에 모두 존재.

**해결 방법**: PlaybackState로 통합. MainScreen에서 개별 StateFlow 참조를 `playbackState.*`로 변경.

**난이도**: ⭐⭐

---

### 5.5 stopTts() / stopAllTts() 중복

**파일**: `domain/audio/TtsPlaybackController.kt` (lines 136-138, 182-184)

**문제**: 두 메서드가 완전히 동일한 동작 (`stopTtsSync()` 호출).

**해결 방법**: `stopAllTts()`를 `stopTts()`의 alias로 만들거나, 사용처를 `stopTts()`로 통일 후 삭제.

**난이도**: ⭐

---

### 5.6 RecordingFileRepositoryImpl — delay() 대신 OnCompletionListener

**파일**: `data/repository/RecordingFileRepositoryImpl.kt` (lines 101-103)

**문제**: `getDuration()` 후 `delay()`로 재생 완료를 추정. 실제 재생 시간과 다를 수 있음.

**해결 방법**: `RecordingAudioPlayerImpl`의 `OnCompletionListener`를 활용한 콜백 기반 완료 감지.

**난이도**: ⭐⭐

---

### 5.7 AudioFileManagerImpl.mergeAudioFiles() — 폴백 없음 + 무한 대기

**파일**: `data/repository/AudioFileManagerImpl.kt` (lines 298-321)

**문제**: `mergeAudioFiles()`는 MediaCodec 실패 시 예외 전파 (폴백 없음). `waitForFileReady()`에 타임아웃 없음 (무한 루프).

**해결 방법**: `mergeAndSaveAudioFiles()`와 동일한 3단계 폴백 추가. `waitForFileReady()`에 타임아웃 추가.

**난이도**: ⭐⭐

---

### 5.8 ScriptProgress 위치 이동

**파일**: `domain/repository/ScriptProgress.kt`

**문제**: 데이터 클래스가 repository 패키지에 있음. entity 패키지가 적절.

**해결 방법**: `domain/entity/ScriptProgress.kt`로 이동.

**난이도**: ⭐

---

### 5.9 ProgressData 이름 충돌 정리

**파일**: `domain/entity/ProgressData.kt` vs `domain/repository/EnglishWritingTestRepository.kt` 내부

**문제**: `ProgressData`라는 이름이 두 곳에 존재. `ProgressData.kt`에는 `AppExitState`와 `CategoryProgress`가, `EnglishWritingTestRepository.kt`에는 다른 필드의 `ProgressData`가 정의됨.

**해결 방법**: Repository의 `ProgressData`를 `TestProgressData`로 이름 변경.

**난이도**: ⭐

---

## 개선 우선순위 추천

| 순서 | 항목 | 이유 |
|------|------|------|
| 1 | 1.1, 1.3 스레드 안전성 | 런타임 버그 위험 |
| 2 | 5.1 미사용 컴포넌트 삭제 | 코드 혼란 즉시 해소 |
| 3 | 5.3 Log.d 제거 | 코딩 컨벤션 준수 |
| 4 | 2.4 TtsOrchestrator 생성자 주입 | 간단한 수정으로 DI 보장 |
| 5 | 2.1 Dual DI 정리 | 혼란 방지 |
| 6 | 5.5 stopTts/stopAllTts 통일 | API 단순화 |
| 7 | 5.8 ScriptProgress 이동 | 패키지 구조 정리 |
| 8 | 2.3 QaDataManager 역의존성 제거 | 아키텍처 개선 |
| 9 | 5.4 PlaybackState 이중 노출 정리 | 상태 관리 일관성 |
| 10 | 4.2 MemorizeLevelPlaybackButton 콜백화 | Compose 규칙 준수 |
| 11 | 5.6 delay → OnCompletionListener | 재생 완료 정확도 |
| 12 | 5.7 mergeAudioFiles 폴백 + 타임아웃 | 안정성 |
| 13 | 3.2 StateFlow 통합 | 상태 관리 일관성 |
| 14 | 3.1 MemorizationViewModel 분리 | 가장 큰 구조적 개선 |
| 15 | 4.1 MainScreen 다중 collect 해결 | 3.1, 3.2 선행 필요 |
