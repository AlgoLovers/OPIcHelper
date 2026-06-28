# 스크립트 편집 기능 구현 계획

## 목표

사용자가 앱 내에서 질문/답변의 한글·영문 문장을 편집할 수 있도록 한다.
문장 쌍(한국어↔영어) 1:1 매칭을 구조적으로 보장하고, 사용자 실수를 사전에 방지한다.

## 핵심 제약

1. **1:1 매칭**: 반복듣기/영작/통암기 모두 `koSentences[i]` ↔ `enSentences[i]`로 매핑. 쌍이 깨지면 크래시 또는 하이라이트 어긋남
2. **마침표 분할**: `SentenceSplitter.split()`로 문장 분할. 마침표 누락 시 분할 오류
3. **TTS 의존**: 빈 문장 또는 공백만 있는 문장은 TTS 재생 실패
4. **문장 분할 일관성**: 편집 UI와 재생 코드가 동일한 분할 로직을 사용해야 함. `SentenceSplitter`(`domain/audio/`)가 단일 진실 공급원

## 아키텍처

### Clean Architecture 준수

```
Presentation (EditScriptViewModel + BottomSheet)
    ↓ 편집 요청 (Entity 전달)
Domain (ScriptEditRepository 인터페이스 + SentenceSplitter)
    ↓ 의존성 역전
Data (ScriptEditRepositoryImpl → Room DB)
```

- Domain 계층은 Data(Room)를 직접 참조하지 않음
- Repository 인터페이스는 Domain에, 구현체는 Data에 위치
- ViewModel은 Repository 인터페이스만 의존
- **문장 분할 로직은 Domain 계층에 위치** — Presentation과 Data 모두 동일한 로직 사용

### Domain Layer

#### 문장 분할 유틸리티 (기존 — 단일 진실 공급원)

```kotlin
// domain/audio/SentenceSplitter.kt (이미 존재)
object SentenceSplitter {
    private val REGEX = Regex("(?<=[.!?。])\\s*")

    fun split(text: String): List<String> =
        text.split(REGEX).map { it.trim() }.filter { it.isNotEmpty() }

    // 추가 필요: 편집 후 문장을 합치는 메서드
    fun join(sentences: List<String>): String =
        sentences.joinToString(" ")
}
```

**주의**:
- 기존 `SentenceSplitter`는 `domain/audio/`에 위치. 계획서 초안의 `domain/util/`이 아님
- 정규식은 `(?<=[.!?。])\\s*`로 CJK 마침표(`。`) 지원 및 `\\s*`(0개 이상) 사용
- **join()의 공백 삽입 한계**: split()은 `\\s*`로 구분자 뒤 공백을 소비. 재결합 시 `joinToString(" ")`이 공백을 삽입. 원본에 공백이 없었던 한국어 텍스트(예: "안녕하세요.반갑습니다")는 "안녕하세요. 반갑습니다"로 변경됨. 편집 워크플로우에서만 사용되며 원본은 `*Original` 컬럼에 보존되므로 복원 가능

#### Repository 인터페이스

```kotlin
// domain/repository/ScriptEditRepository.kt
interface ScriptEditRepository {
    fun getQaItemsByCategory(category: String, level: String): Flow<List<QaItem>>
    suspend fun updateQaItem(item: QaItem, level: UserLevel, scriptIndex: Int)
    suspend fun restoreOriginal(id: String)
    suspend fun restoreAllOriginal()
    suspend fun isModified(id: String): Boolean
}
```

**수정 내용** (초안 대비):
- `updateQaItem(item: QaItem, level: UserLevel)` — QaItem.answers는 단일 레벨만 포함하므로 level 파라미터 필요
- `level: String` → `level: UserLevel` — 타입 안전성

#### 검증 로직

