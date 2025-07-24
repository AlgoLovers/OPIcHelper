package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.ProgressRepository
import com.na982.opichelper.domain.repository.AppExitState
import com.na982.opichelper.domain.repository.CategoryProgress
import com.na982.opichelper.domain.repository.QaDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject

/**
 * 카테고리별 진행 상황 관리 및 복원을 담당하는 클래스
 * 책임: 카테고리별 진행 상황 저장/로드, 앱 종료 시 상태 저장, 테스트 완료 시 상태 삭제
 */
class MemorizeTestState @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val qaDataRepository: QaDataRepository
) {
    // 현재 앱 종료 상태 (하위 호환성을 위해 유지)
    private val _appExitState = MutableStateFlow<AppExitState?>(null)
    val appExitState: StateFlow<AppExitState?> = _appExitState.asStateFlow()
    
    // 모든 카테고리의 진행 상황
    private val _categoryProgressMap = MutableStateFlow<Map<String, CategoryProgress>>(emptyMap())
    val categoryProgressMap: StateFlow<Map<String, CategoryProgress>> = _categoryProgressMap.asStateFlow()
    
    // 진행 상태 존재 여부
    private val _hasProgress = MutableStateFlow(false)
    val hasProgress: StateFlow<Boolean> = _hasProgress.asStateFlow()
    
    /**
     * 앱 시작 시 모든 진행 상황 복원
     */
    suspend fun restoreAllProgress() {
        try {
            // 모든 카테고리 진행 상황 로드
            val allProgress = progressRepository.loadAllCategoryProgress()
            _categoryProgressMap.value = allProgress
            
            // 앱 종료 상태 로드 (하위 호환성)
            val appExitState = progressRepository.loadAppExitState()
            _appExitState.value = appExitState
            
            // 진행 상태 존재 여부 업데이트
            _hasProgress.value = allProgress.isNotEmpty() || appExitState != null
            
            Log.d("MemorizeTestState", "모든 진행 상황 복원 완료: ${allProgress.size}개 카테고리")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "진행 상황 복원 실패", e)
            _categoryProgressMap.value = emptyMap()
            _appExitState.value = null
            _hasProgress.value = false
        }
    }
    
    /**
     * 현재 카테고리의 진행 상황 가져오기
     */
    fun getCurrentCategoryProgress(category: String, scriptIndex: Int): CategoryProgress? {
        val key = "${category}_${scriptIndex}"
        return _categoryProgressMap.value[key]
    }
    
    /**
     * 현재 카테고리의 진행 상황 존재 여부 확인
     */
    fun hasCurrentCategoryProgress(category: String, scriptIndex: Int): Boolean {
        return getCurrentCategoryProgress(category, scriptIndex) != null
    }
    
    /**
     * 카테고리별 진행 상황 저장
     */
    suspend fun saveCategoryProgress(progress: CategoryProgress) {
        try {
            progressRepository.saveCategoryProgress(progress)
            
            // 메모리 상태 업데이트
            val currentMap = _categoryProgressMap.value.toMutableMap()
            currentMap[progress.getKey()] = progress
            _categoryProgressMap.value = currentMap
            
            // 진행 상태 존재 여부 업데이트
            _hasProgress.value = currentMap.isNotEmpty()
            
            Log.d("MemorizeTestState", "카테고리 진행 상황 저장: ${progress.getKey()}")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "카테고리 진행 상황 저장 실패", e)
        }
    }
    
    /**
     * 앱 종료 시 상태 저장 (하위 호환성)
     */
    suspend fun saveAppExitState(
        category: String,
        scriptIndex: Int,
        memorizeLevel: String,
        currentSentenceIndex: Int,
        totalSentences: Int,
        isMemorizeTestRunning: Boolean
    ) {
        try {
            // 카테고리별 진행 상황으로 저장
            val categoryProgress = CategoryProgress(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning
            )
            
            saveCategoryProgress(categoryProgress)
            
            // 하위 호환성을 위해 앱 종료 상태도 저장
            progressRepository.saveAppExitState(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning
            )
            
            val appExitState = AppExitState(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning
            )
            
            _appExitState.value = appExitState
            
            Log.d("MemorizeTestState", "앱 종료 상태 저장: $appExitState")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "앱 종료 상태 저장 실패", e)
        }
    }
    
    /**
     * 현재 문장 인덱스 업데이트
     */
    fun updateCurrentSentenceIndex(index: Int) {
        // 앱 종료 상태 업데이트 (하위 호환성)
        val currentState = _appExitState.value
        if (currentState != null) {
            val updatedState = currentState.copy(currentSentenceIndex = index)
            _appExitState.value = updatedState
            Log.d("MemorizeTestState", "현재 문장 인덱스 업데이트: $index")
        }
        
        // 카테고리 진행 상황도 업데이트
        val currentMap = _categoryProgressMap.value.toMutableMap()
        for ((key, progress) in currentMap) {
            if (progress.isMemorizeTestRunning) {
                val updatedProgress = progress.copy(currentSentenceIndex = index)
                currentMap[key] = updatedProgress
                Log.d("MemorizeTestState", "카테고리 진행 상황 업데이트: $key -> 인덱스 $index")
            }
        }
        _categoryProgressMap.value = currentMap
    }
    
    /**
     * 카테고리 진행 상황 삭제 (테스트 완료 시)
     */
    suspend fun clearCategoryProgress(category: String, scriptIndex: Int) {
        try {
            progressRepository.clearCategoryProgress(category, scriptIndex)
            
            // 메모리 상태 업데이트
            val currentMap = _categoryProgressMap.value.toMutableMap()
            val key = "${category}_${scriptIndex}"
            currentMap.remove(key)
            _categoryProgressMap.value = currentMap
            
            // 진행 상태 존재 여부 업데이트
            _hasProgress.value = currentMap.isNotEmpty()
            
            Log.d("MemorizeTestState", "카테고리 진행 상황 삭제: $key")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "카테고리 진행 상황 삭제 실패", e)
        }
    }
    
    /**
     * 모든 진행 상황 삭제
     */
    suspend fun clearAllProgress() {
        try {
            progressRepository.clearAllProgress()
            _categoryProgressMap.value = emptyMap()
            _appExitState.value = null
            _hasProgress.value = false
            
            Log.d("MemorizeTestState", "모든 진행 상황 삭제 완료")
        } catch (e: Exception) {
            Log.e("MemorizeTestState", "모든 진행 상황 삭제 실패", e)
        }
    }
    
    /**
     * 하위 호환성을 위한 메서드들
     */
    suspend fun restoreAppState() = restoreAllProgress()
    
    fun getCurrentAppState(): AppExitState? = _appExitState.value
    
    suspend fun clearProgress() = clearAllProgress()
} 