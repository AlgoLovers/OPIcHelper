# 스크립트 편집 기능 구현 계획

## 목표

사용자가 앱 내에서 질문/답변의 한글·영문 문장을 편집할 수 있도록 한다.
문장 쌍(한국어↔영어) 1:1 매칭을 구조적으로 보장하고, 사용자 실수를 사전에 방지한다.

## 핵심 제약

1. **1:1 매칭**: 반복듣기/영작/통암기 모두 `koSentences[i]` ↔ `enSentences[i]`로 매핑. 쌍이 깨지면 크래시 또는 하이라이트 어긋남
2. **마침표 분할**: `split(Regex("(?<=[.!?])\\s+"))`로 문장 분할. 마침표 누락 시 분할 오류
3. **TTS 의존**: 빈 문장 또는 공백만 있는 문장은 TTS 재생 실패

## 아키텍처

### Clean Architecture 준수

```
Presentation (EditScriptViewModel + BottomSheet)
    ↓ 편집 요청 (Entity 전달)
Domain (ScriptEditRepository 인터페이스)
    ↓ 의존성 역전
Data (ScriptEditRepositoryImpl → Room DB)
```

- Domain 계층은 Data(Room)를 직접 참조하지 않음
- Repository 인터페이스는 Domain에, 구현체는 Data에 위치
- ViewModel은 Repository 인터페이스만 의존

### Data Layer

#### Room Database

```kotlin
@Entity(tableName = "qa_items")
data class QaItemEntity(
    @PrimaryKey val id: String,           // "{category}_{index}_{level}"
    val category: String,
    val index: Int,
    val level: String,                    // AL, IH, IH_RAW, IM
    val questionEn: String,
    val questionKo: String,
    val answerEn: String,
    val answerKo: String,
    val answerEnOriginal: String,         // 원본 백업 (불변)
    val answerKoOriginal: String,         // 원본 백업 (불변)
    val questionEnOriginal: String,       // 원본 백업 (불변)
    val questionKoOriginal: String,       // 원본 백업 (불변)
    val isModified: Boolean = false,
    val updatedAt: Long = 0L
)
```

**설계 의사결정**:
- `answerEnOriginal` 등 원본 필드를 같은 Entity에 포함: 별도 테이블보다 단순, 원본 복원이 단순 UPDATE
- `isModified` 플래그: UI에서 수정 여부 표시 (편집 아이콘 하이라이트 등)
- `id` 형식: `{category}_{index}_{level}` — 복합키 대신 단일 문자열 PK로 단순화

#### Dao

```kotlin
@Dao
interface QaItemDao {
    @Query("SELECT * FROM qa_items WHERE category = :category AND level = :level ORDER BY `index`")
    fun getByCategoryAndLevel(category: String, level: String): Flow<List<QaItemEntity>>

    @Query("SELECT * FROM qa_items WHERE id = :id")
    suspend fun getById(id: String): QaItemEntity?

    @Update
    suspend fun update(item: QaItemEntity)

    @Query("UPDATE qa_items SET questionEn = questionEnOriginal, questionKo = questionKoOriginal, answerEn = answerEnOriginal, answerKo = answerKoOriginal, isModified = 0, updatedAt = 0 WHERE id = :id")
    suspend fun restoreOriginal(id: String)

    @Query("UPDATE qa_items SET questionEn = questionEnOriginal, questionKo = questionKoOriginal, answerEn = answerEnOriginal, answerKo = answerKoOriginal, isModified = 0, updatedAt = 0")
    suspend fun restoreAllOriginal()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QaItemEntity>)

    @Query("SELECT COUNT(*) FROM qa_items")
    suspend fun getCount(): Int
}
```

#### AssetSeeder

```kotlin
class AssetSeeder(
    private val context: Context,
    private val dao: QaItemDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun seedIfNeeded() {
        if (dao.getCount() > 0) return
        val level = userPreferencesRepository.getUserLevel()
        val items = LeveledQaDataLoader(context).loadQaItemsForLevel(level)
        val entities = items.flatMap { item ->
            item.answers.map { (userLevel, answer) ->
                QaItemEntity(
                    id = "${item.category}_${item.id}_${userLevel.name}",
                    category = item.category,
                    index = item.id.toIntOrNull() ?: 0,
                    level = userLevel.name,
                    questionEn = item.questionEn,
                    questionKo = item.questionKo,
                    answerEn = answer.answerEn,
                    answerKo = answer.answerKo,
                    answerEnOriginal = answer.answerEn,
                    answerKoOriginal = answer.answerKo,
                    questionEnOriginal = item.questionEn,
                    questionKoOriginal = item.questionKo
                )
            }
        }
        dao.insertAll(entities)
    }
}
```

**주의**: `seedIfNeeded()`는 `AppDatabase` 초기화 시 호출. Room 마이그레이션으로 버전 관리.

#### AppDatabase

```kotlin
@Database(entities = [QaItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qaItemDao(): QaItemDao
}
```

### Domain Layer

#### Repository 인터페이스

```kotlin
// domain/repository/ScriptEditRepository.kt
interface ScriptEditRepository {
    fun getQaItemsByCategory(category: String, level: String): Flow<List<QaItem>>
    suspend fun updateQaItem(item: QaItem)
    suspend fun restoreOriginal(id: String)
    suspend fun restoreAllOriginal()
    suspend fun isModified(id: String): Boolean
}
```

