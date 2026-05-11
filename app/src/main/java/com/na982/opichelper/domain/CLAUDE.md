# Domain Layer — 엔티티, 유스케이스, 리포지토리 인터페이스

## 역할
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

## audio/ — 오디오 추상화 & 제어

| 파일 | 역할 | 비고 |
|------|------|------|
| `TtsPlayer.kt` | TTS 엔진 인터페이스 (speak/stop/pause/resume/highlight) | Data 계층에서 구현 |
| `AudioPlayer.kt` | 오디오 파일 재생 인터페이스 | Data 계층에서 구현 |
| `AudioRecorder.kt` | 녹음 인터페이스 | Data 계층에서 구현 |
| `RecordingAudioPlayer.kt` | 녹음 재생 전용 인터페이스 | Data 계층에서 구현 |
| `PlaybackEvent.kt` | 재생 이벤트 sealed class | **현재 미사용** (향후 이벤트 드리븐 아키텍처용) |
| `RepeatListeningUiCallback.kt` | 반복듣기 UI 콜백 인터페이스 | MemorizationViewModel에서 구현 |
| `TtsOrchestrator.kt` | **@Singleton** 언어 감지→TTS 라우팅 | 한국어: Samsung, 영어: Google. speakWithHighlight로 문장별 하이라이트 지원 |
| `TtsPlaybackController.kt` | **@Singleton** 재생 상태 관리 | playQuestion/playAnswer/playMergedAudio. 4개 highlightIndex StateFlow 제공 |

### TtsOrchestrator 핵심 API
```
speak(text, isKorean, rate, callback)     — 기본 TTS 재생
speakWithHighlight(text, onHighlight)     — 문장별 하이라이트 콜백
speakAndWaitForCompletion(text, ...)      — 완료 대기
pause() / resume() / stop()              — 재생 제어
```

### TtsPlaybackController 상태 흐름
```
playQuestion()    → isQuestionPlaying=true, questionHighlightIndex 업데이트
playAnswer()      → isAnswerPlaying=true, answerHighlightIndex, answerKoHighlightIndex 업데이트
playMergedAudio() → isPlaying=true, 녹음 하이라이트 재생
stopTtsSync()     → 모든 상태 리셋, 하이라이트 null
```

4개 하이라이트 StateFlow: `questionHighlightIndex`, `answerHighlightIndex`, `answerKoHighlightIndex`, `recordingHighlightIndex`

## entity/ — 데이터 모델

| 파일 | 내용 |
|------|------|
| `QaItem.kt` | 핵심 QA 데이터. `answers: Map<UserLevel, LeveledAnswer>`. **LeveledAnswer** 도 동일 파일에 정의 |
| `UserLevel.kt` | AL, IH, IH_RAW, IM |
| `MemorizeLevel.kt` | REPEAT_LISTENING, ENGLISH_WRITING, FULL_MEMORIZATION |
| `RepeatListeningData.kt` | 반복듣기용 데이터 (category, scriptIndex, koreanAnswer, englishAnswer) |
| `ProgressData.kt` | AppExitState, CategoryProgress (진행상황 영속화용). **주의: EnglishWritingTestRepository에同名 ProgressData 클래스 존재** |

## manager/

| 파일 | 역할 |
|------|------|
| `WakeLockManager.kt` | 화면 꺼짐 방지. 30분 안전 타임아웃. @Suppress(WAKE_LOCK) 처리됨 |

## repository/ — 인터페이스 정의

