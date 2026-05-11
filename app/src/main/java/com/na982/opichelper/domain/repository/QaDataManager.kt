package com.na982.opichelper.domain.repository

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch

/**
 * QA 데이터 관리 전담 클래스 (Manager 패턴)
 * 책임: QA 데이터 상태 관리, 카테고리 관리, 인덱스 관리, UI 상태 관리
 */
@Singleton
class QaDataManager @Inject constructor(
    private val progressTracker: MemorizeTestProgressTracker,
    private val qaDataLoader: QaDataLoader,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var userLevelJob: Job? = null

    private val itemsByCategory: MutableMap<String, List<QaItem>> = ConcurrentHashMap()
    private val itemIndexByCategory: MutableMap<String, Int> = ConcurrentHashMap()
    
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
        userLevelJob?.cancel()
        userLevelJob = scope.launch {
            userPreferencesRepository.userLevel.collect { newLevel ->
                Log.d("QaDataManager", "사용자 레벨 변경 감지: $newLevel")
                application?.let { app ->
                    loadQaItemsFromAssets(app)
                    restoreLastCategory()
                }
            }
        }
    }
    
    suspend fun loadQaItemsFromAssets(application: Application) {
        val preferredOrder = listOf(
            "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷",
            "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절", "롤플레이"
        )

        val currentUserLevel = userPreferencesRepository.getUserLevel()
        Log.d("QaDataManager", "데이터 로딩 시작 - 현재 사용자 레벨: $currentUserLevel")

        val allLeveledItems = qaDataLoader.loadQaItemsForLevel(currentUserLevel)
        Log.d("QaDataManager", "레벨별 데이터 로드 완료 - 총 ${allLeveledItems.size}개 항목")

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
            Log.d("QaDataManager", "카테고리 로드 완료: $category (${categoryItems.size}개 항목, 레벨: $currentUserLevel)")
        }

        _categories.value = sortedCategories
        Log.d("QaDataManager", "모든 카테고리 로드 완료: ${sortedCategories.size}개 카테고리 (레벨: $currentUserLevel)")
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

    fun release() {
        userLevelJob?.cancel()
        userLevelJob = null
        scope.cancel()
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
            val currentProgress = progressTracker.getScriptProgress(category, currentIndex, memorizeLevel ?: MemorizeLevel.REPEAT_LISTENING.displayName)

            if (currentProgress != null) {
                progressTracker.persistChangedProgress()
                Log.d("QaDataManager", "현재 진행상황 저장: $category (인덱스: $currentIndex, 레벨: ${memorizeLevel ?: MemorizeLevel.REPEAT_LISTENING.displayName})")
            } else {
                Log.d("QaDataManager", "저장할 진행상황 없음: $category (인덱스: $currentIndex)")
            }
        }
    }
}