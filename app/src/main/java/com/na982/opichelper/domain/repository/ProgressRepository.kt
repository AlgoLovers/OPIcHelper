package com.na982.opichelper.domain.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 진행 상황 저장/로드를 담당하는 Repository
 */
@Singleton
class ProgressRepository @Inject constructor(
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
            
            Log.d("ProgressRepository", "앱 종료 상태 저장: $appExitState")
        } catch (e: Exception) {
            Log.e("ProgressRepository", "앱 종료 상태 저장 실패", e)
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
                Log.d("ProgressRepository", "앱 종료 상태 로드: $appExitState")
                appExitState
            } else {
                Log.d("ProgressRepository", "저장된 앱 종료 상태 없음")
                null
            }
        } catch (e: Exception) {
            Log.e("ProgressRepository", "앱 종료 상태 로드 실패", e)
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
            
            Log.d("ProgressRepository", "카테고리 진행 상황 저장: ${progress.getKey()} -> $progress")
        } catch (e: Exception) {
            Log.e("ProgressRepository", "카테고리 진행 상황 저장 실패", e)
        }
    }
    
    /**
     * 카테고리별 진행 상황 로드
     */
    suspend fun loadCategoryProgress(category: String, scriptIndex: Int): CategoryProgress? {
        return try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}"
            val json = prefs.getString(key, null)
            if (json != null) {
                val progress = gson.fromJson(json, CategoryProgress::class.java)
                Log.d("ProgressRepository", "카테고리 진행 상황 로드: $key -> $progress")
                progress
            } else {
                Log.d("ProgressRepository", "저장된 카테고리 진행 상황 없음: $key")
                null
            }
        } catch (e: Exception) {
            Log.e("ProgressRepository", "카테고리 진행 상황 로드 실패", e)
            null
        }
    }
    
    /**
     * 모든 카테고리 진행 상황 로드
     */
    suspend fun loadAllCategoryProgress(): Map<String, CategoryProgress> {
        return try {
            val progressMap = mutableMapOf<String, CategoryProgress>()
            val allKeys = prefs.all.keys.filter { it.startsWith(KEY_CATEGORY_PROGRESS_PREFIX) }
            
            for (key in allKeys) {
                val json = prefs.getString(key, null)
                if (json != null) {
                    val progress = gson.fromJson(json, CategoryProgress::class.java)
                    progressMap[progress.getKey()] = progress
                }
            }
            
            Log.d("ProgressRepository", "모든 카테고리 진행 상황 로드: ${progressMap.size}개")
            progressMap
        } catch (e: Exception) {
            Log.e("ProgressRepository", "모든 카테고리 진행 상황 로드 실패", e)
            emptyMap()
        }
    }
    
    /**
     * 카테고리 진행 상황 삭제
     */
    suspend fun clearCategoryProgress(category: String, scriptIndex: Int) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}"
            prefs.edit().remove(key).apply()
            Log.d("ProgressRepository", "카테고리 진행 상황 삭제: $key")
        } catch (e: Exception) {
            Log.e("ProgressRepository", "카테고리 진행 상황 삭제 실패", e)
        }
    }
    
    /**
     * 모든 진행 상황 삭제
     */
    suspend fun clearAllProgress() {
        try {
            val allKeys = prefs.all.keys.filter { 
                it.startsWith(KEY_APP_EXIT_STATE) || it.startsWith(KEY_CATEGORY_PROGRESS_PREFIX) 
            }
            
            for (key in allKeys) {
                prefs.edit().remove(key).apply()
            }
            
            Log.d("ProgressRepository", "모든 진행 상황 삭제 완료")
        } catch (e: Exception) {
            Log.e("ProgressRepository", "모든 진행 상황 삭제 실패", e)
        }
    }
} 