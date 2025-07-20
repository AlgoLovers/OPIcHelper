# Presentation Layer (프레젠테이션 레이어)

## 개요

Presentation Layer는 사용자와의 상호작용을 담당하는 레이어입니다. Clean Architecture에서 가장 바깥쪽에 위치하며, UI 표시와 사용자 입력 처리를 담당합니다.

## 구조

```
presentation/
├── ui/
│   ├── component/          # 재사용 가능한 UI 컴포넌트
│   │   ├── FlipCard.kt
│   │   ├── TtsService.kt
│   │   └── SpeechRecognizerHelper.kt
│   ├── screen/            # 화면 컴포넌트
│   │   └── MainScreen.kt
│   └── theme/             # 테마 및 스타일
└── viewmodel/             # ViewModel
    └── MainViewModel.kt
```

## 주요 컴포넌트

### 1. MainViewModel

**위치**: `presentation/viewmodel/MainViewModel.kt`

**역할**: 메인 화면의 상태 관리 및 비즈니스 로직 처리

**주요 기능**:
- 질문 데이터 로딩 및 관리
- 현재 질문 인덱스 관리
- 카테고리 변경 처리
- UI 상태 관리 (로딩, 에러, 성공)

**상태 관리**:
```kotlin
data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<QuestionCategory> = emptyList(),
    val currentCategory: QuestionCategory? = null,
    val currentQaItem: Question? = null,
    val questionHighlightIndex: Int? = null,
    val answerHighlightIndex: Int? = null
)
```

**주요 메서드**:
- `loadQuestions()`: 질문 데이터 로딩
- `nextQaItem()`: 다음 질문으로 이동
- `setQuestionHighlightIndex()`: 질문 하이라이트 인덱스 설정
- `setAnswerHighlightIndex()`: 답변 하이라이트 인덱스 설정

### 2. MainScreen

**위치**: `presentation/ui/screen/MainScreen.kt`

**역할**: 메인 화면의 UI 구성

**주요 기능**:
- 질문/답변 카드 표시
- TTS 재생 버튼
- 음성 인식 버튼
- 카테고리 선택
- 하이라이트 표시

**UI 구성 요소**:
- `QuestionCard`: 질문을 표시하는 카드
- `AnswerCard`: 답변을 표시하는 카드
- 카테고리 선택 드롭다운
- TTS 재생 버튼들
- 음성 인식 버튼

**상태 바인딩**:
```kotlin
val uiState by viewModel.uiState.collectAsState()
val questionHighlightIndex by viewModel.questionHighlightIndex.collectAsState()
val answerHighlightIndex by viewModel.answerHighlightIndex.collectAsState()
```

### 3. TtsService

**위치**: `presentation/ui/component/TtsService.kt`

**역할**: Text-to-Speech 기능 제공

**주요 기능**:
- 영어 텍스트를 음성으로 변환
- 문장별 하이라이트 콜백
- 오디오 포커스 관리
- 포그라운드 서비스 실행

**핵심 메서드**:
```kotlin
fun speakQuestion(text: String, rate: Float = 0.8f)
fun speakAnswer(text: String, rate: Float = 0.8f)
fun speakBySentence(text: String, repeatCount: Int = 5, pauseRatio: Float = 1.5f, rate: Float = 0.8f)
fun stopTts()
fun setHighlightCallback(callback: HighlightCallback?)
```

**서비스 바인딩**:
```kotlin
class TtsBinder : Binder() {
    fun getService(): TtsService = this@TtsService
}
```

**하이라이트 콜백**:
```kotlin
interface HighlightCallback {
    fun onQuestionHighlight(index: Int?)
    fun onAnswerHighlight(index: Int?)
}
```

### 4. SpeechRecognizerHelper

**위치**: `presentation/ui/component/SpeechRecognizerHelper.kt`

**역할**: 음성 인식 기능 제공

**주요 기능**:
- 실시간 음성 인식
- 부분 결과 및 최종 결과 처리
- 에러 처리
- RecognitionCallback 인터페이스를 통한 결과 전달

**인터페이스**:
```kotlin
interface RecognitionCallback {
    fun onPartialResult(text: String)
    fun onFinalResult(text: String)
    fun onError(error: String)
}
```

**핵심 메서드**:
```kotlin
fun startListening()
fun stopListening()
fun destroy()
```

**사용 예시**:
```kotlin
val speechHelper = SpeechRecognizerHelper(context).apply {
    recognitionCallback = object : RecognitionCallback {
        override fun onPartialResult(text: String) { /* 처리 */ }
        override fun onFinalResult(text: String) { /* 처리 */ }
        override fun onError(error: String) { /* 처리 */ }
    }
}
```

### 5. FlipCard

**위치**: `presentation/ui/component/FlipCard.kt`

**역할**: 카드 뒤집기 애니메이션 컴포넌트

**주요 기능**:
- 3D 카드 뒤집기 애니메이션
- 질문/답변 전환
- 사용자 터치 인터랙션

**사용법**:
```kotlin
FlipCard(
    modifier = Modifier,
    frontContent = { /* 질문 내용 */ },
    backContent = { /* 답변 내용 */ }
)
```

## 데이터 흐름

### 1. 사용자 입력 처리
```
사용자 액션 → MainScreen → MainViewModel → Domain Layer
```

### 2. 상태 업데이트
```
Domain Layer → MainViewModel → MainScreen → UI 업데이트
```

### 3. TTS 재생
```
버튼 클릭 → MainScreen → TtsService → 하이라이트 콜백 → UI 업데이트
```

### 4. 음성 인식
```
버튼 클릭 → MainScreen → SpeechRecognizerHelper → RecognitionCallback → UI 업데이트
```

## 테스트 전략

### 단위 테스트
- **MainViewModel**: 상태 변경, 비즈니스 로직
- **UI 컴포넌트**: 사용자 인터랙션, 상태 표시

### 계측 테스트
- **TtsService**: 실제 TTS 동작
- **SpeechRecognizerHelper**: 실제 음성 인식
- **MainScreen**: 전체 UI 흐름

## 모범 사례

### 1. 상태 관리
- ViewModel에서 모든 상태를 관리
- UI는 상태를 표시하는 역할만 수행
- 단방향 데이터 흐름 유지

### 2. 컴포넌트 분리
- 재사용 가능한 컴포넌트는 별도로 분리
- 각 컴포넌트는 단일 책임 원칙 준수
- 인터페이스를 통한 의존성 주입

### 3. 에러 처리
- 사용자 친화적인 에러 메시지
- 적절한 로딩 상태 표시
- 네트워크 오류 처리

## 확장성

### 새로운 화면 추가
1. `screen/` 디렉토리에 새 화면 컴포넌트 생성
2. `viewmodel/` 디렉토리에 해당 ViewModel 생성
3. 필요한 UI 컴포넌트를 `component/`에 추가

### 새로운 기능 추가
1. Domain Layer에 UseCase 추가
2. Presentation Layer에 UI 컴포넌트 추가
3. ViewModel에서 상태 관리 로직 추가

## 주의사항

1. **메모리 누수 방지**: DisposableEffect를 사용한 리소스 정리
2. **권한 처리**: 마이크 권한 등 필요한 권한 요청
3. **생명주기 관리**: Activity/Fragment 생명주기에 맞춘 리소스 관리
4. **백그라운드 처리**: 포그라운드 서비스 사용 시 적절한 알림 표시 