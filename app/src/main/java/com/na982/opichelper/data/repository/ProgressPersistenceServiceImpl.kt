package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.na982.opichelper.domain.entity.AppExitState
import com.na982.opichelper.domain.entity.CategoryProgress
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressPersistenceServiceImpl @Inject constructor(
    private val context: Context
) : ProgressPersistenceService {
    private val prefs: SharedPreferences = context.getSharedPreferences("opic_progress", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_APP_EXIT_STATE = "app_exit_state"
        private const val KEY_CATEGORY_PROGRESS_PREFIX = "category_progress_"
    }

    override suspend fun saveAppExitState(
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
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "앱 종료 상태 저장 실패", e)
        }
    }

    override suspend fun loadAppExitState(): AppExitState? {
        return try {
            val json = prefs.getString(KEY_APP_EXIT_STATE, null)
            if (json != null) gson.fromJson(json, AppExitState::class.java) else null
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "앱 종료 상태 로드 실패", e)
            null
        }
    }

    override suspend fun saveCategoryProgress(progress: CategoryProgress) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + progress.getKey()
            val json = gson.toJson(progress)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "카테고리 진행 상황 저장 실패", e)
        }
    }

    override suspend fun loadCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String): CategoryProgress? {
        return try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}_${memorizeLevel}"
            val json = prefs.getString(key, null)
            if (json != null) gson.fromJson(json, CategoryProgress::class.java) else null
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "카테고리 진행 상황 로드 실패", e)
            null
        }
    }

    override suspend fun loadAllCategoryProgress(): Map<String, CategoryProgress> {
        return try {
            val progressMap = mutableMapOf<String, CategoryProgress>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(KEY_CATEGORY_PROGRESS_PREFIX)) {
                    try {
                        val json = value as String
                        val progress = gson.fromJson(json, CategoryProgress::class.java)
                        progressMap[progress.getKey()] = progress
                    } catch (_: Exception) {}
                }
            }
            progressMap
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "모든 진행 상황 로드 실패", e)
            emptyMap()
        }
    }

    override suspend fun clearCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}_${memorizeLevel}"
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "진행 상황 삭제 실패", e)
        }
    }

    override suspend fun clearAllProgress() {
        try {
            val editor = prefs.edit()
            prefs.all.forEach { (key, _) ->
                if (key.startsWith(KEY_CATEGORY_PROGRESS_PREFIX) || key == KEY_APP_EXIT_STATE) {
                    editor.remove(key)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e("ProgressPersistenceService", "모든 진행 상황 삭제 실패", e)
        }
    }
}
