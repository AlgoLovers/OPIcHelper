# Domain Layer (도메인 레이어)

## 개요

Domain Layer는 Clean Architecture의 핵심 레이어로, 비즈니스 로직과 규칙을 담당합니다. 이 레이어는 다른 레이어에 대한 의존성이 없으며, 순수한 Kotlin 코드로 구성됩니다.

## 구조

```
domain/
├── entity/              # 도메인 엔티티
│   ├── Question.kt
│   └── StudySession.kt
├── repository/          # 리포지토리 인터페이스
│   ├── QuestionRepository.kt
│   └── StudySessionRepository.kt
└── usecase/            # 유스케이스
    ├── GetQuestionsUseCase.kt
    └── SaveStudySessionUseCase.kt
```

## 주요 컴포넌트

### 1. Entity (엔티티)

엔티티는 비즈니스 도메인의 핵심 객체입니다. 데이터베이스나 외부 시스템과 무관하게 순수한 비즈니스 규칙을 포함합니다.

#### Question.kt

**위치**: `domain/entity/Question.kt`

**역할**: OPIC 질문을 나타내는 도메인 엔티티

**구조**:
```kotlin
data class Question(
    val id: String,
    val questionEn: String,      // 영어 질문
    val questionKo: String,      // 한국어 질문
    val answerEn: String,        // 영어 답변
    val answerKo: String,        // 한국어 답변
    val category: QuestionCategory,
    val difficulty: QuestionDifficulty
)
```

**비즈니스 규칙**:
- 질문과 답변은 비어있을 수 없음
- 카테고리는 필수 값
- 난이도는 선택적 값

**사용 예시**:
```kotlin
val question = Question(
    id = "q1",
    questionEn = "What do you like to do in your free time?",
    questionKo = "여가 시간에 무엇을 하시나요?",
    answerEn = "I like to read books and watch movies.",
    answerKo = "책을 읽고 영화를 보는 것을 좋아합니다.",
    category = QuestionCategory.HOBBY,
    difficulty = QuestionDifficulty.INTERMEDIATE
)
```

#### StudySession.kt

**위치**: `domain/entity/StudySession.kt`

**역할**: 학습 세션을 나타내는 도메인 엔티티

**구조**:
```kotlin
data class StudySession(
    val id: String,
    val startTime: Long,
    val endTime: Long?,
    val category: QuestionCategory,
    val questionsAnswered: Int,
    val totalQuestions: Int
)
```

**비즈니스 규칙**:
- 시작 시간은 필수
- 종료 시간은 선택적 (진행 중일 수 있음)
- 답변한 질문 수는 0 이상이어야 함

### 2. Repository Interface (리포지토리 인터페이스)

리포지토리 인터페이스는 데이터 접근을 위한 추상화 계층입니다. Domain Layer는 구현체에 대해 알 필요가 없으며, 인터페이스를 통해서만 데이터에 접근합니다.

#### QuestionRepository.kt

**위치**: `domain/repository/QuestionRepository.kt`

**역할**: 질문 데이터 접근을 위한 인터페이스

**메서드**:
```kotlin
interface QuestionRepository {
    suspend fun getQuestions(category: QuestionCategory? = null): List<Question>
    suspend fun getQuestionById(id: String): Question?
    suspend fun getRandomQuestion(category: QuestionCategory? = null): Question?
    suspend fun getQuestionsByDifficulty(difficulty: QuestionDifficulty): List<Question>
}
```

**사용 예시**:
```kotlin
class GetQuestionsUseCase(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(category: QuestionCategory? = null): List<Question> {
        return questionRepository.getQuestions(category)
    }
}
```

#### StudySessionRepository.kt

**위치**: `domain/repository/StudySessionRepository.kt`

**역할**: 학습 세션 데이터 접근을 위한 인터페이스

**메서드**:
```kotlin
interface StudySessionRepository {
    suspend fun saveStudySession(session: StudySession)
    suspend fun getStudySessions(): List<StudySession>
    suspend fun getStudySessionById(id: String): StudySession?
    suspend fun updateStudySession(session: StudySession)
    suspend fun deleteStudySession(id: String)
}
```

### 3. UseCase (유스케이스)

유스케이스는 특정 비즈니스 기능을 수행하는 클래스입니다. 하나의 유스케이스는 하나의 특정 기능을 담당하며, 여러 리포지토리를 조합하여 복잡한 비즈니스 로직을 구현할 수 있습니다.

#### GetQuestionsUseCase.kt

**위치**: `domain/usecase/GetQuestionsUseCase.kt`

**역할**: 질문 목록을 가져오는 비즈니스 로직

**구현**:
```kotlin
class GetQuestionsUseCase(
    private val questionRepository: QuestionRepository
) {
    suspend operator fun invoke(category: QuestionCategory? = null): List<Question> {
        return questionRepository.getQuestions(category)
    }
}
```

**사용 예시**:
```kotlin
// ViewModel에서 사용
class MainViewModel(
    private val getQuestionsUseCase: GetQuestionsUseCase
) : ViewModel() {
    
    fun loadQuestions(category: QuestionCategory? = null) {
        viewModelScope.launch {
            try {
                val questions = getQuestionsUseCase(category)
                _uiState.value = _uiState.value.copy(
                    questions = questions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}
```

#### SaveStudySessionUseCase.kt

