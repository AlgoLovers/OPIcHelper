# 🔄 OPicHelper 리팩토링 계획

## 📋 개요
OPicHelper 프로젝트의 코드 품질 향상과 유지보수성 개선을 위한 대규모 리팩토링 계획입니다.

## 🎯 목표
1. **단일 책임 원칙 (SRP)** 적용
2. **중복 코드 제거**
3. **명확한 네이밍 컨벤션** 적용
4. **테스트 가능성** 향상
5. **확장성** 개선

## 📊 리팩토링 진행 상황

### ✅ 1단계: 도메인 계층 (Domain Layer) 정리

#### ✅ 1.1 Entity 정리 (완료)
**날짜**: 2024년 12월

**변경 사항**:
- `Question.kt` → `QaItem.kt` + `QuestionCategory.kt`로 분리
- `MainScreenState.kt` → `PlaybackState.kt`로 변경
- `StudySession.kt` 삭제 (사용되지 않음)
- `Result.kt` 유지 (범용성 있음)

**개선된 점**:
- **단일 책임 원칙**: 각 파일이 하나의 명확한 책임만 가짐
- **불변성**: `PlaybackState`를 data class로 변경하여 불변성 보장
- **순수성**: UI 관련 로직을 제거하고 순수한 도메인 상태만 포함
- **중복 제거**: 사용되지 않는 코드 제거

**수정된 파일들**:
- `MainScreen.kt`: `MainScreenState` → `PlaybackState` import 변경
- `CategorySelector.kt`: import 경로 수정
- `NavigationSection.kt`: import 경로 수정
- `EnglishWritingTestUseCaseTest.kt`: 새로운 UseCase 구조에 맞게 테스트 수정

#### 🔄 1.2 Repository 정리 (예정)
**계획**:
- `QaDataRepository.kt` → `QaDataManager.kt` (Repository 패턴이 아닌 Manager 패턴)
- `ProgressRepository.kt` → `ProgressPersistenceService.kt` (실제 저장/로드만 담당)
- `AudioFileRepository.kt` → `AudioFileManager.kt`
- `QuestionRepository.kt` → `QaDataLoader.kt` (데이터 로딩만 담당)

#### 🔄 1.3 UseCase 정리 (예정)
**계획**:
- `MemorizeTestProgressTracker.kt` → `MemorizeTestStateManager.kt`
- `MemorizeTestState.kt` → 삭제 (중복 책임)
- `FullMemorizationUseCase.kt` → `FullMemorizationService.kt`
- `EnglishWritingTestUseCase.kt` → `EnglishWritingTestService.kt`
- `RepeatListeningUseCase.kt` → `RepeatListeningService.kt`

### 🔄 2단계: 데이터 계층 (Data Layer) 정리 (예정)

#### 🔄 2.1 Audio 관련
**계획**:
- `AudioRecorderImpl.kt` → `AndroidAudioRecorder.kt`
- `AudioPlayerImpl.kt` → `AndroidAudioPlayer.kt`
- `GoogleTtsPlayer.kt` → `GoogleTextToSpeechPlayer.kt`
- `SamsungTtsPlayer.kt` → `SamsungTextToSpeechPlayer.kt`
- `NaverTtsPlayer.kt` → `NaverTextToSpeechPlayer.kt`
- `KakaoTtsPlayer.kt` → `KakaoTextToSpeechPlayer.kt`

#### 🔄 2.2 Repository 구현체
**계획**:
- `QuestionRepositoryImpl.kt` → `JsonQaDataLoader.kt`
- `AudioFileRepositoryImpl.kt` → `FileSystemAudioManager.kt`

### 🔄 3단계: 프레젠테이션 계층 (Presentation Layer) 정리 (예정)

#### 🔄 3.1 ViewModel
**계획**:
- `MainViewModel.kt` → `MainScreenViewModel.kt`

#### 🔄 3.2 UI Components
**계획**:
- `FlipCard.kt` → `QaCard.kt`
- `HighlightText.kt` → `AnimatedText.kt`

### 🔄 4단계: 중복 책임 제거 (예정)

#### 🔄 4.1 TTS 관련
**계획**:
- `TtsOrchestrator.kt` + `TtsPlaybackController.kt` → `TtsService.kt` (통합)
- 여러 TTS 플레이어들 → `TtsPlayerFactory.kt` (팩토리 패턴)

#### 🔄 4.2 상태 관리
**계획**:
- `MemorizeTestState.kt` + `MemorizeTestProgressTracker.kt` → `MemorizeTestStateManager.kt` (통합)

#### 🔄 4.3 데이터 로딩
**계획**:
- `QaDataRepository.kt` + `QuestionRepository.kt` → `QaDataService.kt` (통합)

### 🔄 5단계: 파일 구조 정리 (예정)

**계획**:
```
domain/
├── entity/          # 순수 데이터 모델
├── repository/      # 인터페이스만
├── service/         # 비즈니스 로직
└── manager/         # 상태 관리

data/
├── audio/           # 오디오 구현체
├── repository/      # Repository 구현체
└── service/         # 데이터 서비스 구현체

presentation/
├── viewmodel/       # ViewModel
├── ui/
│   ├── component/   # 재사용 가능한 컴포넌트
│   └── screen/      # 화면별 컴포넌트
```

### 🔄 6단계: 네이밍 컨벤션 통일 (예정)

**계획**:
- **Manager**: 상태 관리, 전역 상태
- **Service**: 비즈니스 로직, 외부 서비스 연동
- **Repository**: 데이터 접근 인터페이스
- **Controller**: UI 제어
- **Player**: 미디어 재생
- **Loader**: 데이터 로딩

## 📈 품질 지표

### 현재 상태
- **단일 책임 원칙**: 30% 적용 (Entity 계층 완료)
- **중복 코드**: 20% 제거
- **테스트 커버리지**: 유지
- **빌드 성공률**: 100%

### 목표
- **단일 책임 원칙**: 90% 적용
- **중복 코드**: 80% 제거
- **테스트 커버리지**: 향상
- **빌드 성공률**: 100% 유지

## 🚀 다음 단계

1. **1.2 Repository 정리** 진행
2. **1.3 UseCase 정리** 진행
3. **2단계 Data Layer 정리** 시작
4. **테스트 코드 업데이트** 지속

## 📝 참고 사항

- 각 단계 완료 후 GitHub에 커밋 및 푸시
- 빌드 성공 확인 필수
- 테스트 코드도 함께 업데이트
- 문서화 지속 업데이트 