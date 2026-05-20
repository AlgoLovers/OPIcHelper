# 리팩토링 계획서 — OPIc Helper

> 작성일: 2026-05-12
> 목표: 고품질·무버그·유지보수 용이·리소스 관리 우수한 앱 달성
> 원칙: Clean Architecture + SOLID + 구조적 동시성(Structured Concurrency) + 안전한 리소스 관리

## 현재 상태 요약

| 영역 | 평가 | 핵심 문제 |
|------|------|-----------|
| 동시성/경쟁상태 | **위험** | MemorizeTestProgressTracker 비원자 갱신, BaseTtsPlayer CompletableDeferred 이중 완료, MainViewModel runBlocking ANR |
| 리소스 관리 | **위험** | RecordingAudioPlayerImpl MediaPlayer 누수, AudioRecorderImpl 해제 누락, 싱글톤 CoroutineScope 미해지 |
| SRP | **미흡** | MainViewModel 25메서드/22필드/5관심사, MemorizationViewModel 3모드 통합 |
| ISP/DIP | **위반** | Repository에 UI 콜백, QaDataManager Android 직접 의존, Data→Domain 직접 참조 |
| 코드 정리 | **필요** | 미사용 코드 3건, 중복 UseCase 1건, 사실상 미사용 필드 2건 |

## 커밋별 수정 계획

우선순위: **위험도 높음 → 설계 개선 → 코드 정리**

---

### Commit 1: `fix: MemorizeTestProgressTracker 경쟁 상태 수정 — Mutex 도입`

**문제**: `_progressMap`에 대한 read-modify-write가 비원자. `updateProgress`와 `persistChangedProgress`가 동시 실행되면 데이터 손실.

**수정 내용**:
- `MemorizeTestProgressTracker`에 `Mutex()` 추가
- 모든 `_progressMap.value` 읽기-쓰기 시퀀스를 `mutex.withLock { }`로 보호
- 대상 메서드: `updateProgress`, `getProgress`, `clearScriptProgress`, `persistChangedProgress`, `loadPersistedProgress`

**사이드이펙트 고려**:
- `Mutex.withLock`은 suspend 함수이므로, 이미 suspend가 아닌 메서드는 suspend로 변경 필요
- 호출층(MemorizationViewModel, RepeatListeningRepositoryImpl 등)은 이미 코루틴 내에서 호출하므로 영향 없음
- `persistChangedProgress`는 `MainViewModel.cleanupOnAppExit()`에서 `runBlocking`으로 호출 중 → Commit 4에서 함께 해결
- 락 유지 시간이 짧고 I/O가 없어 데드락 위험 없음

**빌드 확인**: `updateProgress`, `getProgress` 등을 suspend fun으로 변경 시 호출측에 `suspend` 컨텍스트 필요. 호출측이 모두 코루틴 내부이므로 문제없음.

---

### Commit 2: `fix: BaseTtsPlayer CompletableDeferred 이중 완료 크래시 방지`

**문제**: 타임아웃 후 `onDone` 콜백이 도착하면 `completionDeferred.complete()`가 두 번 호출되어 `IllegalStateException` 크래시.

**수정 내용**:
- `CompletableDeferred` 대신 `AtomicBoolean` 플래그로 완료 상태 추적
- 또는 `completionDeferred.complete()` 호출을 `try/catch`로 보호
- 더 나은 방법: `CompletableDeferred<Unit>()` 대신 채널 기반 또는 `Job` 기반 대기로 전환
- 권장: `suspendCancellableCoroutine` 패턴으로 재작성. 타임아웃은 `withTimeout`으로 처리, 콜백에서 `resume`은 `tryResume` 사용

**사이드이펙트 고려**:
- `speak()` 메서드 시그니처 변경 없음 (suspend fun 유지)
- `onStart`, `onDone`, `onError` 콜백 구조는 동일하게 유지
- `GoogleTtsPlayer.speakAndGetDuration()`도 동일한 패턴 사용 → 함께 수정 필요
- TTS 엔진 콜백이 Main 스레드에서 오므로, `resume`이 Main에서 실행됨. `withContext(Dispatchers.Main)` 보장 필요

**빌드 확인**: `speak()`가 이미 suspend fun이므로 내부 구현만 변경. 외부 API 변경 없음.

---

