# CLAUDE.md - OPIc Helper 개발 가이드

## 프로젝트 개요

OPIc 영어 말하기 시험 대비 Android 앱. Clean Architecture + MVVM, Hilt DI, Jetpack Compose.

- **패키지**: `com.na982.opichelper`
- **minSdk**: 24 / **targetSdk**: 34
- **Kotlin**: 1.9.22 / **Compose BOM**: 2023.08.00

## 아키텍처

```
Presentation (Compose UI + ViewModel) → Domain (Entity + UseCase + Repository Interface) ← Data (Repository Impl + TTS Players + File Manager)
```

### ViewModel 구조
- `MainViewModel` (**AndroidViewModel**): 통합 상태 관리 (AppState 단일 StateFlow, 의존성 6개)
- `MemorizationViewModel`: 암기 테스트 로직 (반복듣기/영작테스트/통암기)
- TTS 상태는 MainViewModel이 TtsPlaybackController 직접 구독 (별도 TtsViewModel 없음)

### TTS 구조
- `TtsOrchestrator`: 언어 감지 → Google TTS(영문) / Samsung TTS(한글) 라우팅
- `TtsPlaybackController`: 재생 상태 관리 (4개 highlightIndex StateFlow)
- `BaseTtsPlayer` → `GoogleTtsPlayer`, `SamsungTtsPlayer`

### DI
- 모든 싱글톤 바인딩은 `di/AppModule.kt`
- ViewModel은 `@HiltViewModel` + `@Inject constructor`

### 모듈별 상세 문서
- `data/CLAUDE.md` — Data 계층 구현체 & 인프라
- `domain/CLAUDE.md` — Domain 계층 엔티티, 유스케이스, 인터페이스
- `presentation/CLAUDE.md` — Presentation 계층 UI, ViewModel, 네비게이션
- `di/CLAUDE.md` — Hilt DI 바인딩 설정

## 앱 진입점 & 루트 파일

| 파일 | 역할 |
|------|------|
| `OPicHelperApplication.kt` | `@HiltAndroidApp`. TtsOrchestrator 싱글톤 보유 |
| `SplashActivity.kt` | 런처 Activity. 2초 스플래시 후 MainActivity 이동 |
| `MainActivity.kt` | `@AndroidEntryPoint`. RECORD_AUDIO 권한, WakeLock 수명주기, 백버튼 리소스 정리 |

## Assets 구조

```
assets/
  al/           — Advanced Low 레벨 (15개 JSON)
  ih/           — Intermediate High 레벨 (16개 JSON, qa_roleplay 포함)
  ih_raw/       — IH Raw 레벨 (15개 JSON)
  im/           — Intermediate Mid 레벨 (15개 JSON)
```

JSON 포맷: `{ "title": "한글 카테고리명", "items": [{ id, question_en, question_ko, answer_en, answer_ko }] }`
`theme` 필드는 일부 JSON에만 존재하며 파싱 시 무시됨.
새 JSON 추가 시 코드 수정 없이 assets에 넣기만 하면 자동 인식 (동적 카테고리 로딩).

## 테스트 구조

### 단위 테스트 (`app/src/test/`)
`TtsOrchestratorTest`, `TtsPlaybackControllerTest`, `EnglishWritingTestUseCaseTest`, `RepeatListeningUseCaseTest`, `BaseTtsPlayerTest`, `AudioFileRepositoryImplTest`, `MainViewModelTest`, `TtsViewModelTest`(고아), `MemorizationViewModelTest`

### 계측 테스트 (`app/src/androidTest/`)
`SimpleTtsTest`, `RepeatListeningModeTest`, `RepeatListeningProgressTest`, `TtsStateIntegrationTest`, `EnglishWritingTestMergedFileTest`, `EnglishWritingTestProgressTest`, `SequentialAudioPlaybackTest`, `TtsIntegrationTest`, `FlipCardInstrumentedTest`, `TtsRegressionTest`

**주의**: `TtsViewModelTest`는 TtsViewModel이 삭제된 후 남은 고아 테스트. 정리 필요.

## 코딩 컨벤션

