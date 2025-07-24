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

## 2단계: Data Layer 정리 ✅ **완료**

### 2.1 Repository 구현체 정리
- `QuestionRepositoryImpl` → `QaDataLoaderImpl` (네이밍 일치)
- `AudioFileRepositoryImpl` → `AudioFileManagerImpl` (네이밍 일치)
- DI 모듈(AppModule)에서 구현체 주입 코드 변경
- 기존 구현체 파일 삭제

### 2.2 Audio 관련 클래스 정리
- `BaseTtsPlayer` 생성: TTS 공통 로직 추출 및 상속 구조로 개선
- `GoogleTtsPlayer`, `SamsungTtsPlayer` 등은 BaseTtsPlayer 상속으로 단순화
- 중복 코드 제거 및 네이밍 일관성 확보

### 2.3 테스트 코드 및 빌드 오류 수정
- AudioFileManager 인터페이스 변경에 맞춰 테스트 코드 전체 수정
- 불필요한 중복 메서드 제거 및 시그니처 통일
- Lint/빌드 오류 수정

## 3단계: Presentation Layer 정리 🔄 **다음 단계**

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
- ✅ 2단계 Data Layer 정리 완료
- 🔄 3단계 Presentation Layer 정리 (다음 단계) 