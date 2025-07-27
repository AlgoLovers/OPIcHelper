package com.na982.opichelper.domain.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 상황 저장/로드를 담당하는 서비스 (Persistence Service 패턴)
 * 책임: 진행 상황 데이터의 영속성 관리 (저장/로드/삭제)
 */
@Singleton
class ProgressPersistenceService @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("opic_progress", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_APP_EXIT_STATE = "app_exit_state"
        private const val KEY_CATEGORY_PROGRESS_PREFIX = "category_progress_"
    }
    
    /**
     * 앱 종료 상태 저장
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
            val appExitState = AppExitState(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = memorizeLevel,
                currentSentenceIndex = currentSentenceIndex,
                totalSentences = totalSentences,
                isMemorizeTestRunning = isMemorizeTestRunning
            )
            
            val json = gson.toJson(appExitState)
            prefs.edit().putString(KEY_APP_EXIT_STATE, json).apply()
            
            Log.d("ProgressPersistenceService", "앱 종료 상태 저장: $appExitState")
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "앱 종료 상태 저장 실패", e)
        }
    }
    
    /**
     * 앱 종료 상태 로드
     */
    suspend fun loadAppExitState(): AppExitState? {
        return try {
            val json = prefs.getString(KEY_APP_EXIT_STATE, null)
            if (json != null) {
                val appExitState = gson.fromJson(json, AppExitState::class.java)
                Log.d("ProgressPersistenceService", "앱 종료 상태 로드: $appExitState")
                appExitState
            } else {
                Log.d("ProgressPersistenceService", "저장된 앱 종료 상태 없음")
                null
            }
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "앱 종료 상태 로드 실패", e)
            null
        }
    }
    
    /**
     * 카테고리별 진행 상황 저장
     */
    suspend fun saveCategoryProgress(progress: CategoryProgress) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + progress.getKey()
            val json = gson.toJson(progress)
            prefs.edit().putString(key, json).apply()
            
            Log.d("ProgressPersistenceService", "카테고리 진행 상황 저장: ${progress.getKey()} -> $progress")
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "카테고리 진행 상황 저장 실패", e)
        }
    }
    
    /**
     * 카테고리별 진행 상황 로드 (암기레벨별)
     */
    suspend fun loadCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String): CategoryProgress? {
        return try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}_${memorizeLevel}"
            val json = prefs.getString(key, null)
            if (json != null) {
                val progress = gson.fromJson(json, CategoryProgress::class.java)
                Log.d("ProgressPersistenceService", "카테고리 진행 상황 로드: $key -> $progress")
                progress
            } else {
                Log.d("ProgressPersistenceService", "저장된 진행 상황 없음: $key")
                null
            }
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "카테고리 진행 상황 로드 실패", e)
            null
        }
    }
    
    /**
     * 모든 카테고리 진행 상황 로드
     */
    suspend fun loadAllCategoryProgress(): Map<String, CategoryProgress> {
        return try {
            val progressMap = mutableMapOf<String, CategoryProgress>()
            val allPrefs = prefs.all
            
            allPrefs.forEach { (key, value) ->
                if (key.startsWith(KEY_CATEGORY_PROGRESS_PREFIX)) {
                    try {
                        val json = value as String
                        val progress = gson.fromJson(json, CategoryProgress::class.java)
                        progressMap[progress.getKey()] = progress
                    } catch (e: Exception) {
                        Log.e("ProgressPersistenceService", "진행 상황 파싱 실패: $key", e)
                    }
                }
            }
            
            Log.d("ProgressPersistenceService", "모든 진행 상황 로드 완료: ${progressMap.size}개")
            progressMap
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "모든 진행 상황 로드 실패", e)
            emptyMap()
        }
    }
    
    /**
     * 특정 카테고리 진행 상황 삭제 (암기레벨별)
     */
    suspend fun clearCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}_${memorizeLevel}"
            prefs.edit().remove(key).apply()
            Log.d("ProgressPersistenceService", "진행 상황 삭제: $key")
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "진행 상황 삭제 실패", e)
        }
    }
    
    /**
     * 모든 진행 상황 삭제
     */
    suspend fun clearAllProgress() {
        try {
            val allPrefs = prefs.all
            val editor = prefs.edit()
            
            allPrefs.forEach { (key, _) ->
                if (key.startsWith(KEY_CATEGORY_PROGRESS_PREFIX) || key == KEY_APP_EXIT_STATE) {
                    editor.remove(key)
                }
            }
            
            editor.apply()
            Log.d("ProgressPersistenceService", "모든 진행 상황 삭제 완료")
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "모든 진행 상황 삭제 실패", e)
        }
    }
} 