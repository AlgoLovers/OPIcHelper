# Data Layer (데이터 레이어)

## 개요

Data Layer는 데이터 관리와 외부 시스템과의 통신을 담당하는 레이어입니다. Domain Layer에서 정의한 인터페이스를 구현하며, 실제 데이터 저장소와의 상호작용을 담당합니다.

## 구조

```
data/
├── dao/                   # 데이터 액세스 객체
│   ├── QuestionDao.kt
│   └── StudySessionDao.kt
├── database/              # Room 데이터베이스
│   ├── AppDatabase.kt
│   └── DatabaseModule.kt
├── datasource/            # 데이터 소스 구현
│   └── impl/
│       ├── LocalDataSource.kt
│       └── RemoteDataSource.kt
├── model/                 # 데이터 모델
│   ├── QuestionEntity.kt
│   └── StudySessionEntity.kt
├── repository/            # 리포지토리 구현
│   ├── QuestionRepositoryImpl.kt
│   └── StudySessionRepositoryImpl.kt
└── util/                  # 데이터 유틸리티
    ├── JsonParser.kt
    └── AssetLoader.kt
```

## 주요 컴포넌트

### 1. Repository Implementation (리포지토리 구현)

Domain Layer에서 정의한 인터페이스를 실제로 구현하는 클래스들입니다.

#### QuestionRepositoryImpl.kt

**위치**: `data/repository/QuestionRepositoryImpl.kt`

**역할**: 질문 데이터 접근을 위한 실제 구현

**구현**:
```kotlin
class QuestionRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : QuestionRepository {
    
    override suspend fun getQuestions(category: QuestionCategory? = null): List<Question> {
        return try {
            // 먼저 로컬에서 데이터 확인
            val localQuestions = localDataSource.getQuestions(category)
            if (localQuestions.isNotEmpty()) {
                return localQuestions.map { it.toDomain() }
            }
            
            // 로컬에 없으면 원격에서 가져오기
            val remoteQuestions = remoteDataSource.getQuestions(category)
            localDataSource.saveQuestions(remoteQuestions)
            remoteQuestions.map { it.toDomain() }
        } catch (e: Exception) {
            // 에러 발생 시 로컬 데이터 반환
            localDataSource.getQuestions(category).map { it.toDomain() }
        }
    }
    
    override suspend fun getQuestionById(id: String): Question? {
        return localDataSource.getQuestionById(id)?.toDomain()
    }
    
    override suspend fun getRandomQuestion(category: QuestionCategory? = null): Question? {
        val questions = getQuestions(category)
        return if (questions.isNotEmpty()) {
            questions.random()
        } else null
    }
}
```

**특징**:
- **캐싱 전략**: 로컬 → 원격 → 로컬 순서로 데이터 접근
- **에러 처리**: 네트워크 오류 시 로컬 데이터 사용
- **데이터 변환**: Entity ↔ Domain 모델 간 변환

#### StudySessionRepositoryImpl.kt

**위치**: `data/repository/StudySessionRepositoryImpl.kt`

**역할**: 학습 세션 데이터 접근을 위한 실제 구현

**구현**:
```kotlin
class StudySessionRepositoryImpl(
    private val localDataSource: LocalDataSource
) : StudySessionRepository {
    
    override suspend fun saveStudySession(session: StudySession) {
        localDataSource.saveStudySession(session.toEntity())
    }
    
    override suspend fun getStudySessions(): List<StudySession> {
        return localDataSource.getStudySessions().map { it.toDomain() }
    }
    
    override suspend fun getStudySessionById(id: String): StudySession? {
        return localDataSource.getStudySessionById(id)?.toDomain()
    }
    
    override suspend fun updateStudySession(session: StudySession) {
        localDataSource.updateStudySession(session.toEntity())
    }
    
    override suspend fun deleteStudySession(id: String) {
        localDataSource.deleteStudySession(id)
    }
}
```

### 2. DataSource (데이터 소스)

실제 데이터 저장소와의 상호작용을 담당하는 클래스들입니다.

#### LocalDataSource.kt

**위치**: `data/datasource/impl/LocalDataSource.kt`

**역할**: 로컬 데이터베이스와의 상호작용