```kotlin
// domain/usecase/ValidateScriptEditUseCase.kt
data class SentencePair(val korean: String, val english: String)
data class ValidationResult(
    val errors: List<ValidationError>,
    val isValid: Boolean
)
sealed class ValidationError {
    data class MissingPunctuation(val index: Int, val isKorean: Boolean) : ValidationError()
    data class EmptySentence(val index: Int, val isKorean: Boolean) : ValidationError()
}

class ValidateScriptEditUseCase {
    fun validate(pairs: List<SentencePair>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        pairs.forEachIndexed { i, pair ->
            if (pair.korean.isBlank())
                errors.add(ValidationError.EmptySentence(i, isKorean = true))
            if (pair.english.isBlank())
                errors.add(ValidationError.EmptySentence(i, isKorean = false))
            if (pair.korean.isNotBlank() && !pair.korean.trim().endsWithSentenceEnd())
                errors.add(ValidationError.MissingPunctuation(i, isKorean = true))
            if (pair.english.isNotBlank() && !pair.english.trim().endsWithSentenceEnd())
                errors.add(ValidationError.MissingPunctuation(i, isKorean = false))
        }
        return ValidationResult(errors, errors.isEmpty())
    }

    private fun String.endsWithSentenceEnd(): Boolean =
        trimEnd().let { text ->
            text.endsWith(".") || text.endsWith("!") || text.endsWith("?") || text.endsWith("。")
        }
}
```

**수정 내용** (초안 대비):
- `endsWithSentenceEnd()`에 CJK 마침표(`。`) 추가 — SentenceSplitter 정규식과 일치

### Data Layer

#### Room Database

```kotlin
@Entity(tableName = "qa_items")
data class QaItemEntity(
    @PrimaryKey val id: String,           // "{category}_{itemId}_{level}"
    val category: String,
    val itemId: String,                   // JSON 원본 id (빈 값이면 순서로 대체)
    val level: String,                    // AL, IH, IH_RAW, IM
    val questionEn: String,
    val questionKo: String,
    val answerEn: String,
    val answerKo: String,
    val vocabulary: String = "",          // JSON 직렬화 (2차 편집 대비)
    val grammar: String = "",            // JSON 직렬화
    val tips: String = "",                // JSON 직렬화
    val questionEnOriginal: String,       // 원본 백업 (불변)
    val questionKoOriginal: String,       // 원본 백업 (불변)
    val answerEnOriginal: String,         // 원본 백업 (불변)
    val answerKoOriginal: String,         // 원본 백업 (불변)
    val isModified: Boolean = false,
    val updatedAt: Long = 0L
)
```

#### Dao

```kotlin
@Dao
interface QaItemDao {
    @Query("SELECT * FROM qa_items WHERE category = :category AND level = :level ORDER BY CAST(itemId AS INTEGER)")
    fun getByCategoryAndLevel(category: String, level: String): Flow<List<QaItemEntity>>

    @Query("SELECT * FROM qa_items WHERE id = :id")
    suspend fun getById(id: String): QaItemEntity?

    @Update
    suspend fun update(item: QaItemEntity)

    @Query("""
        UPDATE qa_items SET
        questionEn = questionEnOriginal, questionKo = questionKoOriginal,
        answerEn = answerEnOriginal, answerKo = answerKoOriginal,
        isModified = 0, updatedAt = 0
        WHERE id = :id
    """)
    suspend fun restoreOriginal(id: String)

    @Query("""
        UPDATE qa_items SET
        questionEn = questionEnOriginal, questionKo = questionKoOriginal,
        answerEn = answerEnOriginal, answerKo = answerKoOriginal,
        isModified = 0, updatedAt = 0
        WHERE isModified = 1
    """)
    suspend fun restoreAllOriginal()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QaItemEntity>)

    @Query("SELECT COUNT(*) FROM qa_items")
    suspend fun getCount(): Int

    @Query("SELECT DISTINCT level FROM qa_items")
    suspend fun getSeededLevels(): List<String>
}
```

#### AssetSeeder (모든 레벨 시드)

