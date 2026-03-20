package com.na982.opichelper.domain.repository

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QA 데이터 관리 전담 클래스 (Repository 패턴)
 * 책임: QA 데이터 상태 관리, 카테고리 관리, 인덱스 관리, UI 상태 관리
 */
@Singleton
class QaDataRepository @Inject constructor(
    private val leveledQaDataLoader: com.na982.opichelper.data.repository.LeveledQaDataLoader,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    
    // 현재 로드된 카테고리만 저장
    private val loadedCategories: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()
    
    private var prefs: SharedPreferences? = null
    private var application: Application? = null
    private val PREF_KEY_LAST_CATEGORY = "last_category"
    private val PREF_KEY_LAST_INDEX = "last_index"
    
    // UI 상태 관리
    private val _currentQaItem = MutableStateFlow<QaItem?>(null)
    val currentQaItem: StateFlow<QaItem?> = _currentQaItem.asStateFlow()
    
    private val _currentCategory = MutableStateFlow<String?>(null)
    val currentCategory: StateFlow<String?> = _currentCategory.asStateFlow()
    
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    suspend fun init(application: Application) {
        this.application = application
        prefs = application.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
        
        // 카테고리 목록만 설정 (실제 데이터는 로드하지 않음)
        setupCategories()
        restoreLastCategory()
        
        // 사용자 레벨 변경 감지
        setupUserLevelObserver()
    }
    
    /**
     * 카테고리 목록만 설정 (실제 데이터 로드하지 않음)
     */
    private fun setupCategories() {
        val orderedCategories = listOf(
            "자기소개", "롤플레이", "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷",
            "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"
        )
        _categories.value = orderedCategories
        Log.d("QaDataRepository", "카테고리 목록 설정 완료: ${orderedCategories.size}개")
    }
    
    /**
     * 특정 카테고리의 데이터를 로드 (Lazy Loading)
     */
    suspend fun loadCategoryData(category: String): List<QaItem> {
        // 이미 로드된 카테고리인지 확인
        if (loadedCategories.containsKey(category)) {
            Log.d("QaDataRepository", "카테고리 '$category' 이미 로드됨")
            return loadedCategories[category] ?: emptyList()
        }
        
        Log.d("QaDataRepository", "카테고리 '$category' 데이터 로드 시작")
        _isLoading.value = true
        
        try {
            // 현재 사용자 레벨 가져오기
            val currentUserLevel = userPreferencesRepository.getUserLevel()
            
            // 해당 카테고리의 데이터만 로드
            val categoryItems = leveledQaDataLoader.loadQaItemsForCategory(
                level = currentUserLevel,
                category = category
            )
            
            // 로드된 데이터 저장
            loadedCategories[category] = categoryItems
            
            Log.d("QaDataRepository", "카테고리 '$category' 로드 완료: ${categoryItems.size}개 항목")
            _isLoading.value = false
            
            return categoryItems
        } catch (e: Exception) {
            Log.e("QaDataRepository", "카테고리 '$category' 로드 실패", e)
            _error.value = "카테고리 로드 실패: ${e.message}"
            _isLoading.value = false
            return emptyList()
        }
    }
    
    /**
     * 현재 카테고리 변경 시 데이터 로드
     */
    suspend fun setCurrentCategory(category: String) {
        Log.d("QaDataRepository", "현재 카테고리 변경: $category")
        
        // 카테고리 데이터가 없으면 로드
        if (!loadedCategories.containsKey(category)) {
            Log.d("QaDataRepository", "카테고리 '$category' 데이터 로드 시작")
            loadCategoryData(category)
        }
        
        // 카테고리 데이터 로드 (Lazy Loading)
        val items = loadedCategories[category] ?: emptyList()
        
        if (items.isNotEmpty()) {
            _currentCategory.value = category
            _currentQaItem.value = items.first()
            
            // 마지막 인덱스 복원
            val lastIndex = itemIndexByCategory[category] ?: 0
            if (lastIndex < items.size) {
                _currentQaItem.value = items[lastIndex]
            }
            
            // SharedPreferences에 저장
            prefs?.edit()?.apply {
                putString(PREF_KEY_LAST_CATEGORY, category)
                putInt(PREF_KEY_LAST_INDEX, lastIndex)
            }?.apply()
            
            Log.d("QaDataRepository", "카테고리 '$category' 설정 완료")
        } else {
            Log.w("QaDataRepository", "카테고리 '$category'에 데이터가 없음")
        }
    }
    
    /**
     * 카테고리 선택
     */
    suspend fun selectCategory(category: String) {
        // 현재 스크립트의 진행상황 저장
        saveCurrentProgress()
        
        // 카테고리 데이터가 없으면 로드
        if (!loadedCategories.containsKey(category)) {
            Log.d("QaDataRepository", "카테고리 '$category' 데이터 로드 시작")
            loadCategoryData(category)
        }
        
        // 카테고리 설정
        setCurrentCategory(category)
        saveLastCategory(category)
        saveLastIndex(0)  // 카테고리 선택 시 인덱스 0 저장
        Log.d("QaDataRepository", "카테고리 선택: $category (인덱스: 0)")
    }
    
    /**
     * 다음 항목으로 이동
     */
    suspend fun moveToNext() {
        val category = _currentCategory.value
        if (category != null) {
            val items = loadedCategories[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (currentIndex < items.size - 1) {
                // 현재 스크립트의 진행상황 저장
                saveCurrentProgress()
                
                itemIndexByCategory[category] = currentIndex + 1
                updateCurrentQaItem()
                saveLastIndex(currentIndex + 1)
                Log.d("QaDataRepository", "다음 항목으로 이동: ${currentIndex + 1}/${items.size}")
            } else {
                Log.d("QaDataRepository", "마지막 항목에 도달")
            }
        }
    }
    
    /**
     * 이전 항목으로 이동
     */
    suspend fun moveToPrevious() {
        val category = _currentCategory.value
        if (category != null) {
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (currentIndex > 0) {
                // 현재 스크립트의 진행상황 저장
                saveCurrentProgress()
                
                itemIndexByCategory[category] = currentIndex - 1
                updateCurrentQaItem()
                saveLastIndex(currentIndex - 1)
                Log.d("QaDataRepository", "이전 항목으로 이동: ${currentIndex - 1}")
            } else {
                Log.d("QaDataRepository", "첫 번째 항목에 도달")
            }
        }
    }
    
    private fun updateCurrentQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val items = loadedCategories[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (items.isNotEmpty() && currentIndex < items.size) {
                _currentQaItem.value = items[currentIndex]
                Log.d("QaDataRepository", "현재 QA 항목 업데이트: ${items[currentIndex].questionEn}")
            } else {
                _currentQaItem.value = null
                if (items.isEmpty()) {
                    Log.w("QaDataRepository", "카테고리에 항목이 없음: $category")
                } else {
                    Log.w("QaDataRepository", "인덱스가 범위를 벗어남: $currentIndex >= ${items.size}")
                }
            }
        } else {
            _currentQaItem.value = null
        }
    }
    
    private suspend fun restoreLastCategory() {
        val lastCategory = prefs?.getString(PREF_KEY_LAST_CATEGORY, null)
        val lastIndex = prefs?.getInt(PREF_KEY_LAST_INDEX, 0) ?: 0
        
        if (lastCategory != null) {
            try {
                Log.d("QaDataRepository", "마지막 카테고리 복원 시도: $lastCategory (인덱스: $lastIndex)")
                
                // 1. 카테고리 데이터 로드 (Lazy Loading)
                loadCategoryData(lastCategory)
                
                // 2. 인덱스 복원
                itemIndexByCategory[lastCategory] = lastIndex
                
                // 3. 카테고리 설정 (데이터가 로드된 후)
                setCurrentCategory(lastCategory)
                
                Log.d("QaDataRepository", "마지막 카테고리 복원 성공: $lastCategory (인덱스: $lastIndex)")
            } catch (e: Exception) {
                Log.e("QaDataRepository", "마지막 카테고리 복원 실패: $lastCategory", e)
                // 복원 실패 시 기본 카테고리로 폴백
                fallbackToDefaultCategory()
            }
        } else {
            Log.d("QaDataRepository", "저장된 마지막 카테고리 없음, 기본 카테고리 선택")
            fallbackToDefaultCategory()
        }
    }
    
    /**
     * 기본 카테고리로 폴백
     */
    private suspend fun fallbackToDefaultCategory() {
        val firstCategory = _categories.value.firstOrNull()
        if (firstCategory != null) {
            selectCategory(firstCategory)
            Log.d("QaDataRepository", "기본 카테고리 선택: $firstCategory")
        } else {
            Log.w("QaDataRepository", "사용 가능한 카테고리가 없음")
        }
    }
    
    private fun saveLastCategory(category: String) {
        prefs?.edit()?.putString(PREF_KEY_LAST_CATEGORY, category)?.apply()
    }
    
    private fun saveLastIndex(index: Int) {
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, index)?.apply()
        Log.d("QaDataRepository", "인덱스 저장: $index (SharedPreferences: opic_prefs)")
    }
    
    /**
     * 현재 인덱스를 저장 (외부에서 호출 가능)
     */
    fun saveCurrentIndex(index: Int) {
        saveLastIndex(index)
    }
    
    fun clearError() {
        _error.value = null
    }

    /**
     * 현재 스크립트의 진행상황 저장
     */
    suspend fun saveCurrentProgress(memorizeLevel: String? = null) {
        val category = _currentCategory.value
        val currentIndex = itemIndexByCategory[category] ?: 0
        
        if (category != null) {
            Log.d("QaDataRepository", "현재 진행상황 저장: $category (인덱스: $currentIndex, 레벨: ${memorizeLevel ?: "반복 듣기"})")
        }
    }
    
    /**
     * 사용자 레벨 변경 감지
     */
    private fun setupUserLevelObserver() {
        // 코루틴 스코프에서 사용자 레벨 변경 감지
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            userPreferencesRepository.userLevel.collect { newLevel ->
                Log.d("QaDataRepository", "사용자 레벨 변경 감지: $newLevel")
                // 레벨이 변경되면 로드된 카테고리를 모두 클리어
                loadedCategories.clear()
                
                // 현재 카테고리가 있으면 다시 로드
                val currentCategory = _currentCategory.value
                if (currentCategory != null) {
                    loadCategoryData(currentCategory)
                    Log.d("QaDataRepository", "레벨 변경 후 현재 카테고리 재로드: $currentCategory")
                }
            }
        }
    }
    
    // ===== Getter 메서드들 =====
    
    fun getCurrentQaItem(): QaItem? {
        return _currentQaItem.value
    }
    
    fun getCurrentCategory(): String? {
        return _currentCategory.value
    }
    
    fun getCurrentIndex(): Int {
        val category = _currentCategory.value
        return if (category != null) {
            itemIndexByCategory[category] ?: 0
        } else {
            0
        }
    }
    
    fun getItemsInCategory(category: String): List<QaItem> {
        return loadedCategories[category] ?: emptyList()
    }
} 