**주의**: Domain의 `QaItem` Entity는 기존 것을 그대로 사용. Room Entity(`QaItemEntity`)와 매핑은 Data 계층에서 처리.

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
            if (pair.korean.isNotBlank() && !pair.korean.trim().endsWithSentenceEnd(isKorean = true))
                errors.add(ValidationError.MissingPunctuation(i, isKorean = true))
            if (pair.english.isNotBlank() && !pair.english.trim().endsWithSentenceEnd(isKorean = false))
                errors.add(ValidationError.MissingPunctuation(i, isKorean = false))
        }
        return ValidationResult(errors, errors.isEmpty())
    }

    private fun String.endsWithSentenceEnd(isKorean: Boolean): Boolean =
        if (isKorean) endsWith(".") || endsWith("!") || endsWith("?") || endsWith(".")
        else endsWith(".") || endsWith("!") || endsWith("?")
}
```

**설계 의사결정**: 검증을 Domain UseCase로 분리. SRP — 검증 로직이 UI나 Repository에 섞이지 않음. 테스트 가능.

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

    fun loadSentences(qaItem: QaItem, isQuestion: Boolean) {
        val textKo = if (isQuestion) qaItem.questionKo else qaItem.getCurrentAnswerKo()
        val textEn = if (isQuestion) qaItem.questionEn else qaItem.getCurrentAnswer()
        val koSentences = splitSentences(textKo)
        val enSentences = splitSentences(textEn)
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

    fun save(qaItem: QaItem, isQuestion: Boolean) { ... }
    fun restoreOriginal(id: String) { ... }
}
```

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
| 마침표 검증 | 문장 끝 `.`, `!`, `?` 없으면 필드 테두리 빨간색 + 경고 | ValidateScriptEditUseCase |
| 빈 문장 검증 | 한쪽만 비어있으면 저장 불가 | ValidateScriptEditUseCase |
| 최소 1쌍 보장 | 모든 쌍 삭제 불가 | ViewModel |
| 삭제 확인 | 다이얼로그 "이 문장 쌍을 삭제하시겠습니까?" | BottomSheet |
| 원본 복원 | 개별 항목 또는 전체 | ScriptEditRepository |
| 실시간 검증 | 타이핑할 때마다 검증 결과 표시 | ViewModel + UI |
| 수정 표시 | 편집된 항목에 수정 아이콘 표시 | QaBrowserViewModel |

### 기존 코드 변경 영향

| 파일 | 변경 | 이유 |
|------|------|------|
| `QaDataManager` | Data 소스를 `LeveledQaDataLoader` → `QaItemDao` Flow로 교체 | Room이 단일 진실 공급원 |
| `LeveledQaDataLoader` | AssetSeeder에서만 사용, QaDataManager에서 제거 | 시드 전용 |
| `AppModule` | Room DB, Dao, AssetSeeder, ScriptEditRepository 바인딩 추가 | DI |
| `QaBrowserViewModel` | `isModified` 상태 추가 | UI 수정 표시 |
| `MainScreen` | 카드에 편집 버튼 추가, BottomSheet 호출 | UI |
| `build.gradle` | Room 의존성 추가 | Room |

**변경하지 않는 것**:
- `RepeatListeningRepositoryImpl`, `EnglishWritingTestRepositoryImpl` — 문장 분할 로직 동일, Room 데이터가 올바르면 자동 작동
- `FullMemorizationUseCase` — 동일
- `TtsOrchestrator`, `TtsPlaybackController` — 동일
- `MemorizationModeCoordinator` — 동일

## 구현 단계

| 단계 | 작업 | 의존 | 완료 기준 |
|------|------|------|----------|
| 1 | Room 의존성 추가, AppDatabase + QaItemEntity + QaItemDao | 없음 | 빌드 성공 |
| 2 | AssetSeeder 구현, 최초 실행 시 JSON → Room 시드 | 1 | 앱 실행 후 Room에 데이터 존재 |
| 3 | QaDataManager Data 소스를 Room으로 교체 | 2 | 기존 기능 동일 (카테고리/탐색/TTS) |
| 4 | ScriptEditRepository 인터페이스 + 구현체 | 1 | 빌드 성공 |
| 5 | ValidateScriptEditUseCase | 없음 | 단위 테스트 통과 |
| 6 | EditScriptViewModel | 4, 5 | 빌드 성공 |
| 7 | EditScriptBottomSheet UI | 6 | 편집/저장/복원 동작 |
| 8 | MainScreen 편집 버튼 추가 | 7 | 카드에서 편집 진입 가능 |
| 9 | 통합 테스트 | 전체 | 반복듣기/영작/통암기 편집 후 정상 동작 |

## 사이드 이펙트 분석

| 리스크 | 영향 | 대응 |
|--------|------|------|
| Room 시드 실패 | 앱 시작 시 데이터 없음 | AssetSeeder를 비동기 + 에러 핸들링, 실패 시 Asset 폴백 |
| QaDataManager 소스 교체 | 모든 기능에 영향 | 인터페이스 유지, 구현만 교체. 각 모드 회귀 테스트 |
| 편집 후 문장 수 변경 | 반복듣기/영작 진행 상태 인덱스 어긋남 | 편집 시 진행 상태 초기화 |
| 대량 데이터 Room 시드 | 첫 실행 지연 | 백그라운드 코루틴 + 로딩 표시 |

## 2차 범위 (1차에서 제외)

- vocabulary / grammar / tips 편집
- 문장 순서 변경 (드래그앤드롭)
- 편집 내역 (undo/redo)
- 카테고리 추가/삭제
- JSON 내보내기/가져오기