```kotlin
class AssetSeeder(
    private val context: Context,
    private val dao: QaItemDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        const val SEED_VERSION_KEY = "seed_version"
        const val CURRENT_SEED_VERSION = 1
    }

    suspend fun seedIfNeeded() {
        val storedVersion = userPreferencesRepository.getSeedVersion()
        if (storedVersion == CURRENT_SEED_VERSION && dao.getCount() > 0) return

        val loader = LeveledQaDataLoader(context)
        val entities = mutableListOf<QaItemEntity>()

        for (level in UserLevel.entries) {
            val items = loader.loadQaItemsForLevel(level)
            items.forEachIndexed { index, item ->
                val answer = item.answers[level] ?: return@forEachIndexed
                val safeItemId = item.id.ifBlank { index.toString() }
                entities.add(QaItemEntity(
                    id = "${item.category}_${safeItemId}_${level.name}",
                    category = item.category,
                    itemId = safeItemId,
                    level = level.name,
                    questionEn = item.questionEn,
                    questionKo = item.questionKo,
                    answerEn = answer.answerEn,
                    answerKo = answer.answerKo,
                    vocabulary = Gson().toJson(answer.vocabulary),
                    grammar = Gson().toJson(answer.grammar),
                    tips = Gson().toJson(answer.tips),
                    questionEnOriginal = item.questionEn,
                    questionKoOriginal = item.questionKo,
                    answerEnOriginal = answer.answerEn,
                    answerKoOriginal = answer.answerKo
                ))
            }
        }
        dao.insertAll(entities)
        userPreferencesRepository.setSeedVersion(CURRENT_SEED_VERSION)
    }
}
```

**수정 내용** (초안 대비):
- `UserLevel.values()` → `UserLevel.entries` — Kotlin 1.9 권장

#### AppDatabase

```kotlin
@Database(entities = [QaItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qaItemDao(): QaItemDao
}
```

#### ScriptEditRepositoryImpl

```kotlin
class ScriptEditRepositoryImpl(
    private val dao: QaItemDao,
    private val recordingTimeManager: RecordingTimeManager,
    private val progressPersistenceService: ProgressPersistenceService,
    private val userPreferencesRepository: UserPreferencesRepository
) : ScriptEditRepository {

    override fun getQaItemsByCategory(category: String, level: String): Flow<List<QaItem>> =
        dao.getByCategoryAndLevel(category, level).map { entities ->
            entities.map { it.toQaItem() }
        }

    override suspend fun updateQaItem(item: QaItem, level: UserLevel, scriptIndex: Int) {
        val id = generateId(item, level)
        val entity = dao.getById(id) ?: return
        val sentenceCountChanged = hasSentenceCountChanged(entity, item, level)

        dao.update(entity.copy(
            questionEn = item.questionEn,
            questionKo = item.questionKo,
            answerEn = item.answers[level]?.answerEn ?: entity.answerEn,
            answerKo = item.answers[level]?.answerKo ?: entity.answerKo,
            isModified = true,
            updatedAt = System.currentTimeMillis()
        ))

        if (sentenceCountChanged) {
            recordingTimeManager.clearRecordingTimes(item.category, scriptIndex)
            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(item.category, scriptIndex, 0)
            )
            for (memLevel in MemorizeLevel.entries) {
                progressPersistenceService.clearCategoryProgress(item.category, scriptIndex, memLevel.displayName)
            }
        }
    }

    override suspend fun restoreOriginal(id: String) = dao.restoreOriginal(id)
    override suspend fun restoreAllOriginal() = dao.restoreAllOriginal()
    override suspend fun isModified(id: String): Boolean = dao.getById(id)?.isModified ?: false

    private fun generateId(item: QaItem, level: UserLevel): String =
        "${item.category}_${item.id}_${level.name}"

    private fun hasSentenceCountChanged(entity: QaItemEntity, item: QaItem, level: UserLevel): Boolean {
        val oldCount = SentenceSplitter.split(entity.answerEn).size
        val newCount = SentenceSplitter.split(item.answers[level]?.answerEn ?: "").size
        return oldCount != newCount
    }
}
```

**수정 내용** (초안 대비):
- 생성자에 `recordingTimeManager`, `progressPersistenceService`, `userPreferencesRepository` 명시
- `updateQaItem(item, level)` — level 파라미터 추가, `item.answers[level]`로 접근
- `hasSentenceCountChanged()` — SentenceSplitter.split() 사용, level 파라미터로 정확한 답변 접근
- `findScriptIndex()` — Entity의 itemId 활용
- `MemorizeLevel.values()` → `MemorizeLevel.entries`

### QaDataManager 데이터 소스 교체 전략

