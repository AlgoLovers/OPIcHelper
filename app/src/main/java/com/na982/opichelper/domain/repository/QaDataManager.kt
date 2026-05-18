package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * QA 데이터 관리 전담 클래스 (Manager 패턴)
 * 책임: QA 데이터 상태 관리, 카테고리 관리, 인덱스 관리, UI 상태 관리
 */
class QaDataManager(
    private val qaDataLoader: QaDataLoader,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val progressPersistenceService: ProgressPersistenceService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var userLevelJob: Job? = null

    private val itemsByCategory: MutableMap<String, List<QaItem>> = ConcurrentHashMap()
    private val itemIndexByCategory: MutableMap<String, Int> = ConcurrentHashMap()
    
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
    
    suspend fun init() {
        loadQaItemsFromAssets()
        restoreLastCategory()
        setupUserLevelObserver()
    }
    
    /**
     * 사용자 레벨 변경 감지 및 데이터 재로드
     */
    private fun setupUserLevelObserver() {
        userLevelJob?.cancel()
        userLevelJob = scope.launch {
            userPreferencesRepository.userLevel.collect { _ ->
                loadQaItemsFromAssets()
                restoreLastCategory()
            }
        }
    }
    
    suspend fun loadQaItemsFromAssets() {
        val preferredOrder = listOf(
            "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷",
            "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절", "롤플레이"
        )

        val currentUserLevel = userPreferencesRepository.getUserLevel()

        val allLeveledItems = qaDataLoader.loadQaItemsForLevel(currentUserLevel)

        // JSON의 title 필드에서 동적으로 카테고리 추출
        val loadedCategories = allLeveledItems.map { it.category }.distinct()

        // 우선순위 정렬: 기존 순서 유지, 새 카테고리는 끝에 추가
        val sortedCategories = loadedCategories.sortedBy { category ->
            val index = preferredOrder.indexOf(category)
            if (index >= 0) index else Int.MAX_VALUE
        }

        for (category in sortedCategories) {
            val categoryItems = allLeveledItems.filter { it.category == category }
            itemsByCategory[category] = categoryItems
            itemIndexByCategory[category] = 0
        }

        _categories.value = sortedCategories
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
    
    /**
     * 현재 사용자 레벨에 맞는 답변을 가져오기
     */
    fun getCurrentAnswer(qaItem: QaItem?): String {
        if (qaItem == null) return ""
        
        val currentUserLevel = userPreferencesRepository.getUserLevel()
        val leveledAnswer = qaItem.answers[currentUserLevel]
        
        return leveledAnswer?.answerEn ?: qaItem.answers.values.firstOrNull()?.answerEn ?: ""
    }
    
    /**
     * 현재 사용자 레벨에 맞는 한국어 답변을 가져오기
     */
    fun getCurrentAnswerKo(qaItem: QaItem?): String {
        if (qaItem == null) return ""
        
        val currentUserLevel = userPreferencesRepository.getUserLevel()
        val leveledAnswer = qaItem.answers[currentUserLevel]
        
        return leveledAnswer?.answerKo ?: qaItem.answers.values.firstOrNull()?.answerKo ?: ""
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
            } else {
                // 마지막 항목
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
            } else {
                // 첫 번째 항목
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
        val navState = progressPersistenceService.loadNavigationState()
        val lastCategory = navState.category
        if (lastCategory != null && itemsByCategory.containsKey(lastCategory)) {
            _currentCategory.value = lastCategory
            itemIndexByCategory[lastCategory] = navState.index
            updateCurrentQaItem()
        } else {
            val firstCategory = _categories.value.firstOrNull()
            if (firstCategory != null) {
                selectCategory(firstCategory)
            }
        }
    }

    private suspend fun saveLastCategory(category: String) {
        val currentIndex = itemIndexByCategory[category] ?: 0
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, currentIndex, currentIndex)
        )
    }

    private suspend fun saveLastIndex(index: Int) {
        val category = _currentCategory.value
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, index, index)
        )
    }
    
    /**
     * 현재 인덱스를 저장 (외부에서 호출 가능)
     */
    suspend fun saveCurrentIndex(index: Int) {
        saveLastIndex(index)
    }
    
    fun clearError() {
        _error.value = null
    }

    fun release() {
        userLevelJob?.cancel()
        userLevelJob = null
        scope.cancel()
    }

    suspend fun saveCurrentProgress(memorizeLevel: String? = null) {
        // 진행상황 영속화는 ViewModel에서 MemorizeTestProgressTracker를 통해 처리
    }
}