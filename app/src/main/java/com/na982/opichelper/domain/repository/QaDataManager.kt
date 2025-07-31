package com.na982.opichelper.domain.repository

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.data.repository.UserPreferencesRepositoryImpl
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

/**
 * QA 데이터 관리 전담 클래스 (Manager 패턴)
 * 책임: QA 데이터 상태 관리, 카테고리 관리, 인덱스 관리, UI 상태 관리
 */
@Singleton
class QaDataManager @Inject constructor(
    private val leveledQaDataLoader: com.na982.opichelper.data.repository.LeveledQaDataLoader,
    private val userPreferencesRepository: com.na982.opichelper.domain.repository.UserPreferencesRepository
) {
    
    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
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
        loadQaItemsFromAssets(application)
        restoreLastCategory()
        
        // 사용자 레벨 변경 감지 및 데이터 재로드
        setupUserLevelObserver()
    }
    
    /**
     * 사용자 레벨 변경 감지 및 데이터 재로드
     */
    private fun setupUserLevelObserver() {
        // 코루틴 스코프에서 사용자 레벨 변경 감지
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            userPreferencesRepository.userLevel.collect { newLevel ->
                Log.d("QaDataManager", "사용자 레벨 변경 감지: $newLevel")
                // 레벨이 변경되면 데이터를 다시 로드
                application?.let { app ->
                    loadQaItemsFromAssets(app)
                    restoreLastCategory()
                }
            }
        }
    }
    
    suspend fun loadQaItemsFromAssets(application: Application) {
        // application 매개변수는 향후 확장을 위해 유지
        
        // 현재 사용자 레벨 가져오기
        val currentUserLevel = userPreferencesRepository.getUserLevel()
        Log.d("QaDataManager", "데이터 로딩 시작 - 현재 사용자 레벨: $currentUserLevel")
        
        // 현재 레벨에 맞는 데이터 로드
        val allLeveledItems = leveledQaDataLoader.loadQaItemsForLevel(currentUserLevel)
        Log.d("QaDataManager", "레벨별 데이터 로드 완료 - 총 ${allLeveledItems.size}개 항목")
        
        // 강제로 정의된 순서대로 카테고리 설정
        val orderedCategories = listOf(
            "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷", 
            "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"
        )
        
        // 카테고리별로 아이템 분류 (정의된 순서대로)
        for (displayName in orderedCategories) {
            val categoryItems = allLeveledItems.filter { item ->
                // 카테고리명으로 정확히 필터링
                item.category == displayName
            }
            
            if (categoryItems.isNotEmpty()) {
                itemsByCategory[displayName] = categoryItems
                itemIndexByCategory[displayName] = 0
                Log.d("QaDataManager", "카테고리 로드 완료: $displayName (${categoryItems.size}개 항목, 레벨: $currentUserLevel)")
            } else {
                Log.w("QaDataManager", "카테고리에 항목 없음: $displayName (레벨: $currentUserLevel)")
                itemsByCategory[displayName] = emptyList()
                itemIndexByCategory[displayName] = 0
            }
        }
        
        // 강제로 순서대로 카테고리 설정
        _categories.value = orderedCategories
        Log.d("QaDataManager", "모든 카테고리 로드 완료: ${orderedCategories.size}개 카테고리 (레벨: $currentUserLevel)")
        Log.d("QaDataManager", "카테고리 순서: ${orderedCategories.joinToString(", ")}")
    }
    
    fun getCurrentIndex(): Int {
        val category = _currentCategory.value
        return if (category != null) {
            itemIndexByCategory[category] ?: 0
        } else {
            0
        }
    }
    
    fun getCurrentCategory(): String? {
        return _currentCategory.value
    }
    
    fun getCurrentQaItem(): QaItem? {
        return _currentQaItem.value
    }
    
    fun getItemsInCategory(category: String): List<QaItem> {
        return itemsByCategory[category] ?: emptyList()
    }
    
    suspend fun selectCategory(category: String) {
        if (itemsByCategory.containsKey(category)) {
            // 현재 스크립트의 진행상황 저장
            saveCurrentProgress()
            
            _currentCategory.value = category
            itemIndexByCategory[category] = 0
            updateCurrentQaItem()
            saveLastCategory(category)
            saveLastIndex(0)  // 카테고리 선택 시 인덱스 0 저장
            Log.d("QaDataManager", "카테고리 선택: $category (인덱스: 0)")
        } else {
            Log.e("QaDataManager", "존재하지 않는 카테고리: $category")
        }
    }
    
    suspend fun nextQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val items = itemsByCategory[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (currentIndex < items.size - 1) {
                // 현재 스크립트의 진행상황 저장
                saveCurrentProgress()
                
                itemIndexByCategory[category] = currentIndex + 1
                updateCurrentQaItem()
                saveLastIndex(currentIndex + 1)
                Log.d("QaDataManager", "다음 항목으로 이동: ${currentIndex + 1}/${items.size}")
            } else {
                Log.d("QaDataManager", "마지막 항목에 도달")
            }
        }
    }
    
    suspend fun previousQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (currentIndex > 0) {
                // 현재 스크립트의 진행상황 저장
                saveCurrentProgress()
                
                itemIndexByCategory[category] = currentIndex - 1
                updateCurrentQaItem()
                saveLastIndex(currentIndex - 1)
                Log.d("QaDataManager", "이전 항목으로 이동: ${currentIndex - 1}")
            } else {
                Log.d("QaDataManager", "첫 번째 항목에 도달")
            }
        }
    }
    
    private fun updateCurrentQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val items = itemsByCategory[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (items.isNotEmpty() && currentIndex < items.size) {
                _currentQaItem.value = items[currentIndex]
                Log.d("QaDataManager", "현재 QA 항목 업데이트: ${items[currentIndex].questionEn}")
            } else {
                _currentQaItem.value = null
                if (items.isEmpty()) {
                    Log.w("QaDataManager", "카테고리에 항목이 없음: $category")
                } else {
                    Log.w("QaDataManager", "인덱스가 범위를 벗어남: $currentIndex >= ${items.size}")
                }
            }
        } else {
            _currentQaItem.value = null
        }
    }
    
    private suspend fun restoreLastCategory() {
        val lastCategory = prefs?.getString(PREF_KEY_LAST_CATEGORY, null)
        if (lastCategory != null && itemsByCategory.containsKey(lastCategory)) {
            _currentCategory.value = lastCategory
            val lastIndex = prefs?.getInt(PREF_KEY_LAST_INDEX, 0) ?: 0
            itemIndexByCategory[lastCategory] = lastIndex
            updateCurrentQaItem()
            Log.d("QaDataManager", "마지막 카테고리 복원: $lastCategory (인덱스: $lastIndex)")
        } else {
            // 기본 카테고리 선택
            val firstCategory = _categories.value.firstOrNull()
            if (firstCategory != null) {
                selectCategory(firstCategory)
                Log.d("QaDataManager", "기본 카테고리 선택: $firstCategory")
            }
        }
    }
    
    private fun saveLastCategory(category: String) {
        prefs?.edit()?.putString(PREF_KEY_LAST_CATEGORY, category)?.apply()
    }
    
    private fun saveLastIndex(index: Int) {
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, index)?.apply()
        Log.d("QaDataManager", "인덱스 저장: $index (SharedPreferences: opic_prefs)")
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
     * @param memorizeLevel 현재 활성화된 암기레벨
     */
    suspend fun saveCurrentProgress(memorizeLevel: String? = null) {
        val category = _currentCategory.value
        val currentIndex = itemIndexByCategory[category] ?: 0
        
        if (category != null) {
            // 현재 진행상황 확인
            // progressTracker.getScriptProgress(category, currentIndex, memorizeLevel ?: "반복 듣기") // progressTracker 제거
            
            // 진행상황이 있으면 저장
            // progressTracker.persistChangedProgress() // progressTracker 제거
            Log.d("QaDataManager", "현재 진행상황 저장: $category (인덱스: $currentIndex, 레벨: ${memorizeLevel ?: "반복 듣기"})")
        }
    }
    
    // Asset 데이터 클래스
    private data class QaItemAsset(
        val id: String?,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )
} 