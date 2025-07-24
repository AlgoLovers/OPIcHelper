package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.ScriptProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject

/**
 * 암기 테스트 상태를 관리하는 클래스
 * 이 클래스는 MemorizeTestProgressTracker로 대체되었습니다.
 * 하위 호환성을 위해 유지하지만, 새로운 코드는 MemorizeTestProgressTracker를 사용해야 합니다.
 */
@Deprecated("Use MemorizeTestProgressTracker instead")
class MemorizeTestState @Inject constructor(
    private val progressPersistenceService: ProgressPersistenceService
) {
    // 현재 앱 종료 상태 (하위 호환성을 위해 유지)
    private val _appExitState = MutableStateFlow<com.na982.opichelper.domain.repository.AppExitState?>(null)
    val appExitState: StateFlow<com.na982.opichelper.domain.repository.AppExitState?> = _appExitState.asStateFlow()
    
    // 모든 카테고리의 진행 상황
    private val _categoryProgressMap = MutableStateFlow<Map<String, com.na982.opichelper.domain.repository.CategoryProgress>>(emptyMap())
    val categoryProgressMap: StateFlow<Map<String, com.na982.opichelper.domain.repository.CategoryProgress>> = _categoryProgressMap.asStateFlow()
    
    // 진행 상태 존재 여부
    private val _hasProgress = MutableStateFlow(false)
    val hasProgress: StateFlow<Boolean> = _hasProgress.asStateFlow()
    
    /**
     * 앱 시작 시 모든 진행 상황 복원
     */
    suspend fun restoreAllProgress() {
        try {
            // 모든 카테고리 진행 상황 로드
            val allProgress = progressPersistenceService.loadAllCategoryProgress()
            _categoryProgressMap.value = allProgress
            
            // 앱 종료 상태 로드 (하위 호환성)
            val appExitState = progressPersistenceService.loadAppExitState()
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
    fun getCurrentCategoryProgress(category: String, scriptIndex: Int): com.na982.opichelper.domain.repository.CategoryProgress? {
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
    suspend fun saveCategoryProgress(progress: com.na982.opichelper.domain.repository.CategoryProgress) {
        try {
            progressPersistenceService.saveCategoryProgress(progress)
            
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
            val categoryProgress = com.na982.opichelper.domain.repository.CategoryProgress(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning
            )
            
            saveCategoryProgress(categoryProgress)
            
            // 하위 호환성을 위해 앱 종료 상태도 저장
            progressPersistenceService.saveAppExitState(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning
            )
            
            val appExitState = com.na982.opichelper.domain.repository.AppExitState(
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
            progressPersistenceService.clearCategoryProgress(category, scriptIndex)
            
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
            progressPersistenceService.clearAllProgress()
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
    
    fun getCurrentAppState(): com.na982.opichelper.domain.repository.AppExitState? = _appExitState.value
    
    suspend fun clearProgress() = clearAllProgress()
} 