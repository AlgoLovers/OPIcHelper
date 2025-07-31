# OPicHelper 아키텍처 다이어그램

## 🏗️ **고수준 클래스 다이어그램**

```mermaid
classDiagram
    %% Presentation Layer
    class MainActivity
    class MainViewModel
    class TtsViewModel
    class MemorizationViewModel
    
    %% Domain Layer - Interfaces
    class TtsController
    class ButtonEventHandler
    class AppStateManager
    class QaDataManager
    class AudioRecorder
    class AudioPlayer
    
    %% Data Layer - Implementations
    class TtsControllerImpl
    class TtsOrchestrator
    class GoogleTtsPlayer
    class SamsungTtsPlayer
    class AudioRecorderImpl
    class AudioPlayerImpl
    class QaDataLoaderImpl
    
    %% UI Components
    class MainScreen
    class QuestionCard
    class AnswerCard
    class SmartButton
    
    %% Relationships
    MainActivity --> MainViewModel
    MainActivity --> TtsViewModel
    MainActivity --> MemorizationViewModel
    
    MainViewModel --> ButtonEventHandler
    MainViewModel --> AppStateManager
    MainViewModel --> QaDataManager
    
    TtsViewModel --> TtsController
    TtsViewModel --> AppStateManager
    
    ButtonEventHandler --> TtsController
    ButtonEventHandler --> AppStateManager
    
    TtsControllerImpl ..|> TtsController
    TtsControllerImpl --> TtsOrchestrator
    TtsControllerImpl --> AppStateManager
    
    TtsOrchestrator --> GoogleTtsPlayer
    TtsOrchestrator --> SamsungTtsPlayer
    
    MainScreen --> MainViewModel
    MainScreen --> TtsViewModel
    QuestionCard --> TtsViewModel
    AnswerCard --> TtsViewModel
    SmartButton --> MainViewModel
```

## 🔄 **상태 관리 플로우 시퀀스 다이어그램**

```mermaid
sequenceDiagram
    participant UI as MainScreen
    participant VM as TtsViewModel
    participant TC as TtsController
    participant ASM as AppStateManager
    participant TO as TtsOrchestrator
    participant TTS as TtsPlayer
    
    UI->>VM: playQuestion(question)
    VM->>TC: playQuestion(question)
    TC->>ASM: updateTtsPlayingState(isQuestionPlaying=true)
    TC->>TO: speakWithHighlight(question, callback)
    TO->>TTS: speak(text)
    TTS-->>TO: onProgress(index)
    TO->>ASM: updateHighlightState(questionHighlightIndex=index)
    ASM-->>VM: state updated
    VM-->>UI: questionHighlightIndex updated
    UI->>UI: re-render with highlight
    
    Note over TTS: TTS finishes
    TO->>ASM: updateTtsPlayingState(isQuestionPlaying=false)
    ASM-->>VM: state updated
    VM-->>UI: isQuestionPlaying=false
```

## ⚠️ **현재 문제점 분석**

### **1. 상태 관리 분산**
- `TtsViewModel`에서 직접 `TtsOrchestrator` 사용 ❌
- `ButtonStateManager`에서 별도 상태 관리 ❌
- `TtsControllerImpl`에서 중복 상태 관리 ❌

### **2. 의존성 역전 원칙 위배**
- Presentation Layer가 Data Layer에 직접 의존 ❌
- Domain Layer 인터페이스 미사용 ❌

### **3. 테스트 어려움**
- 구체 클래스에 의존하여 Mock 테스트 어려움 ❌
- 상태 관리 복잡성으로 인한 테스트 복잡성 ❌

## ✅ **개선된 아키텍처 제안**

### **1. 단일 진실 소스 (Single Source of Truth)**
```mermaid
graph TB
    subgraph "AppStateManager"
        AS[AppState]
        AS --> BS[ButtonStates]
        AS --> TS[TtsStates]
        AS --> HS[HighlightStates]
        AS --> MS[MemorizationStates]
    end
    
    subgraph "Observers"
        VM[ViewModels]
        TC[TtsController]
        BE[ButtonEventHandler]
    end
    
    VM --> AS
    TC --> AS
    BE --> AS
```

### **2. 의존성 역전 원칙 준수**
```mermaid
graph TB
    subgraph "Presentation Layer"
        VM[ViewModels]
    end
    
    subgraph "Domain Layer"
        TC[TtsController Interface]
        BE[ButtonEventHandler]
    end
    
    subgraph "Data Layer"
        TCI[TtsControllerImpl]
        TO[TtsOrchestrator]
    end
    
    VM --> TC
    BE --> TC
    TCI ..|> TC
    TCI --> TO
```

## 🚀 **리팩토링 로드맵**

### **Phase 1: 상태 관리 통합** ✅
- [x] `AppStateManager`를 단일 진실 소스로 설정
- [x] 모든 ViewModel이 `AppStateManager` 관찰
- [x] `TtsController`를 통한 TTS 제어

### **Phase 2: 의존성 정리**
- [x] `TtsViewModel`이 `TtsController` 인터페이스 사용
- [x] `ButtonEventHandler`가 `TtsController` 사용
- [ ] Use Case 패턴 도입

### **Phase 3: 테스트 개선**
- [ ] Mock 기반 단위 테스트 작성
- [ ] 상태 관리 테스트 작성
- [ ] 통합 테스트 작성

### **Phase 4: 성능 최적화**
- [ ] 불필요한 상태 업데이트 제거
- [ ] 메모리 누수 방지
- [ ] 백그라운드 처리 최적화 