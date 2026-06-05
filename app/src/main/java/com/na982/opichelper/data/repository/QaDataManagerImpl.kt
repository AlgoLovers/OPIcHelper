package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.DataSeeder
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.UserLevelPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.na982.opichelper.domain.manager.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QaDataManagerImpl(
    private val qaDataLoader: QaDataLoader,
    private val userLevelPreferences: UserLevelPreferences,
    private val progressPersistenceService: ProgressPersistenceService,
    private val dataSeeder: DataSeeder,
    private val appLogger: AppLogger
) : QaDataManager {

    companion object {
        private const val MIN_SEARCH_QUERY_LENGTH = 2
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile
    private var userLevelJob: Job? = null

    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()
    private val mutex = Mutex()

    private val _currentQaItem = MutableStateFlow<QaItem?>(null)
    override val currentQaItem: StateFlow<QaItem?> = _currentQaItem.asStateFlow()

    private val _currentCategory = MutableStateFlow<String?>(null)
    override val currentCategory: StateFlow<String?> = _currentCategory.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    override val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    override suspend fun init() = mutex.withLock {
        dataSeeder.seedIfNeeded()
        loadQaItemsFromAssets()
        restoreLastCategory()
        setupUserLevelObserver()
    }

    override suspend fun reload() = mutex.withLock {
        loadQaItemsFromAssets()
        updateCurrentQaItem()
    }

    private fun setupUserLevelObserver() {
        userLevelJob?.cancel()
        userLevelJob = scope.launch {
            userLevelPreferences.userLevel.collect { _ ->
                loadQaItemsFromAssets()
                restoreLastCategory()
            }
        }
    }

    private suspend fun loadQaItemsFromAssets() {
        val preferredOrder = listOf(
            "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷",
            "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절", "롤플레이"
        )

        val currentUserLevel = userLevelPreferences.getUserLevel()

        val allLeveledItems = qaDataLoader.loadQaItemsForLevel(currentUserLevel)

        val loadedCategories = allLeveledItems.map { it.category }.distinct()

        val sortedCategories = loadedCategories.sortedBy { category ->
            val index = preferredOrder.indexOf(category)
            if (index >= 0) index else Int.MAX_VALUE
        }

        for (category in sortedCategories) {
            val categoryItems = allLeveledItems.filter { it.category == category }
            itemsByCategory[category] = categoryItems
            itemIndexByCategory[category] = 0
        }

        _categories.update { sortedCategories }
    }

    override fun getCurrentIndex(): Int {
        val category = _currentCategory.value
        return if (category != null) {
            itemIndexByCategory[category] ?: 0
        } else {
            0
        }
    }

    override fun getCurrentCategory(): String? {
        return _currentCategory.value
    }

    override fun getCurrentQaItem(): QaItem? {
        return _currentQaItem.value
    }

    override fun getCurrentAnswer(qaItem: QaItem?): String {
        if (qaItem == null) return ""

        val currentUserLevel = userLevelPreferences.getUserLevel()
        val leveledAnswer = qaItem.answers[currentUserLevel]

        return leveledAnswer?.answerEn ?: qaItem.answers.values.firstOrNull()?.answerEn ?: ""
    }

    override fun getCurrentAnswerKo(qaItem: QaItem?): String {
        if (qaItem == null) return ""

        val currentUserLevel = userLevelPreferences.getUserLevel()
        val leveledAnswer = qaItem.answers[currentUserLevel]

        return leveledAnswer?.answerKo ?: qaItem.answers.values.firstOrNull()?.answerKo ?: ""
    }

    override fun getItemsInCategory(category: String): List<QaItem> {
        return itemsByCategory[category] ?: emptyList()
    }

    override suspend fun selectCategory(category: String) = mutex.withLock {
        if (itemsByCategory.containsKey(category)) {
            navigateTo(category, 0)
        } else {
            appLogger.e("QaDataManager", "존재하지 않는 카테고리: $category")
        }
    }

    override fun hasNextQaItem(): Boolean {
        val category = _currentCategory.value ?: return false
        val items = itemsByCategory[category] ?: emptyList()
        val currentIndex = itemIndexByCategory[category] ?: 0
        return currentIndex < items.size - 1
    }

    override suspend fun nextQaItem() = mutex.withLock {
        val category = _currentCategory.value
        if (category != null) {
            val items = itemsByCategory[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0

            if (currentIndex < items.size - 1) {
                navigateTo(category, currentIndex + 1)
            }
        }
    }

    override suspend fun previousQaItem() = mutex.withLock {
        val category = _currentCategory.value
        if (category != null) {
            val currentIndex = itemIndexByCategory[category] ?: 0

            if (currentIndex > 0) {
                navigateTo(category, currentIndex - 1)
            }
        }
    }

    private fun updateCurrentQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val items = itemsByCategory[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0

            if (items.isNotEmpty() && currentIndex < items.size) {
                _currentQaItem.update { items[currentIndex] }
            } else {
                _currentQaItem.update { null }
                if (items.isEmpty()) {
                    appLogger.w("QaDataManager", "카테고리에 항목이 없음: $category")
                } else {
                    appLogger.w("QaDataManager", "인덱스가 범위를 벗어남: $currentIndex >= ${items.size}")
                }
            }
        } else {
            _currentQaItem.update { null }
        }
    }

    private suspend fun restoreLastCategory() {
        val navState = progressPersistenceService.loadNavigationState()
        val lastCategory = navState.category
        if (lastCategory != null && itemsByCategory.containsKey(lastCategory)) {
            _currentCategory.update { lastCategory }
            itemIndexByCategory[lastCategory] = navState.scriptIndex
            updateCurrentQaItem()
        } else {
            val firstCategory = _categories.value.firstOrNull()
            if (firstCategory != null) {
                selectCategory(firstCategory)
            }
        }
    }

    private suspend fun navigateTo(category: String, index: Int) {
        _currentCategory.update { category }
        itemIndexByCategory[category] = index
        updateCurrentQaItem()
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, index, 0)
        )
    }

    override fun clearError() {
        _error.update { null }
    }

    override fun searchItems(query: String): List<QaItem> {
        if (query.length < MIN_SEARCH_QUERY_LENGTH) return emptyList()
        val lowerQuery = query.lowercase()
        return itemsByCategory.values.flatten().filter { item ->
            item.questionEn.lowercase().contains(lowerQuery) ||
            item.questionKo.lowercase().contains(lowerQuery) ||
            item.answers.values.any { answer ->
                answer.answerEn.lowercase().contains(lowerQuery) ||
                answer.answerKo.lowercase().contains(lowerQuery)
            }
        }
    }

    override suspend fun navigateToIndex(index: Int) = mutex.withLock {
        val category = _currentCategory.value ?: return@withLock
        val items = itemsByCategory[category] ?: emptyList()
        if (index in 0 until items.size) {
            navigateTo(category, index)
        }
    }

    override fun release() {
        userLevelJob?.cancel()
        userLevelJob = null
        scope.cancel()
    }
}
