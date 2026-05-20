package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.entity.CategoryProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 암기 테스트 진행 상황을 추적하는 싱글톤 클래스
 * 모든 스크립트의 진행 상황을 메모리에 보관하고, 앱 종료 시에만 변경된 항목을 저장
 */
@Singleton
class MemorizeTestProgressTracker @Inject constructor(
    private val progressPersistenceService: ProgressPersistenceService
) {
    private val mutex = Mutex()

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
            val allProgress = progressPersistenceService.loadAllCategoryProgress()

            val scriptProgressMap = allProgress.mapValues { (_, categoryProgress) ->
                ScriptProgress(
                    category = categoryProgress.category,
                    scriptIndex = categoryProgress.scriptIndex,
                    memorizeLevel = categoryProgress.memorizeLevel,
                    currentSentenceIndex = categoryProgress.currentSentenceIndex,
                    totalSentences = categoryProgress.totalSentences,
                    isMemorizeTestRunning = categoryProgress.isMemorizeTestRunning,
                    timestamp = categoryProgress.timestamp,
                    needsSave = false
                )
            }

            mutex.withLock {
                _progressMap.value = scriptProgressMap
                _hasProgress.value = scriptProgressMap.isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e("MemorizeTestProgressTracker", "진행 상황 복원 실패", e)
            mutex.withLock {
                _progressMap.value = emptyMap()
                _hasProgress.value = false
            }
        }
    }
    
    /**
     * 특정 스크립트의 진행 상황 가져오기 (암기레벨별)
     */
    suspend fun getScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String): ScriptProgress? {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        return mutex.withLock { _progressMap.value[key] }
    }
    
    /**
     * 특정 스크립트의 진행 상황 존재 여부 확인 (암기레벨별)
     */
    suspend fun hasScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String): Boolean {
        return getScriptProgress(category, scriptIndex, memorizeLevel) != null
    }
    
    /**
     * 진행 상황 업데이트 (메모리에서만)
     */
    suspend fun updateProgress(
        category: String,
        scriptIndex: Int,
        memorizeLevel: String,
        currentSentenceIndex: Int,
        totalSentences: Int,
        isMemorizeTestRunning: Boolean
    ) {
        val key = "${category}_${scriptIndex}_${memorizeLevel}"
        mutex.withLock {
            val currentMap = _progressMap.value.toMutableMap()

            currentMap[key] = ScriptProgress(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning,
                needsSave = true
            )

            _progressMap.value = currentMap
            _hasProgress.value = currentMap.isNotEmpty()
        }
    }
    
    /**
     * 앱 종료 시 변경된 진행 상황만 저장
     */
    suspend fun persistChangedProgress() {
        try {
            val changedProgress = mutex.withLock {
                _progressMap.value.values.filter { it.needsSave }
            }

            if (changedProgress.isNotEmpty()) {
                changedProgress.forEach { scriptProgress: ScriptProgress ->
                    val categoryProgress = CategoryProgress(
                        category = scriptProgress.category,
                        scriptIndex = scriptProgress.scriptIndex,
                        memorizeLevel = scriptProgress.memorizeLevel,
                        currentSentenceIndex = scriptProgress.currentSentenceIndex,
                        totalSentences = scriptProgress.totalSentences,
                        isMemorizeTestRunning = scriptProgress.isMemorizeTestRunning,
                        timestamp = scriptProgress.timestamp
                    )

                    progressPersistenceService.saveCategoryProgress(categoryProgress)
                }

                mutex.withLock {
                    val currentMap = _progressMap.value.toMutableMap()
                    changedProgress.forEach { scriptProgress: ScriptProgress ->
                        val key = scriptProgress.getKey()
                        currentMap[key] = scriptProgress.toPersistable()
                    }
                    _progressMap.value = currentMap
                }
            } else {
                // 저장할 진행 상황 없음
            }
        } catch (e: Exception) {
            Log.e("MemorizeTestProgressTracker", "진행 상황 저장 실패", e)
        }
    }
    
    /**
     * 특정 스크립트의 진행 상황 삭제 (암기레벨별)
     */
    suspend fun clearScriptProgress(category: String, scriptIndex: Int, memorizeLevel: String) {
        try {
            val key = "${category}_${scriptIndex}_${memorizeLevel}"
            mutex.withLock {
                val currentMap = _progressMap.value.toMutableMap()
                currentMap.remove(key)
                _progressMap.value = currentMap
                _hasProgress.value = currentMap.isNotEmpty()
            }

            progressPersistenceService.clearCategoryProgress(category, scriptIndex, memorizeLevel)
        } catch (e: Exception) {
            Log.e("MemorizeTestProgressTracker", "스크립트 진행 상황 삭제 실패", e)
        }
    }
    
    /**
     * 모든 진행 상황 삭제
     */
    suspend fun clearAllProgress() {
        try {
            progressPersistenceService.clearAllProgress()
            mutex.withLock {
                _progressMap.value = emptyMap()
                _hasProgress.value = false
            }
        } catch (e: Exception) {
            Log.e("MemorizeTestProgressTracker", "모든 진행 상황 삭제 실패", e)
        }
    }
} 