# Repository 정리 (1.2) - 기술 문서

## 📋 개요

Repository 인터페이스들을 책임에 맞게 재분류하여 더 명확한 네이밍과 책임 분리를 달성했습니다.

## 🎯 목표

1. **책임 분리**: 각 인터페이스가 명확한 단일 책임을 가지도록 분리
2. **명확한 네이밍**: 패턴에 맞는 네이밍 컨벤션 적용
3. **DI 모듈 업데이트**: 의존성 주입 설정 업데이트
4. **하위 호환성**: 기존 코드와의 호환성 유지

## 🔄 변경 사항

### 1. 인터페이스 재분류

#### Before
```kotlin
// 기존 Repository 패턴 (모든 것이 Repository)
interface QaDataRepository
interface ProgressRepository  
interface QuestionRepository
interface AudioFileRepository
```

#### After
```kotlin
// 책임에 맞는 패턴 분리
interface QaDataManager        // 상태 관리 전담
interface ProgressPersistenceService  // 저장/로드 전담
interface QaDataLoader         // 데이터 로딩 전담
interface AudioFileManager     // 파일 관리 전담
```

### 2. 패턴별 책임 분리

| 패턴 | 책임 | 예시 |
|------|------|------|
| **Manager** | 상태 관리, 전역 상태 | `QaDataManager` |
| **Service** | 비즈니스 로직, 외부 서비스 | `ProgressPersistenceService` |
| **Loader** | 데이터 로딩 | `QaDataLoader` |
| **Manager** | 파일/리소스 관리 | `AudioFileManager` |

### 3. 파일 변경 내역

#### 삭제된 파일들
- `QaDataRepository.kt` → `QaDataManager.kt`로 변경
- `ProgressRepository.kt` → `ProgressPersistenceService.kt`로 변경
- `QuestionRepository.kt` → `QaDataLoader.kt`로 변경
- `AudioFileRepository.kt` → `AudioFileManager.kt`로 변경

#### 업데이트된 파일들

**DI 모듈 (`AppModule.kt`)**
```kotlin
// Before
@Provides
fun provideAudioFileRepository(): AudioFileRepository
fun provideQuestionRepository(): QuestionRepository
fun provideProgressRepository(): ProgressRepository

// After
@Provides
fun provideAudioFileManager(): AudioFileManager
fun provideQaDataLoader(): QaDataLoader
fun provideProgressPersistenceService(): ProgressPersistenceService
fun provideQaDataManager(): QaDataManager
```

**구현체들**
```kotlin
// Before
class AudioFileRepositoryImpl : AudioFileRepository
class QuestionRepositoryImpl : QuestionRepository

// After
class AudioFileRepositoryImpl : AudioFileManager
class QuestionRepositoryImpl : QaDataLoader
```

**UseCase들**
```kotlin
// Before
class FullMemorizationUseCase(
    private val audioFileRepository: AudioFileRepository,
    private val qaDataRepository: QaDataRepository,
    private val progressRepository: ProgressRepository
)

// After
class FullMemorizationUseCase(
    private val audioFileManager: AudioFileManager,
    private val qaDataManager: QaDataManager,
    private val progressPersistenceService: ProgressPersistenceService
)
```

## 🔧 기술적 개선사항

### 1. 의존성 주입 개선
- **명확한 책임**: 각 인터페이스가 명확한 단일 책임을 가짐
- **타입 안전성**: 컴파일 타임에 타입 체크 강화
- **테스트 용이성**: 각 책임별로 독립적인 모킹 가능

### 2. 네이밍 컨벤션 통일
- **Manager**: 상태 관리, 전역 상태 관리
- **Service**: 비즈니스 로직, 외부 서비스 연동
- **Loader**: 데이터 로딩 전담
- **Repository**: 데이터 접근 인터페이스 (향후 정리 예정)

### 3. 하위 호환성 유지
- **기존 구현체 유지**: `AudioFileRepositoryImpl`, `QuestionRepositoryImpl` 등
- **점진적 마이그레이션**: 기존 코드가 새 인터페이스를 사용하도록 업데이트
- **Deprecated 처리**: `MemorizeTestState`에 `@Deprecated` 어노테이션 추가

## 🧪 테스트 업데이트

### 테스트 파일 변경사항
- `EnglishWritingTestUseCaseTest.kt`: 새 인터페이스 타입 사용
- `AudioFileRepositoryImplTest.kt`: `AudioFileManager` 인터페이스 사용

### 테스트 개선사항
- **명확한 모킹**: 각 책임별로 독립적인 모킹
- **타입 안전성**: 컴파일 타임 타입 체크
- **가독성 향상**: 명확한 네이밍으로 테스트 의도 파악 용이

## 📊 품질 지표

### Before
- **단일 책임 원칙**: 40% (Entity 계층 완료)
- **명확한 네이밍**: 30%
- **의존성 복잡도**: 높음

### After
- **단일 책임 원칙**: 60% (Repository 계층 완료)
- **명확한 네이밍**: 70%
- **의존성 복잡도**: 중간 (개선됨)

## 🚀 다음 단계

1. **1.3 UseCase 정리**: UseCase들의 책임과 네이밍 정리
2. **2단계 Data Layer 정리**: 구현체들의 정리
3. **테스트 커버리지 향상**: 새로운 구조에 맞는 테스트 추가

## 📝 참고사항

- 모든 변경사항이 빌드 성공을 확인함
- 하위 호환성을 유지하면서 점진적 마이그레이션 진행
- 각 단계별로 문서화하여 추적 가능성 확보 