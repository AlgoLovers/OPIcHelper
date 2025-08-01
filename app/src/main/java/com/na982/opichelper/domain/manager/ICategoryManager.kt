package com.na982.opichelper.domain.manager

import kotlinx.coroutines.flow.StateFlow

/**
 * 카테고리 관리 인터페이스
 * 책임: 카테고리 목록 관리, 카테고리 변경, QA 아이템 로딩
 */
interface ICategoryManager {
    
    // 상태 노출
    val categories: StateFlow<List<String>>
    val currentCategory: StateFlow<String?>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    // 카테고리 관리 기능
    fun loadCategories()
    fun changeCategory(category: String)
    fun loadQaItemsForCategory(category: String)
    
    // 상태 초기화
    fun clearError()
    fun resetState()
} 