### Commit 3: `fix: RecordingAudioPlayerImpl MediaPlayer 누수 및 AudioRecorderImpl 해제 누락 수정`

**문제 1**: `RecordingAudioPlayerImpl.startRecordingPlayback()`에 `OnCompletionListener` 없음 → 정상 완료 시 MediaPlayer 누수.
**문제 2**: `AudioRecorderImpl.stopRecording()`에서 `stop()` 예외 시 `release()` 미호출 → MediaRecorder + 마이크 하드웨어 누수.

**수정 내용 — RecordingAudioPlayerImpl**:
- `startRecordingPlayback()`에 `OnCompletionListener` 추가
- 완료 시 MediaPlayer `release()` + null 처리
- `currentPlayer` 참조를 메서드 로컬이 아닌 필드로 관리 (stop()에서도 접근 가능)

**수정 내용 — AudioRecorderImpl**:
- `stopRecording()`의 try-catch를 세분화: `stop()` 예외 시에도 `release()` 보장
- `finally` 블록에서 `recorder = null` 보장

**사이드이펙트 고려**:
- `OnCompletionListener` 추가 시, 기존에 외부에서 `stopRecordingPlayback()`을 명시적 호출하던 경로와 충돌 가능 → `stopRecordingPlayback()`에서 이미 해제된 player에 대한 null 체크 추가
- `AudioRecorderImpl`의 `finally` 블록 변경은 예외 경로만 영향. 정상 경로는 변함 없음

**빌드 확인**: 공개 API 변경 없음. 내부 구현만 수정.

---

### Commit 4: `fix: MainViewModel.runBlocking 제거 — ANR 방지 및 생명주기 정리`

**문제**: `cleanupOnAppExit()`에서 `runBlocking { progressTracker.persistChangedProgress() }` 호출 → Main 스레드 블로킹 → ANR.

**수정 내용**:
- `cleanupOnAppExit()`를 `suspend fun`으로 변경, 내부 `runBlocking` 제거
- `MainActivity.cleanupAllResources()`에서 `lifecycleScope.launch { viewModel.cleanupOnAppExit() }`로 호출
- `onBackPressed()`에서도 동일하게 코루틴으로 전환
- `MainActivity.onDestroy()`에서도 코루틴으로 전환 (Activity가 소멸 중이므로 `GlobalScope` 또는 `ProcessLifecycleOwner`의 scope 사용 검토)

**추가 수정**:
- `FullMemorizationUseCase.currentRecordingPath`에 `@Volatile` 추가
- `TtsOrchestrator.currentKoreanTtsIndex`를 `AtomicInteger`로 변경
- `FullMemorizationUseCase.playbackJob` 접근에 `@Volatile` 추가

**사이드이펙트 고려**:
- `cleanupOnAppExit()`를 suspend로 변경하면 `MainViewModel.onCleared()`에서도 suspend 컨텍스트 필요 → `viewModelScope.launch`로 래핑
- `onCleared()`는 ViewModel 생명주기에서 한 번만 호출되므로, `viewModelScope`가 이미 취소된 상태 → `CoroutineScope(SupervisorJob()).launch` 사용 후 즉시 cancel
- `onBackPressed()`에서 비동기 저장이 완료되기 전에 Activity가 종료될 수 있음 → `lifecycleScope.launch`는 `onDestroy` 전까지 대기하므로 안전

**빌드 확인**: `cleanupOnAppExit()` 시그니처 변경으로 인해 `MainActivity` 수정 필요. 컴파일 에러로 즉시 감지 가능.

---

### Commit 5: `refactor: 싱글톤 CoroutineScope 생명주기 관리 — Closeable 패턴 도입`

**문제**: `TtsPlaybackController`, `FullMemorizationUseCase`, `PlayMergedFileUseCase`의 `CoroutineScope`가 해지되지 않음.

**수정 내용**:
- 각 싱글톤 클래스에 `Closeable` 구현 추가
- `close()`에서 `coroutineScope.cancel()` 호출
- `OPicHelperApplication`에서 `ProcessLifecycleOwner.get().lifecycle.addObserver()`로 앱 종료 시 `close()` 호출 보장
- 또는 `OPicHelperApplication.onTerminate()` 오버라이드 (디버그 전용이므로 불충분)
- 권장: `Application.ActivityLifecycleCallbacks`로 마지막 Activity 소멸 감지 후 자원 해제

