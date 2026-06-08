package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.manager.AppLogger
import com.google.gson.Gson
import com.na982.opichelper.domain.entity.ScriptProgress
import com.na982.opichelper.domain.repository.ProgressPersistenceService

class ProgressPersistenceServiceImpl(
    private val context: Context,
    private val appLogger: AppLogger,
    private val gson: Gson
) : ProgressPersistenceService {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "opic_prefs"
        private const val KEY_CATEGORY_PROGRESS_PREFIX = "category_progress_"
        private const val KEY_NAV_CATEGORY = "last_category"
        private const val KEY_NAV_SCRIPT_INDEX = "last_script_index"
        private const val KEY_NAV_SENTENCE_INDEX = "last_sentence_index"
    }

    override suspend fun saveNavigationState(state: ProgressPersistenceService.NavigationState) {
        try {
            prefs.edit().apply {
                putString(KEY_NAV_CATEGORY, state.category)
                putInt(KEY_NAV_SCRIPT_INDEX, state.scriptIndex)
                putInt(KEY_NAV_SENTENCE_INDEX, state.sentenceIndex)
                apply()
            }
        } catch (e: Exception) {
            appLogger.e("ProgressPersistenceService", "네비게이션 상태 저장 실패", e)
        }
    }

    override suspend fun loadNavigationState(): ProgressPersistenceService.NavigationState {
        return try {
            ProgressPersistenceService.NavigationState(
                category = prefs.getString(KEY_NAV_CATEGORY, null),
                scriptIndex = prefs.getInt(KEY_NAV_SCRIPT_INDEX, -1),
                sentenceIndex = prefs.getInt(KEY_NAV_SENTENCE_INDEX, 0)
            )
        } catch (e: Exception) {
            appLogger.e("ProgressPersistenceService", "네비게이션 상태 로드 실패", e)
            ProgressPersistenceService.NavigationState(null, -1, 0)
        }
    }

    override suspend fun saveCategoryProgress(progress: ScriptProgress) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + progress.getKey()
            val json = gson.toJson(progress)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            appLogger.e("ProgressPersistenceService", "카테고리 진행 상황 저장 실패", e)
        }
    }

    override suspend fun loadAllCategoryProgress(): Map<String, ScriptProgress> {
        return try {
            val progressMap = mutableMapOf<String, ScriptProgress>()
            prefs.all.forEach { (key, value) ->
                if (key.startsWith(KEY_CATEGORY_PROGRESS_PREFIX)) {
                    try {
                        val json = value as String
                        val progress = gson.fromJson(json, ScriptProgress::class.java)
                        progressMap[progress.getKey()] = progress
                    } catch (_: Exception) {}
                }
            }
            progressMap
        } catch (e: Exception) {
            appLogger.e("ProgressPersistenceService", "모든 진행 상황 로드 실패", e)
            emptyMap()
        }
    }

    override suspend fun clearCategoryProgress(category: String, scriptIndex: Int, memorizeLevel: String) {
        try {
            val key = KEY_CATEGORY_PROGRESS_PREFIX + "${category}_${scriptIndex}_${memorizeLevel}"
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            appLogger.e("ProgressPersistenceService", "진행 상황 삭제 실패", e)
        }
    }
}
