# Data Layer — 구현체 & 인프라

## 역할
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
| `BaseTtsPlayer.kt` | Android TTS 추상 베이스. speak/stop/pause/resume 공통 로직 | 하위 클래스는 반드시 `onInit()`에서 엔진 초기화 대기 |
| `GoogleTtsPlayer.kt` | 영어 TTS (Locale.US) | `@Named("google")`으로 주입 |
| `SamsungTtsPlayer.kt` | 한국어 TTS (Locale.KOREAN) | `@Named("samsung")`으로 주입, 한국어 TTS 실패 시 index 0 리셋 |
| `AudioRecorderImpl.kt` | MediaRecorder 기반 녹음. AAC/M4A 128kbps | stopRecording 내부에서 release 자동 호출 |
| `AudioPlayerImpl.kt` | MediaPlayer 기반 오디오 파일 재생, getDuration 지원 | TTS 재생과 별개, 완료 콜백 제공 |
| `RecordingAudioPlayerImpl.kt` | 사용자 녹음 재생 전용 MediaPlayer, startRecordingPlayback 동기 메서드 | AudioPlayerImpl과 분리된 인스턴스 |

### TTS 레이트 로직

| Android 버전 | Google (영어) | Samsung (한국어) |
|-------------|---------------|------------------|
| 14+ | 0.8f | 1.1f |
| 13 | 0.8f | 0.9f |
| 12 이하 | 0.7f | 0.8f |

## repository/ — 데이터 영속성 & 비즈니스 로직 구현

| 파일 | 역할 | 주의사항 |
|------|------|----------|
| `LeveledQaDataLoader.kt` | JSON assets → QaItem 파싱. `{title, items}` 구조 로딩 | 새 JSON 추가 시 코드 수정 불필요 (title에서 동적 카테고리 생성) |
| `QaDataLoaderImpl.kt` | LeveledQaDataLoader에 위임만 하는 래퍼 | **TODO: 제거 대상** (AppModule에 바인딩되지 않음, 미사용 코드) |
| `ProgressPersistenceServiceImpl.kt` | 진행상황 SharedPreferences 영속화 (opic_progress) | 예외 발생 시 null 반환, 유저 피드백 없음 |
| `UserPreferencesRepository.kt` | 사용자 설정 (레벨, TTS 속도) 관리 (user_prefs) | StateFlow로 레벨 변경 감지 가능 |
| `AuthRepository.kt` | 로그인 상태 관리 (auth_prefs) | 현재 게스트 로그인만 작동 |
| `RecordingFileRepositoryImpl.kt` | 녹음 파일 CRUD, 재생 | 파일 생성 시 카테고리/인덱스 기반 경로 |
| `RecordingTimeManagerImpl.kt` | 문장별 녹음 시간 저장/조회 (recording_times) | Gson으로 Long 리스트 직렬화 |
| `RepeatListeningRepositoryImpl.kt` | 반복듣기: 한국어 TTS → 영어 TTS N회 반복 | TTS duration 저장, 적응형 딜레이 |
| `EnglishWritingTestRepositoryImpl.kt` | 영작테스트: 한국어 TTS → 녹음 반복 | 개별 녹음 병합(M4A) 기능 포함 |
| `FullMemorizationRepositoryImpl.kt` | 통암기: 질문 TTS → 녹음 | 파일 삭제 시 실패해도 path 초기화 (경미한 이슈) |
| `AudioFileManagerImpl.kt` | 오디오 파일 병합/저장/삭제 | MediaMuxer + MediaCodec, 폴백 전략 있음 |

## SharedPreferences 키 맵

| 이름 | 키 | 사용 위치 |
|------|-----|----------|
| `opic_prefs` | `last_category`, `last_index` (QaDataManager), `last_memorize_level` (MainViewModel) | QaDataManager, MainViewModel |
| `opic_progress` | `app_exit_state`, `category_progress_*` | ProgressPersistenceServiceImpl |
| `user_prefs` | `user_level`, `english_tts_rate` | UserPreferencesRepository |
| `auth_prefs` | `is_logged_in`, `user_name` 등 | AuthRepository |
| `recording_times` | `recording_times_{category}_{scriptIndex}` | RecordingTimeManagerImpl |

## 아키텍처 규칙
- Domain 인터페이스와 entity만 import. Domain 구현체(UseCase, QaDataManager, Manager)를 직접 import하지 않음
- 모든 싱글톤 바인딩은 `di/AppModule.kt`에서 처리
- Android Context가 필요한 클래스는 `@Inject constructor`로 주입