**수정 대상**:
- `TtsPlaybackController`: `coroutineScope.cancel()` + `cleanupTts()` 통합
- `FullMemorizationUseCase`: `scope.cancel()` + `cancelPlayback()` 통합
- `PlayMergedFileUseCase`: `scope.cancel()` + `release()` 통합

**사이드이펙트 고려**:
- `scope.cancel()` 이후 코루틴 시작 시 `CancellationException` → `close()` 이후 메서드 호출 방지 필요
- `QaDataManager.release()`는 이미 `scope.cancel()` 포함 → 참고 모델
- Hilt 싱글톤은 프로세스 종료 시까지 살아있으므로, 실질적 영향은 테스트 환경에서 큼
- 프로덕션에서는 프로세스 종료가 곧 해제이므로 중요도 낮음. 단, 테스트 격리와 구조적 동시성 원칙 준수를 위해 필수

**빌드 확인**: 새 인터페이스 구현이므로 기존 코드와 충돌 없음.

---

### Commit 6: `refactor: ExecuteFullMemorizationUseCase 삭제 및 FullMemorizationUseCase 정리`

**문제**: `ExecuteFullMemorizationUseCase`는 사실상 미사용 코드. `FullMemorizationUseCase`가 실제 사용 중이지만 Clean Architecture 위반 (Repository 우회).

**수정 내용**:
1. `ExecuteFullMemorizationUseCase.kt` 삭제
2. `FullMemorizationUseCase`의 기능을 `FullMemorizationRepository` 인터페이스에 반영:
   - `playRecordingWithHighlight()` 메서드 추가
   - `highlightIndex: StateFlow<Int?>` 노출 필요성 검토
   - `cancelPlayback()` 메서드 추가
3. `FullMemorizationRepositoryImpl`에서 `FullMemorizationUseCase`의 로직을 이관
4. `FullMemorizationUseCase`를 얇은 래퍼로 축소 (다른 UseCase와 동일한 패턴)

**사이드이펙트 고려**:
- `MemorizationViewModel`이 `FullMemorizationUseCase.highlightIndex`를 직접 collect → Repository로 이동 시 동일한 StateFlow 접근 보장 필요
- `FullMemorizationRepositoryImpl`에 이미 `AudioRecorder`, `AudioPlayer`, `AudioFileManager`가 주입되어 있어 로직 이관 가능
- `RecordingTimeManager` 주입이 `FullMemorizationRepositoryImpl`에 추가 필요 → `AppModule` 바인딩 수정
- `CoroutineScope(SupervisorJob() + Dispatchers.IO)` 이관 시 생명주기 관리도 함께 이동

**빌드 확인**: AppModule 바인딩 변경, MemorizationViewModel import 변경 필요. 컴파일 에러로 즉시 감지.

---

### Commit 7: `refactor: QaDataManager Android 의존성 분리 — DIP 위반 해결`

**문제**: `QaDataManager`가 `domain/repository/`에 위치하지만 `Application`, `Context`, `SharedPreferences`를 직접 사용.

**수정 내용**:
1. `QaDataManager`에서 Android 의존성 제거:
   - `SharedPreferences` → `ProgressPersistenceService` 인터페이스로 교체 (이미 존재)
   - `Application` → `QaDataManager`가 `OPicHelperApplication` 캐스팅 제거
   - 현재 `SharedPreferences`로 읽는 `last_category`, `last_index`, `last_memorize_level`을 `ProgressPersistenceService`에 추가
2. `QaDataManager`를 순수 Kotlin 클래스로 전환
3. Android 의존성이 필요한 부분은 Data 계층의 구현체로 이동

**사이드이펙트 고려**:
- `ProgressPersistenceService` 인터페이스에 새 메서드 추가 → `ProgressPersistenceServiceImpl` 구현 필요
- `last_memorize_level`은 현재 `MainViewModel`이 `SharedPreferences`에서 직접 읽음 → `UserPreferencesRepository`로 이동
- `QaDataManager.init()`에서 `Application`을 `OPicHelperApplication`으로 캐스팅 → 이 로직이 무엇을 하는지 확인 후 적절한 인터페이스로 대체
- AppModule의 `provideQaDataManager()`에서 `Application` 제거, 새 의존성 추가