**구현**:
```kotlin
class LocalDataSource(
    private val questionDao: QuestionDao,
    private val studySessionDao: StudySessionDao
) {
    
    suspend fun getQuestions(category: QuestionCategory? = null): List<QuestionEntity> {
        return if (category != null) {
            questionDao.getQuestionsByCategory(category)
        } else {
            questionDao.getAllQuestions()
        }
    }
    
    suspend fun saveQuestions(questions: List<QuestionEntity>) {
        questionDao.insertQuestions(questions)
    }
    
    suspend fun getQuestionById(id: String): QuestionEntity? {
        return questionDao.getQuestionById(id)
    }
    
    suspend fun saveStudySession(session: StudySessionEntity) {
        studySessionDao.insertStudySession(session)
    }
    
    suspend fun getStudySessions(): List<StudySessionEntity> {
        return studySessionDao.getAllStudySessions()
    }
}
```

#### RemoteDataSource.kt

**위치**: `data/datasource/impl/RemoteDataSource.kt`

**역할**: 원격 서버와의 상호작용 (현재는 JSON Assets 사용)

**구현**:
```kotlin
class RemoteDataSource(
    private val assetLoader: AssetLoader,
    private val jsonParser: JsonParser
) {
    
    suspend fun getQuestions(category: QuestionCategory? = null): List<QuestionEntity> {
        val fileName = when (category) {
            QuestionCategory.HOBBY -> "qa_hobby.json"
            QuestionCategory.FAMILY -> "qa_family.json"
            // ... 기타 카테고리
            else -> "qa_all.json"
        }
        
        val jsonString = assetLoader.loadAsset(fileName)
        return jsonParser.parseQuestions(jsonString)
    }
}
```

### 3. DAO (Data Access Object)

Room 데이터베이스와의 직접적인 상호작용을 담당하는 인터페이스입니다.

#### QuestionDao.kt

**위치**: `data/dao/QuestionDao.kt`

**역할**: 질문 데이터베이스 접근

**구현**:
```kotlin
@Dao
interface QuestionDao {
    
    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<QuestionEntity>
    
    @Query("SELECT * FROM questions WHERE category = :category")
    suspend fun getQuestionsByCategory(category: QuestionCategory): List<QuestionEntity>
    
    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: String): QuestionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)
    
    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)
    
    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()
}
```

#### StudySessionDao.kt

**위치**: `data/dao/StudySessionDao.kt`

**역할**: 학습 세션 데이터베이스 접근

**구현**:
```kotlin
@Dao
interface StudySessionDao {
    
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    suspend fun getAllStudySessions(): List<StudySessionEntity>
    
    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getStudySessionById(id: String): StudySessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity)
    
    @Update
    suspend fun updateStudySession(session: StudySessionEntity)
    
    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteStudySession(id: String)
}
```

### 4. Database (데이터베이스)

Room 데이터베이스 설정과 엔티티 정의를 담당합니다.

#### AppDatabase.kt

**위치**: `data/database/AppDatabase.kt`

**역할**: Room 데이터베이스 설정

**구현**:
```kotlin
@Database(
    entities = [
        QuestionEntity::class,
        StudySessionEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun questionDao(): QuestionDao
    abstract fun studySessionDao(): StudySessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opic_helper_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 5. Model (데이터 모델)

데이터베이스에 저장되는 엔티티와 Domain 모델 간의 매핑을 담당합니다.

#### QuestionEntity.kt

**위치**: `data/model/QuestionEntity.kt`

**역할**: 데이터베이스에 저장되는 질문 엔티티

**구현**:
```kotlin
@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val questionEn: String,
    val questionKo: String,
    val answerEn: String,
    val answerKo: String,
    val category: QuestionCategory,
    val difficulty: QuestionDifficulty?
) {
    fun toDomain(): Question {
        return Question(
            id = id,
            questionEn = questionEn,
            questionKo = questionKo,
            answerEn = answerEn,
            answerKo = answerKo,
            category = category,
            difficulty = difficulty
        )
    }
}

