# OPicHelper 리팩토링 계획

## 1단계: Domain Layer 정리

### 1.1 Entity 정리 ✅ **완료**
- **목표**: 데이터 모델을 순수하고 불변하게 만들기
- **변경사항**:
  - `Question.kt` → `QaItem.kt` + `QuestionCategory.kt`로 분리
  - `MainScreenState.kt` → `PlaybackState.kt`로 변경 (불변 데이터 클래스)
  - `StudySession.kt`, `StudySessionRepository.kt` 삭제 (사용되지 않음)
- **개선점**: 단일 책임 원칙(SRP) 준수, 불변성 확보

### 1.2 Repository 정리 ✅ **완료**
- **목표**: Repository 인터페이스들을 책임에 맞게 재분류
- **변경사항**:
  - `QaDataRepository` → `QaDataManager` (상태 관리 전담)
  - `ProgressRepository` → `ProgressPersistenceService` (저장/로드 전담)
  - `QuestionRepository` → `QaDataLoader` (데이터 로딩 전담)
  - `AudioFileRepository` → `AudioFileManager` (파일 관리 전담)
- **개선점**: 책임 분리, 명확한 네이밍, DI 모듈 업데이트

### 1.3 UseCase 정리 ✅ **완료**
- **목표**: UseCase들의 책임과 네이밍 정리
- **변경사항**:
  - `MemorizeTestUseCase` 인터페이스 삭제 (불필요)
  - `MemorizeTestState` 삭제 (중복 기능)
  - `SaveUserPreferencesUseCase` → `UserPreferencesService`
  - `FullMemorizationUseCase` → `FullMemorizationService`
  - `EnglishWritingTestUseCase` → `EnglishWritingTestService`
  - `RepeatListeningUseCase` → `RepeatListeningService`
- **개선점**: Service 패턴 적용, 의존성 주입 개선, 중복 제거

## 2단계: Data Layer 정리 🔄 **다음 단계**

### 2.1 Repository 구현체 정리
- **목표**: 구현체들의 책임과 네이밍 정리
- **예상 변경사항**:
  - 구현체 클래스명 정리
  - 메서드 네이밍 개선
  - 불필요한 코드 제거

### 2.2 Audio 관련 클래스 정리
- **목표**: Audio 관련 클래스들의 책임 분리
- **예상 변경사항**:
  - TTS 관련 클래스 정리
  - Audio Recorder/Player 정리
  - 파일 관리 로직 분리

## 3단계: Presentation Layer 정리

### 3.1 ViewModel 정리
- **목표**: ViewModel의 책임과 상태 관리 정리
- **예상 변경사항**:
  - 상태 관리 로직 분리
  - 메서드 네이밍 개선
  - 불필요한 코드 제거

### 3.2 UI 컴포넌트 정리
- **목표**: UI 컴포넌트들의 책임과 네이밍 정리
- **예상 변경사항**:
  - 컴포넌트 분리
  - 네이밍 개선
  - 재사용성 향상

## 진행 상황
- ✅ 1.1 Entity 정리 완료
- ✅ 1.2 Repository 정리 완료
- ✅ 1.3 UseCase 정리 완료
- 🔄 2단계 Data Layer 정리 (다음 단계) 