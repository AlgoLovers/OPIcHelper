package com.na982.opichelper.domain.repository

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
 * QA 데이터 관리 전담 클래스 (Manager 패턴)
 * 책임: QA 데이터 상태 관리, 카테고리 관리, 인덱스 관리, UI 상태 관리
 */
@Singleton
class QaDataManager @Inject constructor() {
    
    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()
    
    private var prefs: SharedPreferences? = null
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
                    Log.d("QaDataManager", "카테고리 로드 완료: $displayName (${items.size}개 항목)")
                } else {
                    Log.w("QaDataManager", "파일을 찾을 수 없음: $fileName")
                }
            } catch (e: Exception) {
                Log.e("QaDataManager", "카테고리 로드 실패: $displayName", e)
            }
        }
        
        _categories.value = categories
        Log.d("QaDataManager", "모든 카테고리 로드 완료: ${categories.size}개 카테고리")
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
    
    fun selectCategory(category: String) {
        if (itemsByCategory.containsKey(category)) {
            _currentCategory.value = category
            itemIndexByCategory[category] = 0
            updateCurrentQaItem()
            saveLastCategory(category)
            Log.d("QaDataManager", "카테고리 선택: $category")
        } else {
            Log.e("QaDataManager", "존재하지 않는 카테고리: $category")
        }
    }
    
    fun nextQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val items = itemsByCategory[category] ?: emptyList()
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (currentIndex < items.size - 1) {
                itemIndexByCategory[category] = currentIndex + 1
                updateCurrentQaItem()
                saveLastIndex(currentIndex + 1)
                Log.d("QaDataManager", "다음 항목으로 이동: ${currentIndex + 1}/${items.size}")
            } else {
                Log.d("QaDataManager", "마지막 항목에 도달")
            }
        }
    }
    
    fun previousQaItem() {
        val category = _currentCategory.value
        if (category != null) {
            val currentIndex = itemIndexByCategory[category] ?: 0
            
            if (currentIndex > 0) {
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
            
            if (currentIndex < items.size) {
                _currentQaItem.value = items[currentIndex]
                Log.d("QaDataManager", "현재 QA 항목 업데이트: ${items[currentIndex].questionEn}")
            } else {
                _currentQaItem.value = null
                Log.w("QaDataManager", "인덱스가 범위를 벗어남: $currentIndex >= ${items.size}")
            }
        } else {
            _currentQaItem.value = null
        }
    }
    
    private fun restoreLastCategory() {
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
    }
    
    fun clearError() {
        _error.value = null
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