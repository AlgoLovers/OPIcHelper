# OPicHelper 아키텍처 다이어그램

## 🏗️ 현재 아키텍처 분석

### 1. 하이레벨 클래스 다이어그램

```mermaid
graph TB
    %% Presentation Layer
    subgraph "Presentation Layer"
        MainActivity
        MainScreen
        TtsViewModel
        MainViewModel
        MemorizationViewModel
    end
    
    %% Domain Layer
    subgraph "Domain Layer"
        AppStateManager
        ButtonEventHandler
        TtsController
        TtsOrchestrator
        ButtonStateManager
    end
    
    %% Data Layer
    subgraph "Data Layer"
        TtsControllerImpl
        QaDataManager
        UserPreferencesRepository
        AuthRepository
    end
    
    %% Use Cases
    subgraph "Use Cases"
        ExecuteRepeatListeningUseCase
        ExecuteEnglishWritingTestUseCase
        ExecuteFullMemorizationUseCase
    end
    
    %% Dependencies
    MainActivity --> MainViewModel
    MainScreen --> MainViewModel
    MainViewModel --> AppStateManager
    MainViewModel --> ButtonEventHandler
    TtsViewModel --> AppStateManager
    TtsViewModel --> TtsOrchestrator
    
    ButtonEventHandler --> TtsController
    ButtonEventHandler --> ExecuteRepeatListeningUseCase
    ButtonEventHandler --> ExecuteEnglishWritingTestUseCase
    ButtonEventHandler --> ExecuteFullMemorizationUseCase
    
    TtsController --> TtsOrchestrator
    TtsOrchestrator --> TtsControllerImpl
    
    ExecuteRepeatListeningUseCase --> QaDataManager
    ExecuteEnglishWritingTestUseCase --> QaDataManager
    ExecuteFullMemorizationUseCase --> QaDataManager
```

### 2. 상태 관리 플로우

```mermaid
sequenceDiagram
    participant UI as MainScreen
    participant VM as MainViewModel
    participant ASM as AppStateManager
    participant BEH as ButtonEventHandler
    participant TC as TtsController
    participant TO as TtsOrchestrator
    
    UI->>VM: handleQuestionPlayClick()
    VM->>BEH: handleEvent(QuestionPlayClick)
    BEH->>ASM: updateButtonState(QuestionPlay, Loading)
    BEH->>TC: playQuestion(question)
    TC->>ASM: updateTtsPlayingState(isQuestionPlaying=true)
    TC->>TO: speakWithHighlight(question)
    TO-->>TC: onHighlight(index)
    TC->>ASM: updateHighlightState(questionHighlightIndex=index)
    TO-->>TC: completion
    TC->>ASM: updateTtsPlayingState(isQuestionPlaying=false)
    TC->>ASM: updateHighlightState(questionHighlightIndex=null)
    BEH->>ASM: updateButtonState(QuestionPlay, Idle)
    ASM-->>UI: state update
```

## 🚨 현재 문제점 분석

### 1. **상태 관리 분산**
- 여러 ViewModel에서 각각 상태 관리
- TtsController, ButtonStateManager 등에서 중복된 상태 관리
- 동기화 문제 발생 가능

### 2. **책임 분산**
- 하나의 기능이 여러 클래스에 분산
- TTS 관련 로직이 TtsController, TtsOrchestrator, TtsViewModel에 분산

### 3. **의존성 복잡성**
- 순환 의존성 가능성
- 너무 많은 의존성 주입

## 🔧 개선 제안

### 1. **상태 관리 통합**
```
AppStateManager (단일 진실 소스)
├── UI State
├── TTS State  
├── Button State
└── Business State
```

### 2. **계층별 책임 명확화**
```
Presentation Layer: UI 상태 관리
Domain Layer: 비즈니스 로직
Data Layer: 데이터 접근
```

### 3. **Use Case 중심 아키텍처**
```
Use Cases
├── TtsUseCase
├── MemorizationUseCase
└── DataUseCase
```

## 📊 개선된 아키텍처 제안

```mermaid
graph TB
    %% Presentation Layer
    subgraph "Presentation Layer"
        MainActivity
        MainScreen
        MainViewModel
    end
    
    %% Domain Layer
    subgraph "Domain Layer"
        AppStateManager
        UseCaseOrchestrator
    end
    
    %% Use Cases
    subgraph "Use Cases"
        TtsUseCase
        MemorizationUseCase
        DataUseCase
    end
    
    %% Data Layer
    subgraph "Data Layer"
        RepositoryImpl
        LocalDataSource
        RemoteDataSource
    end
    
    %% Dependencies
    MainActivity --> MainViewModel
    MainScreen --> MainViewModel
    MainViewModel --> AppStateManager
    MainViewModel --> UseCaseOrchestrator
    
    UseCaseOrchestrator --> TtsUseCase
    UseCaseOrchestrator --> MemorizationUseCase
    UseCaseOrchestrator --> DataUseCase
    
    TtsUseCase --> RepositoryImpl
    MemorizationUseCase --> RepositoryImpl
    DataUseCase --> RepositoryImpl
    
    RepositoryImpl --> LocalDataSource
    RepositoryImpl --> RemoteDataSource
```

## 🎯 리팩토링 로드맵

### Phase 1: 상태 관리 통합 ✅ (완료)
- AppStateManager를 단일 진실 소스로 통합
- 중복된 상태 관리 제거

### Phase 2: Use Case 중심 리팩토링
- 비즈니스 로직을 Use Case로 분리
- ViewModel 간소화

### Phase 3: Repository 패턴 개선
- 데이터 접근 계층 정리
- 의존성 주입 단순화

### Phase 4: 테스트 가능성 개선
- 단위 테스트 추가
- 의존성 분리

## 📈 성과 지표

- **코드 복잡도**: 감소
- **테스트 커버리지**: 증가
- **유지보수성**: 향상
- **확장성**: 개선 