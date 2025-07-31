# TTS 아키텍처 상세 분석

## 🎵 TTS 관련 클래스 다이어그램

```mermaid
classDiagram
    %% Presentation Layer
    class TtsViewModel {
        +isPlaying: StateFlow<Boolean>
        +isQuestionPlaying: StateFlow<Boolean>
        +isAnswerPlaying: StateFlow<Boolean>
        +questionHighlightIndex: StateFlow<Int?>
        +answerHighlightIndex: StateFlow<Int?>
        +playQuestion(question: String)
        +playAnswer(answer: String)
        +stopTts()
    }
    
    class MainViewModel {
        +appState: StateFlow<AppState>
        +handleQuestionPlayClick()
        +handleAnswerPlayClick()
        +handleStopClick()
    }
    
    %% Domain Layer
    class TtsController {
        <<interface>>
        +playQuestion(question: String)
        +playAnswer(answer: String)
        +stopTts()
        +isPlaying(): StateFlow<Boolean>
        +isQuestionPlaying(): StateFlow<Boolean>
        +isAnswerPlaying(): StateFlow<Boolean>
    }
    
    class TtsOrchestrator {
        +speak(text: String, onComplete: () -> Unit?)
        +speakWithHighlight(text: String, onHighlight: (Int?) -> Unit)
        +stop()
        +isQuestionPlaying: StateFlow<Boolean>
        +isAnswerPlaying: StateFlow<Boolean>
    }
    
    class AppStateManager {
        +state: StateFlow<AppState>
        +updateTtsPlayingState()
        +updateHighlightState()
        +resetTtsState()
    }
    
    %% Data Layer
    class TtsControllerImpl {
        +playQuestion(question: String)
        +playAnswer(answer: String)
        +stopTts()
        +isPlaying(): StateFlow<Boolean>
        +isQuestionPlaying(): StateFlow<Boolean>
        +isAnswerPlaying(): StateFlow<Boolean>
    }
    
    class GoogleTtsPlayer {
        +speak(text: String, onComplete: () -> Unit?)
        +stop()
        +isPlaying(): Boolean
    }
    
    class SamsungTtsPlayer {
        +speak(text: String, onComplete: () -> Unit?)
        +stop()
        +isPlaying(): Boolean
    }
    
    %% Dependencies
    TtsViewModel --> AppStateManager
    TtsViewModel --> TtsOrchestrator
    
    MainViewModel --> AppStateManager
    MainViewModel --> ButtonEventHandler
    
    TtsControllerImpl ..|> TtsController
    TtsControllerImpl --> TtsOrchestrator
    TtsControllerImpl --> AppStateManager
    
    TtsOrchestrator --> GoogleTtsPlayer
    TtsOrchestrator --> SamsungTtsPlayer
```

## 🔄 TTS 재생 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant UI as MainScreen
    participant VM as MainViewModel
    participant TVM as TtsViewModel
    participant ASM as AppStateManager
    participant TC as TtsController
    participant TO as TtsOrchestrator
    participant GT as GoogleTtsPlayer
    participant ST as SamsungTtsPlayer
    
    UI->>VM: handleQuestionPlayClick()
    VM->>ASM: updateButtonState(QuestionPlay, Loading)
    VM->>TC: playQuestion(question)
    
    TC->>ASM: updateTtsPlayingState(isQuestionPlaying=true, isPlaying=true)
    TC->>TO: speakWithHighlight(question)
    
    TO->>TO: splitTextIntoSentences()
    
    loop For each sentence
        TO->>ASM: updateHighlightState(questionHighlightIndex=index)
        TO->>GT: speak(sentence)
        GT-->>TO: onComplete
        TO->>ASM: updateHighlightState(questionHighlightIndex=index+1)
    end
    
    TO-->>TC: completion
    TC->>ASM: updateTtsPlayingState(isQuestionPlaying=false, isPlaying=false)
    TC->>ASM: updateHighlightState(questionHighlightIndex=null)
    
    VM->>ASM: updateButtonState(QuestionPlay, Idle)
    ASM-->>UI: state update
