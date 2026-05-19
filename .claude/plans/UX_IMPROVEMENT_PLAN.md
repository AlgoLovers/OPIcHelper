# 치명적/높음 UX 개선 계획서

## Context

앱의 핵심 UX 문제 5가지를 해결:
1. **에러 피드백 없음** — TTS/녹음 실패 시 사용자가 아무것도 모름
2. **접근성 거의 없음** — contentDescription 3개뿐
3. **온보딩 없음** — 새 사용자가 앱 사용법을 모름
4. **진행상황 UI 없음** — 백엔드는 있는데 UI에 안 보임
5. **검색 없음** — 카테고리/문항을 찾을 방법이 없음

---

## 1. 에러 피드백 시스템

### 1-1. Scaffold + SnackbarHost 인프라 구축

**파일**: `MainScreen.kt`, `SettingsScreen.kt`

- `Surface` → `Scaffold` + `SnackbarHost` 로 교체
- `SnackbarHostState`를 remember로 생성
- ViewModel에서 발생하는 이벤트를 Snackbar로 표시

### 1-2. UiEvent 공유 플로우 도입

**파일**: `BaseMemorizationViewModel.kt`, `PlaybackViewModel.kt`

BaseMemorizationViewModel에 추가:
```kotlin
private val _events = MutableSharedFlow<String>(extraBufferCapacity = 5)
val events: SharedFlow<String> = _events.asSharedFlow()

protected suspend fun emitEvent(message: String) {
    _events.emit(message)
}
```

PlaybackViewModel은 BaseMemorizationViewModel을 상속하지 않으므로 동일한 패턴을 직접 구현.
QaBrowserViewModel도 BaseMemorizationViewModel을 상속하지 않으므로 동일한 패턴을 직접 구현.

- 각 ViewModel에서 에러 발생 시 `emitEvent("TTS 재생에 실패했습니다")` 호출
- MainScreen에서 각 ViewModel의 events를 `LaunchedEffect`로 수집하여 Snackbar 표시

### 1-3. 주요 에러 포인트에 이벤트 추가

| ViewModel | 에러 상황 | 메시지 |
|-----------|----------|--------|
| PlaybackViewModel | TTS 재생 실패 | "TTS 재생에 실패했습니다" |
| RepeatListeningViewModel | 반복듣기 시작 실패 | "반복듣기를 시작할 수 없습니다" |
| EnglishWritingTestViewModel | 영작 테스트 시작 실패 | "영작 테스트를 시작할 수 없습니다" |
| FullMemorizationViewModel | 통암기 시작/녹음/재생 실패 | 각각 메시지 |
| QaBrowserViewModel | 데이터 로딩 실패 | "데이터를 불러올 수 없습니다" |

### 1-4. 권한 거부 피드백

**파일**: `MainActivity.kt`, `MainScreen.kt`

- MainActivity에서 권한 거부 결과를 StateFlow로 노출
- MainScreen에서 해당 StateFlow를 수집하여 Snackbar로 안내
- (MainActivity는 Scaffold 밖에 있어 SnackbarHostState에 직접 접근 불가하므로, 상태를 MainScreen으로 전달)

---

## 2. 접근성 개선

### 2-1. 버튼 contentDescription 추가 (우선순위 높음)

**파일**: `MainScreenComponentsUI/` 하위 버튼 컴포넌트

| 컴포넌트 | contentDescription |
|----------|-------------------|
| QuestionPlayButton | "질문 재생" / "질문 재생 정지" |
| AnswerPlayButton | "답변 재생" / "답변 재생 정지" |
| FullMemorizationRecordingButton | "녹음 시작" / "녹음 정지" |
| MemorizeLevelPlaybackButton | 상황에 맞는 설명 |
| NavigationSection | "이전 질문" / "다음 질문" |

나머지 컴포넌트(카드 셀렉터, 레벨 셀렉터 등)는 후순위.

