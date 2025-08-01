package com.na982.opichelper.domain.state

import android.util.Log
import com.na982.opichelper.domain.repository.CategoryProgress
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.ScriptProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 암기 테스트 진행 상황을 추적하는 싱글톤 클래스
 * 모든 스크립트의 진행 상황을 메모리에 보관하고, 앱 종료 시에만 변경된 항목을 저장
 */
@Singleton
class MemorizationProgressTracker @Inject constructor(
    private val progressPersistenceService: ProgressPersistenceService
) {
    // 모든 스크립트의 진행 상황 (메모리에서 관리)
    private val _progressMap = MutableStateFlow<Map<String, ScriptProgress>>(emptyMap())
    val progressMap: StateFlow<Map<String, ScriptProgress>> = _progressMap.asStateFlow()
    
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
            
            // ScriptProgress로 변환
            val scriptProgressMap = allProgress.mapValues { (_, categoryProgress) ->
                ScriptProgress(
                    category = categoryProgress.category,
                    scriptIndex = categoryProgress.scriptIndex,
                    memorizeLevel = categoryProgress.memorizeLevel,
                    currentSentenceIndex = categoryProgress.currentSentenceIndex,
                    totalSentences = categoryProgress.totalSentences,
                    isMemorizeTestRunning = categoryProgress.isMemorizeTestRunning,
                    timestamp = categoryProgress.timestamp,
                    needsSave = false // 로드된 데이터는 저장 불필요
                )
            }
            
            _progressMap.value = scriptProgressMap
            _hasProgress.value = scriptProgressMap.isNotEmpty()
            
            Log.d("MemorizationProgressTracker", "모든 진행 상황 복원 완료: ${scriptProgressMap.size}개 스크립트")
        } catch (e: Exception) {
            Log.e("MemorizationProgressTracker", "진행 상황 복원 실패", e)
            _progressMap.value = emptyMap()
            _hasProgress.value = false
        }
    }
    
    /**
     * 특정 스크립트의 진행 상황 가져오기 (암기레벨별)
     */
    fun getScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String): ScriptProgress? {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        return _progressMap.value[key]
    }
    
    /**
     * 특정 스크립트의 진행 상황 존재 여부 확인 (암기레벨별)
     */
    fun hasScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String): Boolean {
        return getScriptProgress(category, scriptIndex, memorizeLevel) != null
    }
    
    /**
     * 진행 상황 업데이트 (메모리에서만)
     */
    fun updateProgress(
        category: String,
        scriptIndex: Int,
        memorizeLevel: String,
        currentSentenceIndex: Int,
        totalSentences: Int,
        isMemorizeTestRunning: Boolean
    ) {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        val currentMap = _progressMap.value.toMutableMap()
        
        currentMap[key] = ScriptProgress(
            category = category,
            scriptIndex = scriptIndex,
            memorizeLevel = memorizeLevel,
            currentSentenceIndex = currentSentenceIndex,
            totalSentences = totalSentences,
            isMemorizeTestRunning = isMemorizeTestRunning,
            needsSave = true // 변경되었으므로 저장 필요
        )
        
        _progressMap.value = currentMap
        _hasProgress.value = true
        
        Log.d("MemorizationProgressTracker", "진행 상황 업데이트: $key -> 문장 ${currentSentenceIndex + 1}/${totalSentences}")
    }
    
    /**
     * 특정 스크립트의 진행 상황 삭제 (암기레벨별)
     */
    fun clearScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String) {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        val currentMap = _progressMap.value.toMutableMap()
        
        currentMap.remove(key)
        _progressMap.value = currentMap
        _hasProgress.value = currentMap.isNotEmpty()
        
        Log.d("MemorizationProgressTracker", "진행 상황 삭제: $key")
    }
    
    /**
     * 변경된 진행 상황만 저장
     */
    suspend fun persistChangedProgress() {
        try {
            val changedProgress = _progressMap.value.values.filter { it.needsSave }
            
            if (changedProgress.isNotEmpty()) {
                Log.d("MemorizationProgressTracker", "변경된 진행 상황 저장 시작: ${changedProgress.size}개")
                
                // 각 진행 상황을 저장
                changedProgress.forEach { scriptProgress ->
                    val categoryProgress = CategoryProgress(
                        category = scriptProgress.category,
                        scriptIndex = scriptProgress.scriptIndex,
                        memorizeLevel = scriptProgress.memorizeLevel,
                        currentSentenceIndex = scriptProgress.currentSentenceIndex,
                        totalSentences = scriptProgress.totalSentences,
                        isMemorizeTestRunning = scriptProgress.isMemorizeTestRunning
                    )
                    progressPersistenceService.saveCategoryProgress(categoryProgress)
                }
                
                // 저장 완료 후 needsSave 플래그 제거
                val updatedMap = _progressMap.value.mapValues { (_, progress) ->
                    progress.copy(needsSave = false)
                }
                _progressMap.value = updatedMap
                
                Log.d("MemorizationProgressTracker", "변경된 진행 상황 저장 완료")
            }
        } catch (e: Exception) {
            Log.e("MemorizationProgressTracker", "진행 상황 저장 실패", e)
        }
    }
    
    /**
     * 모든 진행 상황 저장 (앱 종료 시)
     */
    suspend fun persistAllProgress() {
        try {
            val allProgress = _progressMap.value.values
            
            if (allProgress.isNotEmpty()) {
                Log.d("MemorizationProgressTracker", "모든 진행 상황 저장 시작: ${allProgress.size}개")
                
                // 각 진행 상황을 저장
                allProgress.forEach { scriptProgress ->
                    val categoryProgress = CategoryProgress(
                        category = scriptProgress.category,
                        scriptIndex = scriptProgress.scriptIndex,
                        memorizeLevel = scriptProgress.memorizeLevel,
                        currentSentenceIndex = scriptProgress.currentSentenceIndex,
                        totalSentences = scriptProgress.totalSentences,
                        isMemorizeTestRunning = scriptProgress.isMemorizeTestRunning
                    )
                    progressPersistenceService.saveCategoryProgress(categoryProgress)
                }
                
                Log.d("MemorizationProgressTracker", "모든 진행 상황 저장 완료")
            }
        } catch (e: Exception) {
            Log.e("MemorizationProgressTracker", "모든 진행 상황 저장 실패", e)
        }
    }
    
    /**
     * 모든 진행 상황 초기화
     */
    fun clearAllProgress() {
        _progressMap.value = emptyMap()
        _hasProgress.value = false
        Log.d("MemorizationProgressTracker", "모든 진행 상황 초기화")
    }
} 