**핵심 결정**: `QaDataLoader` 인터페이스를 유지하고 Room 기반 구현체를 추가.

```kotlin
// data/repository/RoomQaDataLoader.kt
class RoomQaDataLoader(
    private val qaItemDao: QaItemDao
) : QaDataLoader {

    override suspend fun loadQaItemsForLevel(level: UserLevel): List<QaItem> {
        return qaItemDao.getByCategoryAndLevelDirect(level.name)
            .map { it.toQaItem() }
    }
}
```

**이유**: QaDataManager는 `QaDataLoader` 인터페이스에만 의존. DI 바인딩만 `LeveledQaDataLoader` → `RoomQaDataLoader`로 교체하면 QaDataManager 수정 불필요.

**시드 실행 시점**: `AssetSeeder.seedIfNeeded()`는 `QaDataManager.init()`에서 1회 호출. `RoomQaDataLoader`는 Room 조회만 담당.

**DAO 추가 쿼리 필요**:
```kotlin
@Query("SELECT * FROM qa_items WHERE level = :level ORDER BY category, CAST(itemId AS INTEGER)")
suspend fun getByCategoryAndLevelDirect(level: String): List<QaItemEntity>
```

**QaItemEntity → QaItem 매핑**:
```kotlin
private val mappingGson = Gson()
private val stringListType = object : TypeToken<List<String>>() {}.type

fun QaItemEntity.toQaItem(): QaItem {
    val userLevel = UserLevel.entries.find { it.name == level } ?: UserLevel.IH
    return QaItem(
        id = itemId,
        category = category,
        questionEn = questionEn,
        questionKo = questionKo,
        answers = mapOf(
            userLevel to LeveledAnswer(
                answerEn = answerEn,
                answerKo = answerKo,
                vocabulary = try { mappingGson.fromJson<List<String>>(vocabulary, stringListType) ?: emptyList() } catch (_: Exception) { emptyList() },
                grammar = try { mappingGson.fromJson<List<String>>(grammar, stringListType) ?: emptyList() } catch (_: Exception) { emptyList() },
                tips = try { mappingGson.fromJson<List<String>>(tips, stringListType) ?: emptyList() } catch (_: Exception) { emptyList() }
            )
        )
    )
}
```

### Presentation Layer

#### EditScriptViewModel

```kotlin
@HiltViewModel
class EditScriptViewModel @Inject constructor(
    private val scriptEditRepository: ScriptEditRepository,
    private val validateScriptEditUseCase: ValidateScriptEditUseCase
) : ViewModel() {

    private val _sentencePairs = MutableStateFlow<List<SentencePair>>(emptyList())
    val sentencePairs: StateFlow<List<SentencePair>> = _sentencePairs.asStateFlow()

    private val _validationResult = MutableStateFlow(ValidationResult(emptyList(), true))
    val validationResult: StateFlow<ValidationResult> = _validationResult.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    fun loadSentences(qaItem: QaItem, isQuestion: Boolean, level: UserLevel) {
        val textKo = if (isQuestion) qaItem.questionKo else qaItem.answers[level]?.answerKo ?: ""
        val textEn = if (isQuestion) qaItem.questionEn else qaItem.answers[level]?.answerEn ?: ""
        val koSentences = SentenceSplitter.split(textKo)
        val enSentences = SentenceSplitter.split(textEn)
        val pairs = koSentences.zip(enSentences).map { (ko, en) -> SentencePair(ko, en) }
        _sentencePairs.value = pairs
    }

    fun updatePair(index: Int, korean: String? = null, english: String? = null) {
        val current = _sentencePairs.value.toMutableList()
        if (index in current.indices) {
            val pair = current[index]
            current[index] = pair.copy(
                korean = korean ?: pair.korean,
                english = english ?: pair.english
            )
            _sentencePairs.value = current
            validate()
        }
    }

    fun addPair() {
        _sentencePairs.value = _sentencePairs.value + SentencePair("", "")
    }

    fun removePair(index: Int) {
        val current = _sentencePairs.value.toMutableList()
        if (current.size > 1) {
            current.removeAt(index)
            _sentencePairs.value = current
            validate()
        }
    }

    fun validate() {
        _validationResult.value = validateScriptEditUseCase.validate(_sentencePairs.value)
    }

    fun save(qaItem: QaItem, isQuestion: Boolean, level: UserLevel, scriptIndex: Int) {
        val koText = SentenceSplitter.join(_sentencePairs.value.map { it.korean })
        val enText = SentenceSplitter.join(_sentencePairs.value.map { it.english })
        val updatedItem = if (isQuestion) {
            qaItem.copy(questionKo = koText, questionEn = enText)
        } else {
            val currentAnswer = qaItem.answers[level] ?: return
            qaItem.copy(answers = qaItem.answers + (level to currentAnswer.copy(answerKo = koText, answerEn = enText)))
        }
        viewModelScope.launch {
            scriptEditRepository.updateQaItem(updatedItem, level, scriptIndex)
        }
    }

    fun restoreOriginal(id: String) {
        viewModelScope.launch {
            scriptEditRepository.restoreOriginal(id)
        }
    }
}
```