| 파일 | 타입 | 주요 메서드 |
|------|------|------------|
| `QaDataLoader.kt` | 인터페이스 | `loadQaItemsForLevel(level)` |
| `QaDataManager.kt` | **@Singleton 클래스** | 카테고리/인덱스 관리, 데이터 로딩, 진행상황 저장 |
| `UserPreferencesRepository.kt` | 인터페이스 | 레벨/TTS속도 get/set, StateFlow |
| `ProgressPersistenceService.kt` | 인터페이스 | AppExitState, CategoryProgress CRUD |
| `RecordingFileRepository.kt` | 인터페이스 | 녹음 파일 CRUD, 재생, 하이라이트 재생 |
| `RecordingTimeManager.kt` | 인터페이스 | 문장별 녹음 시간 관리 |
| `RepeatListeningRepository.kt` | 인터페이스 | `executeRepeatListening(data, uiCallback, repeatCount)` |
| `EnglishWritingTestRepository.kt` | 인터페이스 + ProgressData | 영작테스트 실행, 진행상황 CRUD |
| `FullMemorizationRepository.kt` | 인터페이스 | playQuestionWithHighlight, startRecording, stopRecording, playRecording, hasRecording, getRecordingPath, clearRecording |
| `AudioFileManager.kt` | 인터페이스 | 오디오 병합/저장/삭제 |
| `ScriptProgress.kt` | 데이터 클래스 | 진행상황 상태 (category, index, sentenceIndex, totalSentences, isMemorizeTestRunning, needsSave). getKey(), toPersistable() 포함 |

### 주의: QaDataManager는 인터페이스가 아닌 구현체
`domain/repository/`에 위치하지만 인터페이스가 아님. SharedPreferences, Application context를 직접 사용.
→ **기술 부채: ISP 위반 + Android 의존성.** 향후 인터페이스 분리 및 Data 계층 이동 필요.

## usecase/ — 비즈니스 로직

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `ExecuteRepeatListeningUseCase.kt` | RepeatListeningRepository 위임 래퍼 | 얇은 래퍼 |
| `ExecuteEnglishWritingTestUseCase.kt` | EnglishWritingTestRepository 위임 래퍼 | 얇은 래퍼 |
| `ExecuteFullMemorizationUseCase.kt` | FullMemorizationRepository 위임 | start/stopRecording/playRecording |
| `FullMemorizationUseCase.kt` | TtsOrchestrator + RecordingFileRepository + AudioRecorder + QaDataManager + RecordingTimeManager 직접 사용 | highlightIndex StateFlow 제공, CoroutineScope( SupervisorJob + IO) 사용 |
| `RepeatListeningUseCase.kt` | 독립 반복듣기 로직. 문장별 한국어→영어 N회 | TTS duration 저장, 적응형 딜레이 |
| `PlayMergedFileUseCase.kt` | 영작테스트 병합 파일 재생 + 하이라이트 | 타이밍 추정: `(문자수 * 50ms).coerceAtLeast(1000ms)` (부정확) |
| `MemorizeTestProgressTracker.kt` | **@Singleton** 진행상황 메모리 관리 + 영속화 | 변경된 항목만 저장 (needsSave 플래그) |

### 두 FullMemorization UseCase 중복
`ExecuteFullMemorizationUseCase` (Repository 위임) vs `FullMemorizationUseCase` (Orchestrator 직접 사용).
현재 MainScreen에서는 `FullMemorizationUseCase` 사용 중.
→ **기술 부채: 통합 필요**

## 아키텍처 규칙
- Data 계층을 직접 import하지 않음 (인터페이스로만 의존)
- Entity는 data class 위주. getKey()/toPersistable() 등 경미한 로직만 허용
- UseCase는 단일 책임, Repository 인터페이스만 참조

### 알려진 규칙 위반
Domain 계층에 Android 프레임워크 import가 존재함:
- `TtsOrchestrator` → Context, Build, Log
- `TtsPlaybackController` → Log
- `QaDataManager` → Application, Context, SharedPreferences, Log
- `WakeLockManager` → Context, PowerManager, Log
- 여러 UseCase → Log
→ **기술 부채: 순수 Kotlin 도메인 목표 달성 불가.** Log는 허용 범위, Context/SharedPreferences는 Data 계층으로 이동 필요.
