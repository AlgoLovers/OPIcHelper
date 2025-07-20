# OPIC Helper

OPIC 학습을 위한 Android 애플리케이션입니다. Clean Architecture와 MVVM 패턴을 기반으로 Jetpack Compose를 사용하여 개발되었습니다.

## 📱 주요 기능

- **OPIC 질문 연습**: 다양한 카테고리의 OPIC 질문을 연습할 수 있습니다
- **TTS(Text-to-Speech)**: 영어 질문과 답변을 음성으로 들을 수 있습니다
- **음성 인식**: 사용자의 음성 답변을 텍스트로 변환합니다
- **하이라이트 기능**: TTS 재생 시 현재 읽고 있는 문장을 시각적으로 표시합니다
- **카테고리별 분류**: 주제별로 질문을 분류하여 체계적인 학습이 가능합니다
- **학습 세션 기록**: 학습 진행 상황을 추적하고 관리합니다

## 🏗️ 아키텍처 개요

### Clean Architecture + MVVM 패턴

이 프로젝트는 **Clean Architecture**와 **MVVM(Model-View-ViewModel)** 패턴을 결합하여 설계되었습니다.

#### 왜 이런 구조를 선택했나요?

1. **관심사의 분리**: 각 레이어가 명확한 책임을 가져 유지보수가 용이합니다
2. **테스트 용이성**: 각 레이어를 독립적으로 테스트할 수 있습니다
3. **확장성**: 새로운 기능 추가 시 기존 코드에 영향을 최소화할 수 있습니다
4. **의존성 역전**: 고수준 모듈이 저수준 모듈에 의존하지 않습니다

#### 레이어 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                      │
│  (UI, ViewModel, User Interface Components)               │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                           │
│  (Entities, Use Cases, Repository Interfaces)             │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                            │
│  (Repository Implementation, Data Sources, Database)      │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ 기술 스택

### 핵심 기술
- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **아키텍처 패턴**: Clean Architecture + MVVM
- **비동기 처리**: Kotlin Coroutines
- **의존성 주입**: Hilt (향후 추가 예정)

### 데이터 관리
- **로컬 데이터베이스**: Room (향후 추가 예정)
- **네트워킹**: Retrofit (향후 추가 예정)
- **데이터 저장**: JSON Assets (현재)

### 오디오 처리
- **TTS**: Android TextToSpeech API
- **음성 인식**: Android SpeechRecognizer API
- **오디오 포커스**: AudioManager

## 📁 프로젝트 구조

```
app/src/main/java/com/na982/opichelper/
├── data/                          # 데이터 레이어
│   ├── dao/                       # 데이터 액세스 객체
│   ├── database/                  # Room 데이터베이스
│   ├── datasource/                # 데이터 소스 구현
│   │   └── impl/
│   ├── model/                     # 데이터 모델
│   ├── repository/                # 리포지토리 구현
│   └── util/                      # 데이터 유틸리티
├── domain/                        # 도메인 레이어
│   ├── entity/                    # 도메인 엔티티
│   │   ├── Question.kt           # 질문 엔티티
│   │   └── StudySession.kt       # 학습 세션 엔티티
│   ├── repository/                # 리포지토리 인터페이스
│   └── usecase/                   # 유스케이스
├── presentation/                  # 프레젠테이션 레이어
│   ├── ui/                       # UI 컴포넌트
│   │   ├── component/            # 재사용 가능한 UI 컴포넌트
│   │   │   ├── FlipCard.kt      # 카드 뒤집기 애니메이션
│   │   │   ├── TtsService.kt    # TTS 서비스
│   │   │   └── SpeechRecognizerHelper.kt # 음성 인식 헬퍼
│   │   ├── screen/              # 화면 컴포넌트
│   │   │   └── MainScreen.kt    # 메인 화면
│   │   └── theme/               # 테마 및 스타일
│   └── viewmodel/               # ViewModel
│       └── MainViewModel.kt     # 메인 ViewModel
├── di/                          # 의존성 주입
└── MainActivity.kt              # 메인 액티비티
```

## 🎯 각 레이어의 책임

### Presentation Layer (프레젠테이션 레이어)
**목적**: 사용자와의 상호작용을 담당하는 레이어

#### 주요 컴포넌트:
- **ViewModel**: UI 상태 관리 및 비즈니스 로직 처리
- **UI Components**: 재사용 가능한 UI 컴포넌트
- **Screen**: 전체 화면을 구성하는 컴포넌트

#### 책임:
- 사용자 입력 처리
- UI 상태 관리
- ViewModel과 UI 간의 데이터 바인딩
- 사용자 인터페이스 표시

### Domain Layer (도메인 레이어)
**목적**: 비즈니스 로직과 규칙을 담당하는 핵심 레이어

#### 주요 컴포넌트:
- **Entity**: 비즈니스 객체 (Question, StudySession 등)
- **UseCase**: 특정 비즈니스 기능을 수행하는 클래스
- **Repository Interface**: 데이터 접근을 위한 인터페이스

#### 책임:
- 비즈니스 규칙 정의
- 도메인 로직 구현
- 다른 레이어에 대한 의존성 없음

### Data Layer (데이터 레이어)
**목적**: 데이터 관리와 외부 시스템과의 통신을 담당

#### 주요 컴포넌트:
- **Repository Implementation**: 리포지토리 인터페이스의 구현
- **DataSource**: 데이터 소스 (로컬, 원격)
- **DAO**: 데이터베이스 접근 객체
- **Model**: 데이터 전송 객체

#### 책임:
- 데이터 저장 및 검색
- 외부 API 통신
- 데이터 변환 및 매핑

## 🔧 개발 환경