**수정 내용** (초안 대비):
- `loadSentences()`에서 `qaItem.getCurrentAnswerKo()`/`getCurrentAnswer()` 대신 `qaItem.answers.values.firstOrNull()?.answerKo` 사용 — QaItem에는 getCurrentAnswer() 메서드 없음
- `save()`에 `level: UserLevel` 파라미터 추가
- `save()`에서 `SentenceSplitter.join()` 사용 (추가 필요한 메서드)
- `save()`에서 `qaItem.copy(answers = ...)`로 answers 맵 업데이트

#### 편집 UI 데이터 흐름

```
MainScreen (카드 편집 버튼 클릭)
  → QaBrowserViewModel.getCurrentIndex() → scriptIndex: Int
  → QaBrowserViewModel.currentUserLevel → level: UserLevel
  → 현재 QaItem + isQuestion 여부
  → EditScriptBottomSheet(qaItem, isQuestion, level, scriptIndex)
    → EditScriptViewModel.loadSentences(qaItem, isQuestion, level)
    → 사용자 편집
    → EditScriptViewModel.save(qaItem, isQuestion, level, scriptIndex)
      → ScriptEditRepository.updateQaItem(item, level, scriptIndex)
```

**QaBrowserViewModel 노출 필요**:
- `getCurrentIndex(): Int` — `qaDataManager.getCurrentIndex()` 위임
- `currentUserLevel: UserLevel` — `QaBrowserState.currentUserLevel` 타입을 `String` → `UserLevel`로 변경

#### EditScriptBottomSheet UI

```
┌─────────────────────────────────────┐
│  스크립트 편집            [원본 복원]  │
├─────────────────────────────────────┤
│                                     │
│  문장 1/5                    [🗑️]    │
│  ┌─────────────────────────────┐    │
│  │ 🇰🇷 한국어 문장               │    │
│  │ [텍스트 필드]                 │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🇺🇸 English sentence          │    │
│  │ [텍스트 필드]                 │    │
│  └─────────────────────────────┘    │
│                                     │
│  문장 2/5                    [🗑️]    │
│  ┌─────────────────────────────┐    │
│  │ 🇰🇷 ...                       │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ 🇺🇸 ...                       │    │
│  └─────────────────────────────┘    │
│                                     │
│  [+ 문장 쌍 추가]                    │
│                                     │
├─────────────────────────────────────┤
│  ⚠️ 마침표(.!?)로 끝나야 정상 분할됩니다 │
│                                     │
│  [취소]              [저장]         │
└─────────────────────────────────────┘
```

#### 어시스턴스 기능

| 기능 | 설명 | 구현 위치 |
|------|------|----------|
| 문장 쌍 단위 편집 | 추가/삭제가 항상 쌍 단위 → 1:1 구조적 보장 | ViewModel |
| 마침표 검증 | 문장 끝 `.`, `!`, `?`, `。` 없으면 필드 테두리 빨간색 + 경고 | ValidateScriptEditUseCase |
| 빈 문장 검증 | 한쪽만 비어있으면 저장 불가 | ValidateScriptEditUseCase |
| 최소 1쌍 보장 | 모든 쌍 삭제 불가 | ViewModel |
| 삭제 확인 | 다이얼로그 "이 문장 쌍을 삭제하시겠습니까?" | BottomSheet |
| 원본 복원 | 개별 항목 또는 전체 | ScriptEditRepository |
| 실시간 검증 | 타이핑할 때마다 검증 결과 표시 | ViewModel + UI |
| 수정 표시 | 편집된 항목에 수정 아이콘 표시 | QaBrowserViewModel |