fun Question.toEntity(): QuestionEntity {
    return QuestionEntity(
        id = id,
        questionEn = questionEn,
        questionKo = questionKo,
        answerEn = answerEn,
        answerKo = answerKo,
        category = category,
        difficulty = difficulty
    )
}
```

#### StudySessionEntity.kt

**위치**: `data/model/StudySessionEntity.kt`

**역할**: 데이터베이스에 저장되는 학습 세션 엔티티

**구현**:
```kotlin
@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long?,
    val category: QuestionCategory,
    val questionsAnswered: Int,
    val totalQuestions: Int
) {
    fun toDomain(): StudySession {
        return StudySession(
            id = id,
            startTime = startTime,
            endTime = endTime,
            category = category,
            questionsAnswered = questionsAnswered,
            totalQuestions = totalQuestions
        )
    }
}

fun StudySession.toEntity(): StudySessionEntity {
    return StudySessionEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        category = category,
        questionsAnswered = questionsAnswered,
        totalQuestions = totalQuestions
    )
}
```

### 6. Utility (유틸리티)

데이터 처리에 필요한 유틸리티 클래스들입니다.

#### AssetLoader.kt

**위치**: `data/util/AssetLoader.kt`

**역할**: Assets 폴더의 파일 로딩

**구현**:
```kotlin
class AssetLoader(private val context: Context) {
    
    fun loadAsset(fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw IOException("Failed to load asset: $fileName", e)
        }
    }
    
    fun listAssets(): List<String> {
        return context.assets.list("")?.toList() ?: emptyList()
    }
}
```

#### JsonParser.kt

**위치**: `data/util/JsonParser.kt`

**역할**: JSON 데이터 파싱

**구현**:
```kotlin
class JsonParser {
    
    fun parseQuestions(jsonString: String): List<QuestionEntity> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val questions = mutableListOf<QuestionEntity>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val question = QuestionEntity(
                    id = jsonObject.getString("id"),
                    questionEn = jsonObject.getString("questionEn"),
                    questionKo = jsonObject.getString("questionKo"),
                    answerEn = jsonObject.getString("answerEn"),
                    answerKo = jsonObject.getString("answerKo"),
                    category = QuestionCategory.valueOf(jsonObject.getString("category")),
                    difficulty = jsonObject.optString("difficulty")?.let { 
                        QuestionDifficulty.valueOf(it) 
                    }
                )
                questions.add(question)
            }
            questions
        } catch (e: Exception) {
            throw IOException("Failed to parse JSON", e)
        }
    }
}
```

## 데이터 흐름

### 1. 데이터 로딩
```
ViewModel → Repository → DataSource → DAO → Database
```

### 2. 데이터 저장
```
ViewModel → Repository → DataSource → DAO → Database
```

### 3. 캐싱 전략
```
1. 로컬 데이터 확인
2. 로컬에 없으면 원격에서 가져오기
3. 원격 데이터를 로컬에 저장
4. 에러 발생 시 로컬 데이터 사용
```

## 테스트 전략

### 단위 테스트
- **Repository**: Mock DataSource를 사용한 테스트
- **DataSource**: Mock DAO를 사용한 테스트
- **Utility**: 순수 함수 테스트

### 계측 테스트
- **Database**: 실제 Room 데이터베이스 테스트
- **AssetLoader**: 실제 Assets 파일 로딩 테스트

## 모범 사례

### 1. 데이터 변환
- Entity ↔ Domain 모델 간 명확한 매핑
- 확장 함수를 사용한 변환
- 일관된 네이밍 컨벤션

### 2. 에러 처리
- 네트워크 오류 시 로컬 데이터 사용
- 적절한 예외 전파
- 사용자 친화적인 에러 메시지

### 3. 캐싱 전략
- 로컬 우선 접근
- 백그라운드 동기화
- 캐시 무효화 정책

## 확장성

### 새로운 데이터 소스 추가
1. 새로운 DataSource 인터페이스 정의
2. 구현체 생성
3. Repository에서 조합

### 새로운 엔티티 추가
1. Entity 클래스 생성
2. DAO 인터페이스 정의
3. Database에 엔티티 추가
4. Repository 구현

## 주의사항

1. **메모리 관리**: 대용량 데이터 처리 시 메모리 사용량 고려
2. **백그라운드 처리**: 데이터베이스 작업은 백그라운드 스레드에서 수행
3. **데이터 일관성**: 트랜잭션을 사용한 데이터 일관성 보장
4. **보안**: 민감한 데이터는 암호화하여 저장 