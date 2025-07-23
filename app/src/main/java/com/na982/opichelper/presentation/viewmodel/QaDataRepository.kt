package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * QA 데이터 관리 전담 클래스 (Repository 패턴)
 * 책임: QA 데이터 로딩, 카테고리 관리, 인덱스 관리, 프리퍼런스 저장/로드
 */
@Singleton
class QaDataRepository @Inject constructor() {
    
    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()
    
    private var prefs: SharedPreferences? = null
    private val PREF_KEY_LAST_CATEGORY = "last_category"
    private val PREF_KEY_LAST_INDEX = "last_index"
    
    // UI 상태
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
    
    fun init(application: Application) {
        prefs = application.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
        loadQaItemsFromAssets(application)
        restoreLastCategory()
    }
    
    fun loadQaItemsFromAssets(application: Application) {
        val context = application
        val gson = Gson()
        // Define the desired category order and display names
        val categoryDisplayNames = listOf(
            "집", "음악", "집에서 보내는 휴가", "영화", "레스토랑", "해변", "인터넷", "산업,커리어", "은행", "교통", "패션", "가족,친구", "가구", "예약", "명절"
        )
        // Map display names to asset file names (for 레스토랑, use restaurants asset)
        val categoryAssetMap = mapOf(
            "집" to "home",
            "음악" to "music",
            "집에서 보내는 휴가" to "home_vacation",
            "영화" to "movie",
            "레스토랑" to "restaurants",
            "해변" to "beach",
            "인터넷" to "internet",
            "산업,커리어" to "industry_career",
            "은행" to "bank",
            "교통" to "transportation",
            "패션" to "fashion",
            "가족,친구" to "family_friends",
            "가구" to "furniture",
            "예약" to "reservation",
            "명절" to "holiday"
        )
        val categories = categoryDisplayNames
        for (displayName in categoryDisplayNames) {
            val assetKey = categoryAssetMap[displayName] ?: displayName
            val fileName = "qa_${assetKey}.json"
            try {
                val assetList = context.assets.list("")?.toList() ?: emptyList()
                if (assetList.contains(fileName)) {
                    val inputStream = context.assets.open(fileName)
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<List<QaItemAsset>>() {}.type
                    val assetItems: List<QaItemAsset> = gson.fromJson(reader, type)
                    val items = assetItems.map {
                        QaItem(
                            id = it.id ?: "",
                            category = displayName,
                            questionEn = it.question_en,
                            questionKo = it.question_ko,
                            answerEn = it.answer_en,
                            answerKo = it.answer_ko
                        )
                    }
                    itemsByCategory[displayName] = items
                    itemIndexByCategory[displayName] = 0
                } else {
                    // No asset file yet, initialize with empty list
                    itemsByCategory[displayName] = emptyList()
                    itemIndexByCategory[displayName] = 0
                }
            } catch (e: Exception) {
                itemsByCategory[displayName] = emptyList()
                itemIndexByCategory[displayName] = 0
            }
        }
        _categories.value = categories
    }
    
    fun selectCategory(category: String) {
        _isLoading.value = true
        _error.value = null
        
        val items = itemsByCategory[category] ?: emptyList()
        val index = itemIndexByCategory[category] ?: 0
        
        prefs?.edit()?.putString(PREF_KEY_LAST_CATEGORY, category)?.apply()
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, index)?.apply()
        
        if (items.isNotEmpty()) {
            _currentQaItem.value = items[index]
            _currentCategory.value = category
        } else {
            _currentQaItem.value = null
            _currentCategory.value = category
            _error.value = "해당 카테고리에 질문이 없습니다."
        }
        
        _isLoading.value = false
    }
    
    fun nextQaItem() {
        val category = _currentCategory.value ?: return
        val items = itemsByCategory[category] ?: return
        if (items.isEmpty()) {
            Log.w("QaDataRepository", "Items list is empty for category: $category")
            return
        }
        
        val currentIndex = itemIndexByCategory[category] ?: 0
        val nextIndex = (currentIndex + 1) % items.size
        
        Log.d("QaDataRepository", "Moving from index $currentIndex to $nextIndex in category $category")
        
        itemIndexByCategory[category] = nextIndex
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, nextIndex)?.apply()
        _currentQaItem.value = items[nextIndex]
        _error.value = null
        
        Log.d("QaDataRepository", "Successfully moved to next question: ${items[nextIndex].questionEn.take(50)}...")
    }
    
    fun previousQaItem() {
        val category = _currentCategory.value ?: return
        val items = itemsByCategory[category] ?: return
        if (items.isEmpty()) {
            Log.w("QaDataRepository", "Items list is empty for category: $category")
            return
        }
        
        val currentIndex = itemIndexByCategory[category] ?: 0
        val previousIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
        
        Log.d("QaDataRepository", "Moving from index $currentIndex to $previousIndex in category $category")
        
        itemIndexByCategory[category] = previousIndex
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, previousIndex)?.apply()
        _currentQaItem.value = items[previousIndex]
        _error.value = null
        
        Log.d("QaDataRepository", "Successfully moved to previous question: ${items[previousIndex].questionEn.take(50)}...")
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun setError(error: String) {
        _error.value = error
    }
    
    fun getCurrentIndex(): Int {
        val category = _currentCategory.value ?: return 0
        return itemIndexByCategory[category] ?: 0
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
    
    private fun restoreLastCategory() {
        val lastCategory = prefs?.getString(PREF_KEY_LAST_CATEGORY, null)
        val lastIndex = prefs?.getInt(PREF_KEY_LAST_INDEX, 0) ?: 0
        if (lastCategory != null && itemsByCategory.containsKey(lastCategory)) {
            val items = itemsByCategory[lastCategory] ?: emptyList()
            val safeIndex = if (items.isNotEmpty()) lastIndex.coerceIn(0, items.size - 1) else 0
            itemIndexByCategory[lastCategory] = safeIndex
            if (items.isNotEmpty()) {
                _currentQaItem.value = items[safeIndex]
                _currentCategory.value = lastCategory
            }
        }
    }
    
    data class QaItemAsset(
        val id: String? = null,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )
} 