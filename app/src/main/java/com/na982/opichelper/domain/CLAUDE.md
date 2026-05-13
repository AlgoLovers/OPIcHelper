# Domain Layer — 엔티티, 유스케이스, 인터페이스

비즈니스 로직의 핵심. Data 계층을 직접 참조하지 않고 인터페이스로 의존성 역전.

## 패키지 구조

```
domain/
  audio/           — 오디오 인터페이스 & 컨트롤러
  entity/          — 데이터 모델 & Enum
  manager/         — 시스템 매니저
  repository/      — 리포지토리 인터페이스 (+ QaDataManager 구현체)
  usecase/         — 비즈니스 유스케이스
```

## entity/ — 데이터 모델

| 파일 | 내용 |
|------|------|
| `QaItem.kt` | 핵심 QA 데이터. 필드: `id`, `category`, `questionEn`, `questionKo`, `answers: Map<UserLevel, LeveledAnswer>`. **LeveledAnswer** (동일 파일): `answerEn`, `answerKo`, `vocabulary`, `grammar`, `tips` |
| `UserLevel.kt` | AL, IH, IH_RAW, IM — 각각 displayName과 description 포함 |
| `MemorizeLevel.kt` | REPEAT_LISTENING, ENGLISH_WRITING, FULL_MEMORIZATION — allDisplayNames 제공 |
| `RepeatListeningData.kt` | 반복듣기용 데이터 (category, scriptIndex, koreanAnswer, englishAnswer) |
| `ProgressData.kt` | AppExitState, CategoryProgress (진행상황 영속화용). CategoryProgress에 getKey() 메서드 포함 |

**주의**: `ProgressData` data class는 이 파일이 아닌 `EnglishWritingTestRepository.kt`에 별도 정의됨. 파일명과 클래스명 혼동 주의.

## audio/ — 오디오 추상화 & 제어

| 파일 | 역할 | 비고 |
|------|------|------|
| `TtsPlayer.kt` | TTS 엔진 인터페이스 | speak, stop, pause, resume, isPlaying, isAvailable, getServiceName, speakWithHighlight, speakAndGetDuration, release |
| `AudioPlayer.kt` | 오디오 파일 재생 인터페이스 | Data 계층에서 구현 |
| `AudioRecorder.kt` | 녹음 인터페이스 | Data 계층에서 구현 |
| `RecordingAudioPlayer.kt` | 녹음 재생 전용 인터페이스 | Data 계층에서 구현 |
| `PlaybackEvent.kt` | 재생 이벤트 sealed class | **현재 미사용** |
| `RepeatListeningUiCallback.kt` | 반복듣기 UI 콜백 인터페이스 | MemorizationViewModel에서 구현 |
| `TtsOrchestrator.kt` | **@Singleton** 언어 감지→TTS 라우팅 | 한국어: Samsung, 영어: Google. speakWithHighlight로 문장별 하이라이트 지원 (15초 타임아웃). 한글 TTS 폴백: 모든 서비스 실패 시 index 0 리셋 |
| `TtsPlaybackController.kt` | **@Singleton** 재생 상태 관리 | playQuestion/playAnswer/playMergedAudio. 7개 StateFlow 제공 (isPlaying, isQuestionPlaying, isAnswerPlaying + 4개 highlightIndex) |

### TtsPlaybackController 핵심 API

```
playQuestion()       → stopCurrentAndPrepare() + 코루틴에서 speakWithHighlight
playAnswer()         → 동일 패턴
playMergedAudio()    → 질문 TTS → 500ms 딜레이 → 답변 TTS 순차 재생
stopCurrentAndPrepare() → 기존 Job 취소 + 상태 리셋 (TTS stop 호출 안함)
stopTts() / stopAllTts() → TTS stop + Job 취소 + 상태 리셋 + 하이라이트 제거
forceStopTts()       → TTS stop + Job 취소 + 상태 리셋 (하이라이트 유지)
```

**주의**: playQuestion/playAnswer/playMergedAudio는 `stopCurrentAndPrepare()`를 사용. `forceStopTts()`를 사용하면 TTS 엔진에 불필요한 stop이 추가로 전송되어 간헐적 재생 불가 버그 발생 가능.

### TtsOrchestrator 핵심 API

```
speak(text, onComplete)           — 기본 TTS 재생 (언어 자동 감지)
speakWithHighlight(text, onHighlight) — 문장별 하이라이트 콜백 (15초/문장 타임아웃)
speakAndWaitForCompletion(text, ...)  — 완료 대기 (30초 타임아웃)
stop() / pause() / resume()       — 재생 제어
```

한글 감지: 유니코드 범위 0xAC00~0xD7AF, 0x3131~0x318E
문장 분할: `Regex("(?<=[.!?])\\s+")`

## manager/