### 기존 코드 변경 영향

| 파일 | 변경 | 이유 |
|------|------|------|
| `SentenceSplitter.kt` | `join()` 메서드 추가 | 편집 후 문장 합치기 |
| `QaDataManager` | 생성자에 `AssetSeeder` 추가, `init()`에서 `assetSeeder.seedIfNeeded()`를 `loadQaItemsFromAssets()` **이전**에 호출 | Room 시드 1회 실행 |

#### QaDataManager 수정 상세

```kotlin
class QaDataManager(
    private val qaDataLoader: QaDataLoader,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val progressPersistenceService: ProgressPersistenceService,
    private val assetSeeder: AssetSeeder  // 추가
) {
    suspend fun init() {
        assetSeeder.seedIfNeeded()       // 시드 먼저 실행 (Room에 데이터 보장)
        loadQaItemsFromAssets()
        restoreLastCategory()
        setupUserLevelObserver()
    }
    // ... 나머지 동일
}
```
| `UserPreferencesRepository` | 인터페이스에 `getSeedVersion(): Int`/`setSeedVersion(version: Int)` 추가, 구현체에 SharedPreferences 읽기/쓰기 추가 (키: `"seed_version"`, 기본값: `0`) | AssetSeeder 버전 관리 |
| `QaBrowserViewModel` | `getCurrentIndex(): Int`, `currentUserLevel: UserLevel` 노출 | 편집 UI에서 scriptIndex/level 전달 |
| `AppModule` | `LeveledQaDataLoader` → `RoomQaDataLoader` 바인딩 교체, Room DB/Dao/AssetSeeder/ScriptEditRepository `@Provides` 추가 | DI |

#### AppModule 추가 바인딩 상세

```kotlin
// Room DB
@Provides @Singleton
fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "opic_database").build()

@Provides
fun provideQaItemDao(db: AppDatabase): QaItemDao = db.qaItemDao()

@Provides @Singleton
fun provideAssetSeeder(
    @ApplicationContext context: Context,
    dao: QaItemDao,
    userPreferencesRepository: UserPreferencesRepository
): AssetSeeder = AssetSeeder(context, dao, userPreferencesRepository)

// QaDataLoader 바인딩 교체
@Provides @Singleton
fun provideQaDataLoader(dao: QaItemDao): QaDataLoader = RoomQaDataLoader(dao)

// QaDataManager 바인딩 — AssetSeeder 파라미터 추가
@Provides @Singleton
fun provideQaDataManager(
    qaDataLoader: QaDataLoader,
    userPreferencesRepository: UserPreferencesRepository,
    progressPersistenceService: ProgressPersistenceService,
    assetSeeder: AssetSeeder
): QaDataManager = QaDataManager(qaDataLoader, userPreferencesRepository, progressPersistenceService, assetSeeder)

@Provides @Singleton
fun provideScriptEditRepository(
    dao: QaItemDao,
    recordingTimeManager: RecordingTimeManager,
    progressPersistenceService: ProgressPersistenceService,
    userPreferencesRepository: UserPreferencesRepository
): ScriptEditRepository = ScriptEditRepositoryImpl(dao, recordingTimeManager, progressPersistenceService, userPreferencesRepository)
```
| `build.gradle.kts` | Room 의존성 추가 | Room |
| `QaBrowserViewModel` | `isModified` 상태 추가 | UI 수정 표시 |
| `MainScreen` | 카드에 편집 버튼 추가, BottomSheet 호출 | UI |