### 2-2. FlipCard 접근성

**파일**: `FlipCard.kt`

- `Modifier.semantics` 추가: "탭하여 영어/한국어 전환"
- `role = Role.Button` 설정

### 2-3. HighlightText 접근성

**파일**: `HighlightText.kt`

- 하이라이트된 문장에 `semantics { stateDescription = "현재 재생 중" }` 추가

### 2-4. 설정 버튼 개선

**파일**: `AppTitle.kt`

- 3dp 점 → `Icons.Filled.Settings` 아이콘으로 교체
- `contentDescription = "설정"` 추가

### 2-5. 최소 터치 타겟 보장

- AnswerCard 숨기기/보이기 버튼: 24dp → 36dp
- MemorizeLevelSelector 화살표: 48dp 터치 타겟 보장

---

## 3. 온보딩 시스템

### 3-1. 첫 실행 감지

**파일**: `UserPreferencesRepository.kt`, `UserPreferencesRepositoryImpl.kt`

- `isOnboardingCompleted(): Boolean` / `setOnboardingCompleted()` 추가
- SharedPreferences 키: `onboarding_completed`

### 3-2. 온보딩 다이얼로그

**파일**: 신규 `presentation/ui/component/OnboardingDialog.kt`

첫 실행 시 MainScreen에 간결한 다이얼로그 표시 (1페이지):
- "OPic Helper에 오신 것을 환영합니다!"
- 암기레벨 3가지 한 줄 설명
- "시작하기" 버튼

### 3-3. MainScreen에 온보딩 연동

**파일**: `MainScreen.kt`, `QaBrowserViewModel.kt`

- QaBrowserState에 `isOnboardingCompleted: Boolean` 필드 추가
- QaBrowserViewModel에서 UserPreferencesRepository 구독하여 상태 제공
- MainScreen에서 `qaState.isOnboardingCompleted`가 false면 다이얼로그 표시
- 다이얼로그 "시작하기" 클릭 시 `qaViewModel.setOnboardingCompleted()` 호출

---

## 4. 진행상황 UI

### 4-1. 프로그레스 바 추가

**파일**: `QuestionCard.kt`, `MainScreen.kt`

- QuestionCard에 `completedCount: Int = 0` 파라미터 추가
- 기존 "X / Y" 텍스트 → 선형 프로그레스 바 + "X / Y" 텍스트
- 프로그레스 바: `LinearProgressIndicator(progress = completedCount.toFloat() / totalCount)`
- MainScreen에서 `qaState.completedCount`를 QuestionCard에 전달

### 4-2. 이어서 듣기 프롬프트

**파일**: `MainScreen.kt`

- 앱 시작 시 저장된 진행상황이 있으면 Snackbar로 "이전 위치에서 이어서 듣기 가능" 안내
- Snackbar 액션 버튼 없이 텍스트만 표시 (간단한 구현)

### 4-3. QaBrowserState에 진행 데이터 추가

**파일**: `QaBrowserViewModel.kt`

- `completedCount: Int` 필드 추가 (현재 카테고리에서 학습 완료된 문항 수)
- `MemorizeTestProgressTracker.progressMap` 구독
- 완료 기준: `currentSentenceIndex >= totalSentences - 1 && !isMemorizeTestRunning` 인 항목 수

---

## 5. 검색 기능

### 5-1. 검색 아이콘 추가

**파일**: `AppTitle.kt`

- 설정 아이콘 옆에 검색 아이콘 추가
- 클릭 시 검색 다이얼로그 표시

### 5-2. 검색 다이얼로그

**파일**: 신규 `presentation/ui/component/SearchDialog.kt`

- `TextField` + 결과 리스트
- QaDataManager의 itemsByCategory에서 질문/답변 텍스트 검색
- 결과 클릭 시 해당 카테고리/문항으로 이동

### 5-3. 검색 UseCase 추가