| 파일 | 역할 |
|------|------|
| `WakeLockManager.kt` | 화면 꺼짐 방지. 30분 안전 타임아웃. `@Suppress("DEPRECATION")` 처리됨 |

## repository/ — 인터페이스 정의

| 파일 | 타입 | 주요 메서드 |
|------|------|------------|
| `QaDataLoader.kt` | 인터페이스 | `loadQaItemsForLevel(level)` |
| `QaDataManager.kt` | **@Singleton 클래스** | 카테고리/인덱스 관리, 데이터 로딩, 진행상황 저장. getCurrentAnswer(), getCurrentAnswerKo(), saveCurrentProgress() 포함 |
| `UserPreferencesRepository.kt` | 인터페이스 | 레벨/TTS속도 get/set, StateFlow |
| `ProgressPersistenceService.kt` | 인터페이스 | AppExitState, CategoryProgress CRUD |
| `RecordingFileRepository.kt` | 인터페이스 | 녹음 파일 CRUD, 재생, 하이라이트 재생 |
| `RecordingTimeManager.kt` | 인터페이스 | saveRecordingTime, getRecordingTime, getAllRecordingTimes, hasRecordingTimes, clearRecordingTimes |
| `RepeatListeningRepository.kt` | 인터페이스 | `executeRepeatListening(data, uiCallback, repeatCount)` — **UI 콜백 결합 위반** |
| `EnglishWritingTestRepository.kt` | 인터페이스 + ProgressData | 영작테스트 실행, 진행상황 CRUD — **UI 콜백 결합 위반** |
| `FullMemorizationRepository.kt` | 인터페이스 | playQuestionWithHighlight, startRecording, stopRecording, playRecording, hasRecording, getRecordingPath, clearRecording |
| `AudioFileManager.kt` | 인터페이스 | 오디오 병합/저장/삭제 (다수 메서드) |
| `ScriptProgress.kt` | 데이터 클래스 | 진행상황 상태. 필드: category, scriptIndex, memorizeLevel, currentSentenceIndex, totalSentences, isMemorizeTestRunning, timestamp, needsSave. getKey(), toPersistable() 포함 |

### 주의: QaDataManager는 인터페이스가 아닌 구현체
`domain/repository/`에 위치하지만 인터페이스가 아님. Android 의존성은 ProgressPersistenceService로 분리 완료.
순수 Kotlin 클래스이나 여전히 `domain/repository/`에 위치 (향후 Data 계층 이동 검토).

## usecase/ — 비즈니스 로직

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `ExecuteRepeatListeningUseCase.kt` | RepeatListeningRepository 위임 래퍼 | 얇은 래퍼 |
| `ExecuteEnglishWritingTestUseCase.kt` | EnglishWritingTestRepository 위임 래퍼 | 얇은 래퍼 |
| `FullMemorizationUseCase.kt` | TtsOrchestrator + RecordingFileRepository + AudioRecorder + QaDataManager + RecordingTimeManager 직접 사용 | highlightIndex StateFlow 제공, CoroutineScope(SupervisorJob + IO) 사용. Closeable 구현. 메서드: startFullMemorization, stopRecording, playRecordingWithHighlight, playRecordingSimple, hasRecording, clearRecording, cancelPlayback |
| `RepeatListeningUseCase.kt` | 독립 반복듣기 로직. 문장별 한국어→영어 N회 | TTS duration 저장, 적응형 딜레이 (단어수*500ms*가중치). RepeatListeningRepositoryImpl과 거의 동일한 로직 포함 |
| `PlayMergedFileUseCase.kt` | 영작테스트 병합 파일 재생 + 하이라이트 | StateFlow: isPlaying, highlightIndex, hasFile. 타이밍 추정: `(문자수 * 50ms).coerceAtLeast(1000ms)`. 정확한 타이밍 모드: playWithExactHighlight(). checkFile() 3회 재시도 |
| `MemorizeTestProgressTracker.kt` | **@Singleton** 진행상황 메모리 관리 + 영속화 | 변경된 항목만 저장 (needsSave 플래그) |

### FullMemorizationUseCase
이전에 `ExecuteFullMemorizationUseCase`가 존재했으나 삭제됨. 현재 `FullMemorizationUseCase`만 사용 중.

## 아키텍처 규칙
- Data 계층을 직접 import하지 않음 (인터페이스로만 의존)
- Entity는 data class 위주. getKey()/toPersistable() 등 경미한 로직만 허용
- UseCase는 단일 책임, Repository 인터페이스만 참조

### 알려진 규칙 위반
Domain 계층에 Android 프레임워크 import가 존재함:
- `TtsOrchestrator` → Context, Log
- `TtsPlaybackController` → Log
- `QaDataManager` → Log (Android 의존성은 ProgressPersistenceService로 분리 완료)
- `WakeLockManager` → Context, PowerManager, Log
→ Log는 허용 범위, Context/PowerManager는 Data 계층으로 이동 검토.