**빌드 확인**: 인터페이스 변경으로 컴파일 에러 발생 → 순차적 수정으로 해결.

---

### Commit 8: `refactor: Repository 인터페이스에서 UI 콜백 제거 — ISP 위반 해결`

**문제**: `RepeatListeningRepository`와 `EnglishWritingTestRepository`가 UI 콜백(`RepeatListeningUiCallback`)을 직접 수신.

**수정 내용**:
1. `RepeatListeningRepository.executeRepeatListening()`에서 `uiCallback` 파라미터 제거
2. `EnglishWritingTestRepository`의 메서드에서 UI 콜백 제거
3. 콜백 대신 StateFlow/Kotlin Flow로 상태 전달:
   - 하이라이트 인덱스 → `SharedFlow<Int>` 또는 `StateFlow<Int?>`
   - 카드 플립 → `SharedFlow<Boolean>`
   - 완료 → `SharedFlow<Unit>`
4. Repository 구현체에서 콜백 대신 Flow emit
5. UseCase/ViewModel에서 Flow collect로 전환

**사이드이펙트 고려**:
- `RepeatListeningRepositoryImpl`이 `MemorizeTestProgressTracker`를 직접 import (Data→Domain 위반) → 이 커밋에서 함께 해결
- `EnglishWritingTestRepositoryImpl`도 동일한 위반 → 함께 해결
- `MemorizationViewModel`이 `RepeatListeningUiCallback`을 구현 → Flow collect로 전환
- 콜백 기반 동기 처리에서 Flow 기반 비동기 처리로 전환 → 타이밍 변경 가능성
- 하이라이트 이벤트는 실시간성이 중요하므로 `SharedFlow(replay=0, extraBufferCapacity=1)` 권장

**빌드 확인**: 인터페이스 시그니처 변경으로 다수 파일 수정. 컴파일 에러로 순차적 해결.

---

### Commit 9: `refactor: MainViewModel 분리 — QaBrowserViewModel 추출`

**문제**: MainViewModel이 5개 관심사(25메서드/22필드)를 통합 관리.

**수정 내용 — 1단계 (QA 데이터 분리)**:
1. `QaBrowserViewModel` 신규 생성:
   - 이관 메서드: `selectCategory`, `nextQaItem`, `previousQaItem`, `clearError`, `getItemsInCategory`, `getCurrentAnswer`, `getCurrentAnswerKo`
   - 이관 상태: `currentQaItem`, `currentCategory`, `isLoading`, `error`, `categories`
   - 이관 의존성: `qaDataManager`, `userPreferencesRepository`
2. `MainViewModel`에서 QA 관련 코드 제거
3. `MainScreen.kt`에서 두 ViewModel 사용
4. `AppState`에서 QA 필드 분리 → `QaBrowserState` data class 생성

**사이드이펙트 고려**:
- `MemorizationViewModel`도 `qaDataManager`를 사용 → `QaBrowserViewModel`과 공유 필요
- `qaDataManager`는 싱글톤이므로 두 ViewModel이 같은 인스턴스 주입 가능
- `MainScreen.kt`에서 `hiltViewModel()` 호출 추가
- 카테고리 변경 시 암기 테스트 상태 리셋 로직이 `MainScreen`에서 조율 필요
- `cleanupOnAppExit()`에서 `qaDataManager.saveCurrentProgress()` 호출 → `QaBrowserViewModel.onCleared()`로 이동

**빌드 확인**: 새 ViewModel 생성, MainScreen import 변경. 컴파일 에러로 순차 해결.

---

### Commit 10: `refactor: MainViewModel 분리 — PlaybackViewModel 추출`

**수정 내용 — 2단계 (TTS 재생 분리)**:
1. `PlaybackViewModel` 신규 생성:
   - 이관 메서드: `playQuestion`, `playAnswer`, `stopCurrentTts`, `stopAllTts`, `cleanupAllTts`, `onBackgroundMove`, `onForegroundReturn`
   - 이관 상태: `isPlaying`, `isQuestionPlaying`, `isAnswerPlaying`, `questionHighlightIndex`, `answerHighlightIndex`, `answerKoHighlightIndex`, `recordingHighlightIndex`
   - 이관 의존성: `ttsPlaybackController`
