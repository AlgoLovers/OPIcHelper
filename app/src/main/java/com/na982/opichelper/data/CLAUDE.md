# Data Layer — 구현체 & 인프라

Domain 계층의 인터페이스를 구현하고, Android 프레임워크 의존성(Context, SharedPreferences, MediaRecorder 등)을 처리.

## 패키지 구조

```
data/
  audio/           — TTS/녹음 하드웨어 구현체
  repository/      — Repository 인터페이스 구현체
```

## audio/ — TTS & 오디오 하드웨어

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `BaseTtsPlayer.kt` | Android TTS 추상 베이스. speak/stop/release 공통 로직 | `initializeTts()`에서 TextToSpeech 콜백으로 초기화. speak()는 Mutex로 직렬화, TtsSpeakResult 반환 (Success(durationMs)/Error/Timeout/Unavailable). onStart 콜백으로 재생 시작 보장(2초 타임아웃), 재생 완료까지 대기(30초 타임아웃), 능동적 stop() + isSpeaking 폴링으로 엔진 정지 대기. CancellationException 시 tts?.stop() 호출 |
| `GoogleTtsPlayer.kt` | 영어 TTS (Locale.US) | `@Named("google")`으로 AppModule에서 주입. speakAndGetDuration()에서 버전별 속도 최적화 |
| `SamsungTtsPlayer.kt` | 한국어 TTS (Locale.KOREAN) | `@Named("samsung")`으로 AppModule에서 주입. 한글 TTS 실패 시 폴백은 TtsOrchestrator에서 처리(index 0 리셋) |
| `AudioRecorderImpl.kt` | MediaRecorder(context) 기반 녹음. AAC/M4A 128kbps, 44100Hz | stopRecording 내부에서 release 자동 호출. Context 필요 생성자 사용 |
| `AudioPlayerImpl.kt` | MediaPlayer 기반 오디오 파일 재생, getDuration 지원 | play(), playAudio(), stop(), stopAudio(), release(), getDuration() 제공 |
| `RecordingAudioPlayerImpl.kt` | 사용자 녹음 재생 전용 MediaPlayer | startRecordingPlayback() 동기 메서드, getDuration() 제공 |

### BaseTtsPlayer.speak() 재생 보장 메커니즘

1. `tts?.stop()` + `isSpeaking` 폴링: 능동적으로 엔진 정지 후 완전히 idle일 때까지 대기 (최대 2초)
2. `tts.speak()` 반환값 검사: `ERROR` 반환 시 즉시 `TtsSpeakResult.Error` 반환
3. `onStart` 콜백 대기: 실제 재생 시작 확인 (2초 타임아웃, 초과 시 `TtsSpeakResult.Error` 반환)
4. `completionDeferred.await()`: 재생 완료까지 대기 (30초 안전 타임아웃, 초과 시 `TtsSpeakResult.Timeout` 반환)
5. `CancellationException` 시 `tts?.stop()` 호출 후 재throw

이 메커니즘은 간헐적 TTS 재생 불가 버그를 방지합니다. speak()는 재생 완료 후 `TtsSpeakResult`를 반환합니다.

### TTS 레이트 로직

사용자가 `setSpeechRate()`로 설정한 값이 우선. 미설정 시 SDK 버전별 기본값 사용.

| Android 버전 | Google (영어) 기본 | Samsung (한국어) 기본 |
|-------------|-------------------|---------------------|
| 14+ | 0.8f | 1.1f |
| 13 | 0.8f | 0.9f |
| 12 이하 | 0.7f | 0.8f |

영어 TTS 속도는 설정 화면 슬라이더(0.5x~1.5x)에서 제어. `TtsOrchestrator.speakEnglish()`에서 `UserPreferencesRepository.getEnglishTtsRate()` 읽어 `GoogleTtsPlayer.setSpeechRate()` 적용.

## repository/ — 데이터 영속성 & 비즈니스 로직 구현

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `LeveledQaDataLoader.kt` | JSON assets → QaItem 파싱. `{title, items}` 구조 로딩 | 새 JSON 추가 시 코드 수정 불필요 (title에서 동적 카테고리 생성) |
| `ProgressPersistenceServiceImpl.kt` | 진행상황 + 네비게이션 상태 SharedPreferences 영속화 (opic_prefs) | NavigationState(category, index) 저장/로드 포함 |
| `UserPreferencesRepository.kt` | 사용자 설정 (레벨, TTS 속도, 암기레벨) 관리 (user_prefs) | Domain 인터페이스 구현. StateFlow로 레벨 변경 감지 가능. `last_memorize_level` 키 포함 |
| `RecordingFileRepositoryImpl.kt` | 녹음 파일 CRUD, 재생 | 파일 생성 시 카테고리/인덱스 기반 경로. currentRecordingPath/currentPlayingPath 동기화 미비 |
| `RecordingTimeManagerImpl.kt` | 문장별 녹음 시간 저장/조회 (recording_times) | Gson으로 Long 리스트 직렬화 |
| `RepeatListeningRepositoryImpl.kt` | 반복듣기: 한국어 TTS → 영어 TTS N회 반복 | SharedFlow\<MemorizeTestEvent\>로 이벤트 발행. ProgressPersistenceService로 진행상황 영속화 |
| `EnglishWritingTestRepositoryImpl.kt` | 영작테스트: 한국어 TTS → 녹음 반복 | SharedFlow\<MemorizeTestEvent\>로 이벤트 발행. 병합은 AudioFileManager에 위임 |
| `AudioFileManagerImpl.kt` | 오디오 파일 병합/저장/삭제 | mergeAndSaveAudioFiles()는 폴백 전략 3단계 (mergeWithMediaCodec → mergeWithHeaderAnalysis → 파일 연결). mergeAudioFiles()는 폴백 없이 MediaCodec만 사용 |

## SharedPreferences 키 맵

| 이름 | 키 | 사용 위치 |
|------|-----|----------|
| `opic_prefs` | `last_category`, `last_index`, `app_exit_state`, `category_progress_*` | ProgressPersistenceServiceImpl |
| `user_prefs` | `user_level`, `english_tts_rate`, `last_memorize_level` | UserPreferencesRepository |
| `recording_times` | `recording_times_{category}_{scriptIndex}` | RecordingTimeManagerImpl |

## 아키텍처 규칙
- Domain 인터페이스와 entity만 import. Domain 구현체(UseCase, QaDataManager, Manager)를 직접 import하지 않음
- 모든 싱글톤 바인딩은 `di/AppModule.kt`에서 처리
- Android Context가 필요한 클래스는 AppModule의 `@Provides`에서 생성하거나 `@Inject constructor`로 주입
