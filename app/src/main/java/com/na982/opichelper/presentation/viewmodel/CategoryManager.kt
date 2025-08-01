package com.na982.opichelper.presentation.viewmodel

import android.util.Log
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.usecase.LoadCategoriesUseCase
import com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 카테고리 관리 전담 클래스
 * 책임: 카테고리 목록 관리, 카테고리 변경, QA 아이템 로딩
 */
@Singleton
class CategoryManager @Inject constructor(
    private val qaDataRepository: QaDataRepository,
    private val appStateManager: AppStateManager,
    private val loadCategoriesUseCase: LoadCategoriesUseCase,
    private val loadQaItemsUseCase: LoadQaItemsUseCase
) {
    
    // 카테고리 관련 상태
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()
    
    private val _currentCategory = MutableStateFlow<String?>(null)
    val currentCategory: StateFlow<String?> = _currentCategory.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        Log.d("CategoryManager", "카테고리 매니저 초기화")
        loadCategories()
    }
    
    /**
     * 카테고리 목록 로드
     */
    fun loadCategories() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                Log.d("CategoryViewModel", "카테고리 목록 로드 시작")
                _isLoading.value = true
                _error.value = null
                
                val categoriesList = loadCategoriesUseCase()
                _categories.value = categoriesList
                
                Log.d("CategoryViewModel", "카테고리 목록 로드 완료: ${categoriesList.size}개")
                
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "카테고리 목록 로드 실패", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 카테고리 변경
     */
    fun changeCategory(category: String) {
        Log.d("CategoryManager", "카테고리 변경: $category")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 기존 작업 중단
                stopAllOperations()
                
                // 카테고리 변경
                qaDataRepository.selectCategory(category)
                _currentCategory.value = category
                
                // QA 아이템 로드
                loadQaItemsForCategory(category)
                
                // 암기 모드 상태 초기화
                appStateManager.updateMemorizationModeState(
                    isRepeatListeningMode = false,
                    isEnglishWritingTestMode = false,
                    isFullMemorizationMode = false
                )
                
                Log.d("CategoryViewModel", "카테고리 변경 완료: $category")
                
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "카테고리 변경 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 특정 카테고리의 QA 아이템 로드
     */
    private suspend fun loadQaItemsForCategory(category: String) {
        Log.d("CategoryViewModel", "QA 아이템 로드 시작: $category")
        
        try {
            val qaItems = loadQaItemsUseCase(category)
            Log.d("CategoryViewModel", "QA 아이템 로드 완료: ${qaItems.size}개")
            
            // 첫 번째 아이템으로 초기화
            if (qaItems.isNotEmpty()) {
                val firstItem = qaItems.first()
                updateCurrentQaItem(firstItem, category, 0, qaItems.size)
            }
            
        } catch (e: Exception) {
            Log.e("CategoryViewModel", "QA 아이템 로드 실패", e)
            throw e
        }
    }
    
    /**
     * 현재 QA 아이템 업데이트
     */
    private fun updateCurrentQaItem(qaItem: QaItem, category: String, index: Int, totalCount: Int) {
        Log.d("CategoryViewModel", "QA 아이템 업데이트: $category, $index/$totalCount")
        
        appStateManager.updateQaItemState(
            qaItem = qaItem,
            category = category,
            index = index,
            totalCount = totalCount
        )
    }
    
    /**
     * 모든 작업 중단
     */
    private fun stopAllOperations() {
        Log.d("CategoryViewModel", "모든 작업 중단")
        // TTS 중지 등은 AudioControlViewModel에서 처리
        appStateManager.resetTtsState()
    }
    
    /**
     * 에러 상태 초기화
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 현재 카테고리 가져오기
     */
    fun getCurrentCategory(): String? {
        return _currentCategory.value
    }
    
    /**
     * 카테고리 목록 가져오기
     */
    fun getCategories(): List<String> {
        return _categories.value
    }
} 