2. 영작 병합 파일 재생도 `PlaybackViewModel`로 이관:
   - 메서드: `playEnglishWritingTestMergedFile`, `stopEnglishWritingTestMergedFile`, `checkEnglishWritingTestMergedFile`
   - 상태: `hasEnglishWritingTestMergedFile`, `isEnglishWritingTestMergedFilePlaying`, `englishWritingTestMergedFileHighlightIndex`
   - 의존성: `playMergedFileUseCase`
3. `SettingsScreen`이 `MainViewModel` 대신 가벼운 `SettingsViewModel` 사용하도록 변경:
   - `SettingsViewModel` 의존성: `UserPreferencesRepository`만
   - 상태: `currentUserLevel`, `currentKoreanTtsService`

**사이드이펙트 고려**:
- TTS 정지가 여러 ViewModel에서 필요 → `stopAllTts()`는 `PlaybackViewModel`에만 두고, 다른 ViewModel은 참조로 호출
- `MainViewModel.onCleared()`에서 TTS 정리 → `PlaybackViewModel.onCleared()`로 이동
- `MainScreen`이 3개 ViewModel(`QaBrowserViewModel`, `PlaybackViewModel`, `MemorizationViewModel`)을 참조 → 복잡도 증가. 이는 향후 Coordinator/Reducer 패턴 도입으로 완화 가능
- `SettingsViewModel` 신규 생성 시 `AppNavigation` 수정 불필요 (SettingsScreen이 hiltViewModel() 사용)

**빌드 확인**: 대규모 리팩토링. 빌드 후 UI 동작 테스트 필수.

---

### Commit 11: `refactor: MemorizationUiState 정리 — 미사용 필드 제거 및 중복 상태 통합`

**수정 내용**:
1. `MemorizationUiState`에서 미사용 필드 제거:
   - `repeatListeningCurrentRepeat` (항상 0)
   - `repeatListeningTotalRepeats` (항상 5)
2. 중복 상태 통합:
   - `_englishWritingTestCompleted: MutableStateFlow<Boolean>` → `MemorizationUiState.englishWritingTestCompleted`로 통합
   - `_stopEnglishWritingTestMergedFilePlaying: MutableStateFlow<Boolean>` → `MemorizationUiState.stopEnglishWritingTestMergedFilePlaying`로 통합
3. `MainScreen.kt`에서 직접 collect하던 별도 StateFlow를 `uiState` 경유로 변경

**사이드이펙트 고려**:
- `MainScreen.kt`에서 `memorizationViewModel.englishWritingTestCompleted` 직접 참조 → `memorizationViewModel.uiState.englishWritingTestCompleted`로 변경
- `updateUiState()`가 모든 필드를 재계산하므로, 통합 후에도 동일 패턴 유지
- `englishWritingTestCompleted` 플래그 리셋 시점이 MainScreen에서 이루어짐 → 이 로직이 정상 동작하는지 확인

**빌드 확인**: StateFlow 참조 경로 변경. 컴파일 에러로 즉시 감지.

---

### Commit 12: `refactor: 미사용 코드 일괄 삭제`

**수정 내용**:
1. `QaDataLoaderImpl.kt` 삭제 (LeveledQaDataLoader만 사용 중)
2. `PlaybackEvent.kt` 삭제 (사용처 없음)
3. `AppModule.kt`에서 미사용 import 제거 (`QaDataLoaderImpl` import)
4. TtsViewModelTest 고아 테스트 파일 존재 여부 확인 후 삭제
5. `docs/BASIC_OPERATIONS.md` 내용 검증 — 유효하면 유지, 아니면 삭제

**사이드이펙트 고려**:
- `QaDataLoaderImpl`은 AppModule에 바인딩되지 않으므로 삭제 안전
- `PlaybackEvent`은 import하는 파일 없는지 grep으로 사전 확인 필요
- 테스트 파일 삭제는 테스트 스위트 실행에 영향 없음 (이미 비활성)

**빌드 확인**: 삭제 후 빌드 + 테스트 실행으로 누락 참조 확인.

---

### Commit 13: `refactor: 과도한 로깅 정리`