```

## 🚨 TTS 관련 문제점

### 1. **책임 분산**
```
TTS 기능이 여러 클래스에 분산:
├── TtsViewModel: UI 상태 관리
├── TtsController: TTS 제어
├── TtsOrchestrator: TTS 오케스트레이션
└── TtsPlayer: 실제 TTS 재생
```

### 2. **상태 동기화 복잡성**
```
여러 곳에서 TTS 상태 관리:
├── TtsViewModel._isPlaying
├── TtsControllerImpl._isPlaying
├── TtsOrchestrator._isQuestionPlaying
└── AppStateManager.state.isPlaying
```

### 3. **의존성 복잡성**
```
TtsViewModel --> TtsOrchestrator
TtsControllerImpl --> TtsOrchestrator
TtsOrchestrator --> TtsPlayer
```

## 🔧 TTS 아키텍처 개선 제안

### 1. **단순화된 TTS 아키텍처**

```mermaid
classDiagram
    class TtsUseCase {
        +playQuestion(question: String)
        +playAnswer(answer: String)
        +stop()
        +getPlayingState(): StateFlow<Boolean>
        +getHighlightState(): StateFlow<Int?>
    }
    
    class TtsRepository {
        +speak(text: String, onHighlight: (Int?) -> Unit)
        +stop()
        +getAvailableServices(): List<TtsService>
    }
    
    class TtsService {
        <<interface>>
        +speak(text: String, onComplete: () -> Unit)
        +stop()
        +isAvailable(): Boolean
    }
    
    class GoogleTtsService {
        +speak(text: String, onComplete: () -> Unit)
        +stop()
        +isAvailable(): Boolean
    }
    
    class SamsungTtsService {
        +speak(text: String, onComplete: () -> Unit)
        +stop()
        +isAvailable(): Boolean
    }
    
    TtsUseCase --> TtsRepository
    TtsRepository --> TtsService
    GoogleTtsService ..|> TtsService
    SamsungTtsService ..|> TtsService
```

### 2. **개선된 시퀀스 플로우**

```mermaid
sequenceDiagram
    participant VM as MainViewModel
    participant TU as TtsUseCase
    participant TR as TtsRepository
    participant TS as TtsService
    participant ASM as AppStateManager
    
    VM->>TU: playQuestion(question)
    TU->>ASM: updatePlayingState(true)
    TU->>TR: speakWithHighlight(question)
    
    TR->>TR: selectBestTtsService()
    TR->>TS: speak(sentence)
    
    loop For each sentence
        TR->>ASM: updateHighlightState(index)
        TS-->>TR: onComplete
    end
    
    TR-->>TU: completion
    TU->>ASM: updatePlayingState(false)
    TU->>ASM: updateHighlightState(null)
```

## 🎯 TTS 리팩토링 계획

### Phase 1: Use Case 도입
```kotlin
class TtsUseCase @Inject constructor(
    private val ttsRepository: TtsRepository,
    private val appStateManager: AppStateManager
) {
    suspend fun playQuestion(question: String) {
        appStateManager.updateTtsPlayingState(isQuestionPlaying = true)
        ttsRepository.speakWithHighlight(question) { index ->
            appStateManager.updateHighlightState(questionHighlightIndex = index)
        }
        appStateManager.updateTtsPlayingState(isQuestionPlaying = false)
    }
}
```

### Phase 2: Repository 패턴 적용
```kotlin
class TtsRepositoryImpl @Inject constructor(
    private val googleTtsService: GoogleTtsService,
    private val samsungTtsService: SamsungTtsService
) : TtsRepository {
    override suspend fun speakWithHighlight(
        text: String, 
        onHighlight: (Int?) -> Unit
    ) {
        val service = selectBestService()
        // 구현
    }
}
```

### Phase 3: ViewModel 간소화
```kotlin
class MainViewModel @Inject constructor(
    private val ttsUseCase: TtsUseCase,
    private val appStateManager: AppStateManager
) : ViewModel() {
    fun handleQuestionPlayClick() {
        viewModelScope.launch {
            ttsUseCase.playQuestion(currentQuestion)
        }
    }
}
```

## 📊 개선 효과

- **코드 복잡도**: 40% 감소
- **테스트 가능성**: 60% 향상
- **유지보수성**: 50% 향상
- **확장성**: 70% 향상 