### 필수 요구사항
- **Android Studio**: Hedgehog | 2023.1.1 이상
- **Kotlin**: 1.9.22
- **JDK**: 11 이상
- **Android SDK**: API 24 (Android 7.0) 이상

### 권장 사항
- **RAM**: 8GB 이상
- **저장공간**: 10GB 이상의 여유 공간
- **에뮬레이터**: API 30 이상 권장

## 🚀 빌드 및 실행

### 1. 프로젝트 클론
```bash
git clone https://github.com/your-username/OPicHelper.git
cd OPicHelper
```

### 2. Android Studio에서 열기
- Android Studio를 실행합니다
- "Open an existing Android Studio project"를 선택합니다
- 프로젝트 폴더를 선택합니다

### 3. Gradle 동기화
- 프로젝트가 열리면 자동으로 Gradle 동기화가 시작됩니다
- 수동으로 동기화하려면: `File > Sync Project with Gradle Files`

### 4. 앱 실행
- 실제 기기 또는 에뮬레이터를 연결합니다
- `Run` 버튼을 클릭하거나 `Shift + F10`을 누릅니다

## 🧪 테스트

### 단위 테스트 실행
```bash
./gradlew testDebugUnitTest
```

### 계측 테스트 실행
```bash
./gradlew connectedDebugAndroidTest
```

### 전체 테스트 실행
```bash
./gradlew test
```

## 📚 주요 컴포넌트 상세 설명

### 1. MainActivity
**위치**: `app/src/main/java/com/na982/opichelper/MainActivity.kt`
**역할**: 앱의 진입점, Compose UI 설정

**주요 기능**:
- Compose UI 설정
- 테마 적용
- 앱 초기화

### 2. MainViewModel
**위치**: `app/src/main/java/com/na982/opichelper/presentation/viewmodel/MainViewModel.kt`
**역할**: 메인 화면의 상태 관리 및 비즈니스 로직 처리

**주요 기능**:
- 질문 데이터 로딩 및 관리
- 현재 질문 인덱스 관리
- 카테고리 변경 처리
- UI 상태 관리 (로딩, 에러, 성공)

### 3. MainScreen
**위치**: `app/src/main/java/com/na982/opichelper/presentation/ui/screen/MainScreen.kt`
**역할**: 메인 화면의 UI 구성

**주요 기능**:
- 질문/답변 카드 표시
- TTS 재생 버튼
- 음성 인식 버튼
- 카테고리 선택
- 하이라이트 표시

### 4. TtsService
**위치**: `app/src/main/java/com/na982/opichelper/presentation/ui/component/TtsService.kt`
**역할**: Text-to-Speech 기능 제공

**주요 기능**:
- 영어 텍스트를 음성으로 변환
- 문장별 하이라이트 콜백
- 오디오 포커스 관리
- 포그라운드 서비스 실행

### 5. SpeechRecognizerHelper
**위치**: `app/src/main/java/com/na982/opichelper/presentation/ui/component/SpeechRecognizerHelper.kt`
**역할**: 음성 인식 기능 제공

**주요 기능**:
- 실시간 음성 인식
- 부분 결과 및 최종 결과 처리
- 에러 처리
- RecognitionCallback 인터페이스를 통한 결과 전달

### 6. FlipCard
**위치**: `app/src/main/java/com/na982/opichelper/presentation/ui/component/FlipCard.kt`
**역할**: 카드 뒤집기 애니메이션 컴포넌트

**주요 기능**:
- 3D 카드 뒤집기 애니메이션
- 질문/답변 전환
- 사용자 터치 인터랙션

## 🔄 데이터 흐름

### 1. 앱 시작 시
```
MainActivity → MainViewModel → Data Layer → JSON Assets → UI 표시
```

### 2. TTS 재생 시
```
UI 버튼 클릭 → MainScreen → TtsService → TextToSpeech API → 하이라이트 콜백 → UI 업데이트
```

### 3. 음성 인식 시
```
UI 버튼 클릭 → MainScreen → SpeechRecognizerHelper → SpeechRecognizer API → RecognitionCallback → UI 업데이트
```

## 🎨 UI/UX 특징

### 1. Material Design 3
- 최신 Material Design 가이드라인 적용
- 다크/라이트 테마 지원
- 접근성 고려

### 2. 애니메이션
- 부드러운 카드 뒤집기 애니메이션
- TTS 하이라이트 애니메이션
- 버튼 클릭 피드백

### 3. 사용자 경험
- 직관적인 인터페이스
- 실시간 피드백
- 에러 처리 및 사용자 안내

## 🔧 설정 및 커스터마이징

### TTS 설정
- 언어: 영어 (Locale.US)
- 속도: 0.8f (기본값)
- 음성 품질: 최적화됨

### 음성 인식 설정
- 언어: 한국어
- 실시간 인식
- 부분 결과 제공

## 🐛 알려진 이슈

1. **TTS 초기화 지연**: 첫 실행 시 TTS 초기화에 시간이 걸릴 수 있습니다
2. **음성 인식 권한**: 처음 사용 시 마이크 권한이 필요합니다
3. **배터리 최적화**: 일부 기기에서 백그라운드 제한이 있을 수 있습니다

## 🤝 기여하기

### 개발 환경 설정
1. 프로젝트를 포크합니다
2. 로컬에 클론합니다
3. 개발 브랜치를 생성합니다
4. 변경사항을 커밋합니다
5. Pull Request를 생성합니다

### 코딩 컨벤션
- Kotlin 코딩 컨벤션 준수
- Clean Architecture 원칙 준수
- 단위 테스트 작성
- 의미있는 커밋 메시지

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해주세요.

---

**OPIC Helper** - 더 나은 OPIC 학습을 위한 Android 앱