**수정 내용**:
- `TtsOrchestrator`의 `Log.d` 호출 축소: 문장 분할, 언어 감지 등 반복 로그를 `Log.v`(Verbose)로 강하향 또는 제거
- `TtsPlaybackController`의 과다 `Log.d` 축소
- `BaseTtsPlayer`의 타임아웃/콜백 로그는 `Log.w`(경고 수준)로 유지
- 각 UseCase의 `Log.d` 호출 검토 → 진단에 필요한 최소한만 유지
- 권장: `BuildConfig.DEBUG` 가드 추가하여 릴리즈 빌드에서 로그 제거

**사이드이펙트 고려**:
- 로그 제거로 인해 디버깅 시 정보 부족 가능 → 중요 경로(에러, 타임아웃, 폴백)의 로그는 유지
- `BuildConfig.DEBUG` 가드는 ProGuard가 릴리즈에서 자동 제거하므로 성능 영향 없음

**빌드 확인**: 로그 제거는 빌드/런타임 동작에 영향 없음.

---

## 수정 순서 종합

| # | 커밋 타입 | 주요 변경 | 위험도 | 독립성 |
|---|----------|----------|--------|--------|
| 1 | fix | MemorizeTestProgressTracker Mutex | 높음 | 독립 |
| 2 | fix | BaseTtsPlayer CompletableDeferred | 높음 | 독립 |
| 3 | fix | MediaPlayer/MediaRecorder 누수 | 높음 | 독립 |
| 4 | fix | runBlocking 제거 + @Volatile/AtomicInteger | 높음 | 독립 |
| 5 | refactor | 싱글톤 CoroutineScope 관리 | 중간 | 독립 |
| 6 | refactor | 중복 UseCase 삭제/통합 | 중간 | 독립 |
| 7 | refactor | QaDataManager DIP 위반 해결 | 중간 | 8번과 부분 의존 |
| 8 | refactor | Repository ISP 위반 해결 | 중간 | 7번과 부분 의존 |
| 9 | refactor | MainViewModel → QaBrowserViewModel | 중간 | 7번 선행 권장 |
| 10 | refactor | MainViewModel → PlaybackViewModel | 중간 | 9번 선행 권장 |
| 11 | refactor | MemorizationUiState 정리 | 낮음 | 독립 |
| 12 | refactor | 미사용 코드 삭제 | 낮음 | 독립 |
| 13 | refactor | 과도한 로깅 정리 | 낮음 | 독립 |

**1-4번**: 버그 수정. 각각 독립적이며, 어느 것부터 시작해도 무방.
**5-8번**: 아키텍처 위반 해결. 7번과 8번은 약간의 의존성이 있으나 순서 바꿔도 가능.
**9-10번**: ViewModel 분리. 7번(QaDataManager 정리)이 선행되면 더 깔끔하게 진행 가능.
**11-13번**: 코드 정리. 언제든 가능.

---

## 검증 체크리스트 (각 커밋마다 수행)

- [ ] `./gradlew assembleDebug` 빌드 성공
- [ ] `./gradlew testDebugUnitTest` 단위 테스트 통과
- [ ] `./gradlew lint` 정적 분석 경로 없음 (기존 경고 외)
- [ ] 수동 테스트: 앱 실행 → 카테고리 전환 → 3모드 동작 확인
- [ ] 수동 테스트: 앱 백그라운드/포그라운드 전환 시 정상 동작
- [ ] 수동 테스트: 앱 종료 후 재시작 시 진행상황 복원 확인
- [ ] Domain 계층이 Data 계층을 직접 import하지 않는지 확인
- [ ] Data 계층이 Domain 구현체를 직접 import하지 않는지 확인
- [ ] 새 CoroutineScope가 적절히 해지되는지 확인
- [ ] StateFlow 업데이트가 스레드 안전한지 확인

---

## 장기 개선 방향 (현재 계획 범위 외)

1. **MemorizationViewModel 모드 분리**: 3모드를 각각 독립 ViewModel/UseCase로 분리. CurrentMode enum 12개 값을 각 모드 내부 상태로 은닉
2. **단위 테스트 구축**: 핵심 로직(TTS 재생 흐름, UseCase, ViewModel 상태 전이)에 대한 테스트 작성
3. **Coordinator 패턴 도입**: 다수 ViewModel 간 조율을 MainScreen Composable에서 분리
4. **ProgressPersistenceService 확장**: QaDataManager의 SharedPreferences 의존성 완전 흡수