**파일**: 신규 `domain/usecase/SearchQaItemsUseCase.kt`

QaDataManager에 직접 search()를 넣지 않고 UseCase로 분리 (Domain 계층에 UI 로직 침투 방지):
```kotlin
class SearchQaItemsUseCase(private val qaDataManager: QaDataManager) {
    fun search(query: String): List<QaItem> {
        if (query.length < 2) return emptyList()
        return qaDataManager.searchItems(query)
    }
}
```

QaDataManager에는 최소한의 검색 지원만 추가:
```kotlin
fun searchItems(query: String): List<QaItem> {
    return itemsByCategory.values.flatten().filter { ... }
}
```

### 5-4. QaBrowserViewModel에 검색 상태 추가

**파일**: `QaBrowserViewModel.kt`, `QaDataManager.kt`

- QaBrowserState에 `searchResults: List<QaItem> = emptyList()` 필드 추가
- `search(query: String)` / `clearSearch()` 메서드
- 검색 결과 선택 시:
  1. `qaDataManager.selectCategory(item.category)` — 카테고리 이동
  2. `qaDataManager.navigateToIndex(index)` — 해당 문항으로 이동 (신규 메서드 필요)
- QaDataManager에 `navigateToIndex(index: Int)` 추가: 현재 카테고리 내에서 인덱스 이동

---

## 수정 파일 요약

| 파일 | 변경 |
|------|------|
| `MainScreen.kt` | Scaffold+SnackbarHost, 온보딩 다이얼로그, 이어서 듣기 프롬프트, 검색 연동 |
| `SettingsScreen.kt` | Scaffold+SnackbarHost |
| `BaseMemorizationViewModel.kt` | UiEvent SharedFlow 추가 |
| `PlaybackViewModel.kt` | 에러 이벤트 발생 |
| `RepeatListeningViewModel.kt` | 에러 이벤트 발생 |
| `EnglishWritingTestViewModel.kt` | 에러 이벤트 발생 |
| `FullMemorizationViewModel.kt` | 에러 이벤트 발생 |
| `QaBrowserViewModel.kt` | 진행상황 데이터, 검색 상태, 온보딩 상태 |
| `QaDataManager.kt` | searchItems(), navigateToIndex() 메서드 추가 |
| 신규 `SearchQaItemsUseCase.kt` | 검색 UseCase (@Inject constructor로 자동 등록) |
| `UserPreferencesRepository.kt` | isOnboardingCompleted() 추가 |
| `UserPreferencesRepositoryImpl.kt` | 온보딩 키 구현 |
| `MainActivity.kt` | 권한 거부 피드백 |
| `AppTitle.kt` | 설정 아이콘 교체, 검색 아이콘 추가 |
| `QuestionCard.kt` | 프로그레스 바 |
| `HighlightText.kt` | 접근성 semantics |
| `FlipCard.kt` | 접근성 semantics |
| `AnswerCard.kt` | 터치 타겟, 접근성 |
| `MemorizeLevelSelector.kt` | 터치 타겟, 접근성 |
| 신규 `OnboardingDialog.kt` | 온보딩 다이얼로그 |
| 신규 `SearchDialog.kt` | 검색 다이얼로그 |

## 구현 순서

1 → 3 → 4 → 2 → 5 (에러 피드백이 기반 인프라, 온보딩/진행상황이 Snackbar에 의존, 접근성/검색은 독립적)

## 검증

1. `./gradlew assembleDebug` 빌드 성공
2. TTS 실패 시 Snackbar 표시 확인
3. 녹음 권한 거부 시 안내 메시지 확인
4. 첫 실행 시 온보딩 다이얼로그 표시, 두 번째 실행 시 미표시
5. 프로그레스 바가 카테고리 진행도를 시각적으로 표시
6. 검색으로 질문/답변 텍스트 검색 후 해당 문항으로 이동
7. TalkBack 활성화 시 버튼/카드 설명 읽기 확인
