package com.na982.opichelper.presentation.viewmodel

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject

/**
 * QA 데이터 상태 관리 전담 클래스
 * 책임: 카테고리 선택, 질문 네비게이션, QA 데이터 관리
 */
class QaStateManager @Inject constructor() {
    
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
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
    }
    
    fun setQaItems(items: Map<String, List<QaItem>>) {
        itemsByCategory.clear()
        itemsByCategory.putAll(items)
        _categories.value = items.keys.toList()
        restoreLastCategory()
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
        if (items.isEmpty()) return
        
        val currentIndex = itemIndexByCategory[category] ?: 0
        val nextIndex = (currentIndex + 1) % items.size
        
        itemIndexByCategory[category] = nextIndex
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, nextIndex)?.apply()
        _currentQaItem.value = items[nextIndex]
        _error.value = null
    }
    
    fun previousQaItem() {
        val category = _currentCategory.value ?: return
        val items = itemsByCategory[category] ?: return
        if (items.isEmpty()) return
        
        val currentIndex = itemIndexByCategory[category] ?: 0
        val previousIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
        
        itemIndexByCategory[category] = previousIndex
        prefs?.edit()?.putInt(PREF_KEY_LAST_INDEX, previousIndex)?.apply()
        _currentQaItem.value = items[previousIndex]
        _error.value = null
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
} 