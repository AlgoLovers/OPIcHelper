package com.na982.opichelper.domain.usecase

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 설정 관리를 담당하는 Service
 * 책임: 사용자 설정 저장/로드, 기본값 관리
 */
@Singleton
class UserPreferencesService @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_LAST_CATEGORY = "last_category"
        private const val KEY_LAST_INDEX = "last_index"
        private const val KEY_LAST_MEMORIZE_LEVEL = "last_memorize_level"
    }
    
    fun saveLastCategory(category: String) {
        sharedPreferences.edit().putString(KEY_LAST_CATEGORY, category).apply()
    }
    
    fun saveLastIndex(index: Int) {
        sharedPreferences.edit().putInt(KEY_LAST_INDEX, index).apply()
    }
    
    fun saveLastMemorizeLevel(level: String) {
        sharedPreferences.edit().putString(KEY_LAST_MEMORIZE_LEVEL, level).apply()
    }
    
    fun getLastCategory(): String? {
        return sharedPreferences.getString(KEY_LAST_CATEGORY, null)
    }
    
    fun getLastIndex(): Int {
        return sharedPreferences.getInt(KEY_LAST_INDEX, 0)
    }
    
    fun getLastMemorizeLevel(): String? {
        return sharedPreferences.getString(KEY_LAST_MEMORIZE_LEVEL, null)
    }
} 