### SOLID 원칙
- **SRP**: 각 클래스는 하나의 변경 이유만 가져야 함. ViewModel이 비대해지면 새 ViewModel로 분리
- **OCP**: TTS 플레이어 추가 시 `TtsPlayer` 인터페이스 구현체만 추가 (Orchestrator 수정 불필요)
- **LSP**: BaseTtsPlayer 하위 클래스는 치환 가능해야 함
- **ISP**: Repository 인터페이스는 UI 콜백을 받지 않아야 함 (현재 RepeatListeningRepository, EnglishWritingTestRepository 위반 — 리팩토링 필요)
- **DIP**: Domain 계층은 Data 계층을 직접 참조하지 않아야 함 (현재 QaDataManager가 SharedPreferences, Application 직접 사용 — 수정 필요)

### 네이밍
- Repository 인터페이스: `domain/repository/`에 정의
- Repository 구현체: `data/repository/`에 `~Impl` 접미사
- UseCase: `domain/usecase/`에 `~UseCase` 접미사 (Service 패턴 혼용 주의)
- StateFlow: `_` 접두사는 private, 공개는 읽기 전용

### Compose
- UI 컴포넌트는 `presentation/ui/screen/MainScreenComponentsUI/`에 분리
- 재사용 컴포넌트는 `presentation/ui/component/`
- 상태는 ViewModel의 StateFlow를 `collectAsState()`로 구독

### 로깅
- Tag: 클래스명 사용
- 프로덕션 코드에서 과도한 Log.d 제거 (TtsOrchestrator, TtsPlaybackController 등에 과다 로깅 존재)

## 코드 리뷰 체크리스트

### 필수 확인
- [ ] Domain 계층이 Data 계층을 직접 import하지 않는지
- [ ] Repository 인터페이스에 UI 콜백이 없는지
- [ ] CoroutineScope가 적절히 취소되는지 (메모리 누수 방지)
- [ ] StateFlow 업데이트가 스레드 안전한지
- [ ] 하드코딩된 문자열이 string resource로 분리되었는지

### 보안 확인
- [ ] API 키, 비밀번호 등이 코드에 하드코딩되지 않았는지
- [ ] SharedPreferences에 민감 정보를 평문 저장하지 않는지
- [ ] 권한이 최소한으로 선언되었는지

### 테스트
- 단위 테스트: `app/src/test/`
- 계측 테스트: `app/src/androidTest/`
- 실행: `./gradlew testDebugUnitTest`

## 알려진 기술 부채

| 항목 | 상태 | 우선순위 |
|------|------|----------|
| QaDataManager Android 의존성 (SharedPreferences, Application) | 미해결 | 높음 |
| Repository 인터페이스 UI 콜백 결합 (RepeatListening, EnglishWritingTest) | 미해결 | 높음 |
| MainViewModel God Class 경향 | 미해결 | 높음 |
| MemorizationViewModel SRP 위반 (3개 모드 통합) | 미해결 | 중간 |
| FullMemorization UseCase 중복 (Execute vs 직접) | 미해결 | 중간 |
| QaDataLoaderImpl 미사용 래퍼 | 미해결 | 낮음 |
| PlaybackEvent 미사용 코드 | 미해결 | 낮음 |
| TtsViewModelTest 고아 테스트 | 미해결 | 낮음 |
| ScriptProgress가 domain/repository에 위치 | 미해결 | 낮음 |
| Domain 계층 Android import (Log, Context) | 미해결 | 낮음 |
| WakeLock deprecated API | @Suppress 처리 | 낮음 |
| LoginScreen 미작동 (인증 로직 비활성) | 미해결 | 낮음 |

## Git 커밋 규칙

- 커밋 메시지는 한글로 작성
- 커밋 메시지에 AI 코딩 관련 내용(Co-Authored-By 등)을 포함하지 않음
- 커밋 생성 후 반드시 패치 파일을 생성: `git format-patch -1 HEAD`
- 커밋 메시지 마지막에 패치 파일명을 포함: `Patch: 000X-xxx.patch`
- 패치 파일명 형식: `NNNN-간단한설명.patch` (N은 0패딩 순번)

### 커밋 메시지 형식
```
<type>: <한글 설명>

Patch: 000X-xxx.patch
```

### 예시
```
fix: TTS 음성 재생 불가 버그 수정 및 초기 진입 화면 수정

Patch: 0008-tts-and-ui-bugfix.patch
```

## 빌드 명령어

```bash
./gradlew assembleDebug          # 디버그 빌드
./gradlew testDebugUnitTest      # 단위 테스트
./gradlew connectedDebugAndroidTest  # 계측 테스트
./gradlew lint                   # 정적 분석
```
