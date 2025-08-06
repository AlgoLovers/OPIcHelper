package com.na982.opichelper.domain.manager

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
) : ICategoryManager {
    
    // 카테고리 관련 상태
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    override val categories: StateFlow<List<String>> = _categories.asStateFlow()
    
    private val _currentCategory = MutableStateFlow<String?>(null)
    override val currentCategory: StateFlow<String?> = _currentCategory.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        Log.d("CategoryManager", "카테고리 매니저 초기화")
        loadCategories()
    }
    
    /**
     * 카테고리 목록 로드
     */
    override fun loadCategories() {
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
    override fun changeCategory(category: String) {
        Log.d("CategoryManager", "카테고리 변경: $category")
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // 기존 작업 중단
                stopAllOperations()
                
                // 카테고리 변경 (Lazy Loading 사용)
                qaDataRepository.selectCategory(category)
                _currentCategory.value = category
                
                // AppStateManager 상태 업데이트
                val currentQaItem = qaDataRepository.getCurrentQaItem()
                val currentIndex = qaDataRepository.getCurrentIndex()
                val itemsInCategory = qaDataRepository.getItemsInCategory(category)
                
                appStateManager.updateQaItemState(
                    qaItem = currentQaItem,
                    category = category,
                    index = currentIndex,
                    totalCount = itemsInCategory.size
                )
                
                Log.d("CategoryManager", "카테고리 변경 완료: $category")
                
            } catch (e: Exception) {
                Log.e("CategoryManager", "카테고리 변경 실패", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * 특정 카테고리의 QA 아이템 로드 (기존 인터페이스 유지)
     */
    override fun loadQaItemsForCategory(category: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                Log.d("CategoryManager", "QA 아이템 로드 시작: $category")
                _isLoading.value = true
                _error.value = null
                
                val qaItems = loadQaItemsUseCase(category)
                
                if (qaItems.isNotEmpty()) {
                    Log.d("CategoryManager", "QA 아이템 로드 완료: ${qaItems.size}개")
                } else {
                    Log.w("CategoryManager", "QA 아이템이 없습니다: $category")
                    _error.value = "해당 카테고리에 QA 아이템이 없습니다."
                }
                
            } catch (e: Exception) {
                Log.e("CategoryManager", "QA 아이템 로드 실패", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 모든 작업 중단
     */
    private fun stopAllOperations() {
        Log.d("CategoryManager", "모든 작업 중단")
        // TTS 중지 등은 AudioControlViewModel에서 처리
        appStateManager.resetTtsState()
    }
    
    /**
     * 에러 상태 초기화
     */
    override fun clearError() {
        _error.value = null
    }
    
    /**
     * 상태 초기화
     */
    override fun resetState() {
        _categories.value = emptyList()
        _currentCategory.value = null
        _isLoading.value = false
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