**변경하지 않는 것**:
- `BaseMemorizeTestRepository.splitSentences()` — 이미 SentenceSplitter 위임 중, 유지
- `BaseMemorizeTestRepository.splitSentences()` — 이미 SentenceSplitter 위임 중, 유지
- `RepeatListeningRepositoryImpl`, `EnglishWritingTestRepositoryImpl` — 동일
- `FullMemorizationUseCase` — 동일
- `TtsOrchestrator`, `TtsPlaybackController` — 동일
- `MemorizationModeCoordinator` — 동일
- `LeveledQaDataLoader` — AssetSeeder에서만 사용, 삭제하지 않음

### 편집 시 사이드 이펙트 정리 (저장 시 수행)

편집 저장 시 `ScriptEditRepositoryImpl.updateQaItem()`에서 다음을 수행:

1. Room 업데이트
2. 문장 수 변경 시 관련 데이터 정리:
   - `recordingTimeManager.clearRecordingTimes(category, scriptIndex)`
   - `progressPersistenceService.saveNavigationState(NavigationState(category, scriptIndex, 0))`
   - `progressPersistenceService.clearCategoryProgress(category, scriptIndex, memorizeLevel)` for each MemorizeLevel

## 구현 단계

| 단계 | 작업 | 의존 | 완료 기준 |
|------|------|------|----------|
| 1 | Room 의존성 추가, AppDatabase + QaItemEntity + QaItemDao | 없음 | 빌드 성공 |
| 2 | SentenceSplitter에 `join()` 메서드 추가 | 없음 | 기존 기능 동일 |
| 3 | AssetSeeder 구현 (모든 레벨 시드), 최초 실행 시 JSON → Room | 1 | 앱 실행 후 Room에 4개 레벨 데이터 존재 |
| 4 | RoomQaDataLoader 구현, DI 바인딩 교체 (LeveledQaDataLoader → RoomQaDataLoader) | 3 | 기존 기능 동일 (카테고리/탐색/TTS/레벨전환) |
| 5 | ScriptEditRepository 인터페이스 + 구현체 (사이드 이펙트 정리 포함) | 1 | 빌드 성공 |
| 6 | ValidateScriptEditUseCase | 없음 | 단위 테스트 통과 |
| 7 | EditScriptViewModel | 5, 6 | 빌드 성공 |
| 8 | EditScriptBottomSheet UI | 7 | 편집/저장/복원 동작 |
| 9 | MainScreen 편집 버튼 추가 | 8 | 카드에서 편집 진입 가능 |
| 10 | 통합 테스트 | 전체 | 반복듣기/영작/통암기 편집 후 정상 동작 |

## 사이드 이펙트 분석

| 리스크 | 영향 | 대응 |
|--------|------|------|
| Room 시드 실패 | 앱 시작 시 데이터 없음 | AssetSeeder를 비동기 + 에러 핸들링, 실패 시 LeveledQaDataLoader 폴백 |
| QaDataManager 소스 교체 | 모든 기능에 영향 | 인터페이스 유지, DI 바인딩만 교체. 각 모드 회귀 테스트 |
| 편집 후 문장 수 변경 | 녹음 시간/진행상태 무효화 | 저장 시 RecordingTimeManager + NavigationState + CategoryProgress 초기화 |
| 대량 데이터 Room 시드 | 첫 실행 지연 | 백그라운드 코루틴 + 로딩 표시 |
| 레벨 변경 시 Room 미시드 | 특정 레벨 데이터 없음 | AssetSeeder에서 모든 레벨 시드, 버전 기반 재시드 전략 (`SEED_VERSION_KEY`) |
| 앱 업데이트 시 신규 에셋 | Room에 신규 카테고리 누락 | `CURRENT_SEED_VERSION` 증가 시 자동 재시드 |
| JSON id 빈 값 | PK 충돌 | `item.id.ifBlank { index.toString() }`로 대체 |
| 분할 로직 불일치 | 편집과 재생 결과 불일치 | `SentenceSplitter` 단일 진실 공급원 사용 |

## 2차 범위 (1차에서 제외)

- vocabulary / grammar / tips 편집 (Room 스키마에 JSON 문자열로 보관 완료, UI만 추가)
- 문장 순서 변경 (드래그앤드롭)
- 편집 내역 (undo/redo)
- 카테고리 추가/삭제
- JSON 내보내기/가져오기