**위치**: `domain/usecase/SaveStudySessionUseCase.kt`

**역할**: 학습 세션을 저장하는 비즈니스 로직

**구현**:
```kotlin
class SaveStudySessionUseCase(
    private val studySessionRepository: StudySessionRepository
) {
    suspend operator fun invoke(session: StudySession) {
        // 비즈니스 규칙 검증
        require(session.startTime > 0) { "시작 시간은 필수입니다." }
        require(session.questionsAnswered >= 0) { "답변한 질문 수는 0 이상이어야 합니다." }
        
        studySessionRepository.saveStudySession(session)
    }
}
```

## 비즈니스 규칙

### 1. 질문 관련 규칙
- 질문 ID는 고유해야 함
- 영어 질문과 한국어 질문은 모두 필수
- 카테고리는 미리 정의된 값 중 하나여야 함
- 난이도는 선택적이지만, 설정 시 유효한 값이어야 함

### 2. 학습 세션 관련 규칙
- 세션 ID는 고유해야 함
- 시작 시간은 필수이며, 현재 시간보다 과거여야 함
- 종료 시간이 설정된 경우, 시작 시간보다 이후여야 함
- 답변한 질문 수는 전체 질문 수를 초과할 수 없음

### 3. 데이터 검증 규칙
```kotlin
// 엔티티 생성 시 검증
fun validateQuestion(question: Question): Boolean {
    return question.questionEn.isNotBlank() &&
           question.questionKo.isNotBlank() &&
           question.answerEn.isNotBlank() &&
           question.answerKo.isNotBlank()
}

fun validateStudySession(session: StudySession): Boolean {
    return session.startTime > 0 &&
           (session.endTime == null || session.endTime > session.startTime) &&
           session.questionsAnswered >= 0 &&
           session.questionsAnswered <= session.totalQuestions
}
```

## 의존성 역전 원칙

Domain Layer는 Clean Architecture의 핵심 원칙인 **의존성 역전 원칙**을 따릅니다.

### 원칙
1. **고수준 모듈** (Domain Layer)은 **저수준 모듈** (Data Layer)에 의존하지 않습니다
2. **추상화** (Repository Interface)에 의존하고, **구체화** (Repository Implementation)에 의존하지 않습니다

### 구현 방법
```kotlin
// Domain Layer (고수준 모듈)
interface QuestionRepository {
    suspend fun getQuestions(): List<Question>
}

// Data Layer (저수준 모듈) - Domain Layer에 의존
class QuestionRepositoryImpl : QuestionRepository {
    override suspend fun getQuestions(): List<Question> {
        // 구현
    }
}
```

## 테스트 전략

### 단위 테스트
Domain Layer는 순수한 Kotlin 코드이므로 단위 테스트가 용이합니다.

#### 엔티티 테스트
```kotlin
@Test
fun `Question 생성 시 유효한 데이터 검증`() {
    val question = Question(
        id = "q1",
        questionEn = "What do you like?",
        questionKo = "무엇을 좋아하나요?",
        answerEn = "I like reading.",
        answerKo = "독서를 좋아합니다.",
        category = QuestionCategory.HOBBY
    )
    
    assertTrue(validateQuestion(question))
}
```

#### 유스케이스 테스트
```kotlin
@Test
fun `GetQuestionsUseCase는 repository에서 질문을 가져옴`() = runTest {
    val mockRepository = mock<QuestionRepository>()
    val questions = listOf(Question(...))
    whenever(mockRepository.getQuestions(any())).thenReturn(questions)
    
    val useCase = GetQuestionsUseCase(mockRepository)
    val result = useCase(QuestionCategory.HOBBY)
    
    assertEquals(questions, result)
    verify(mockRepository).getQuestions(QuestionCategory.HOBBY)
}
```

## 확장성

### 새로운 엔티티 추가
1. `entity/` 디렉토리에 새 엔티티 클래스 생성
2. 비즈니스 규칙 정의
3. 필요한 경우 검증 함수 추가

### 새로운 유스케이스 추가
1. `usecase/` 디렉토리에 새 유스케이스 클래스 생성
2. 필요한 리포지토리 인터페이스 정의
3. 비즈니스 로직 구현

### 새로운 리포지토리 인터페이스 추가
1. `repository/` 디렉토리에 새 인터페이스 생성
2. 필요한 메서드 정의
3. Data Layer에서 구현

## 모범 사례

### 1. 엔티티 설계
- 불변 객체로 설계 (data class 사용)
- 비즈니스 규칙을 엔티티 내부에 캡슐화
- 유효성 검증 로직 포함

### 2. 유스케이스 설계
- 단일 책임 원칙 준수
- operator fun invoke() 사용으로 함수형 호출 가능
- 예외 처리 및 비즈니스 규칙 검증

### 3. 리포지토리 인터페이스 설계
- 데이터 접근에 필요한 메서드만 정의
- 구현체에 대한 의존성 없음
- 테스트 용이성을 고려한 설계

## 주의사항

1. **외부 의존성 금지**: Android Framework, 데이터베이스 등에 직접 의존하지 않음
2. **순수 함수**: 외부 상태에 영향을 주지 않는 순수한 함수로 구성
3. **비즈니스 규칙 중심**: 기술적 구현보다는 비즈니스 규칙에 집중
4. **테스트 용이성**: 모든 컴포넌트가 쉽게 테스트 가능하도록 설계 