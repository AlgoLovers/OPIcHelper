package com.na982.opichelper.domain.usecase

import android.content.SharedPreferences
import javax.inject.Inject

/**
 * 사용자 설정 저장을 담당하는 UseCase
 */
class SaveUserPreferencesUseCase @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun saveLastCategory(category: String) {
        sharedPreferences.edit().putString("last_category", category).apply()
    }
    
    fun saveLastIndex(index: Int) {
        sharedPreferences.edit().putInt("last_index", index).apply()
    }
    
    fun saveLastMemorizeLevel(level: String) {
        sharedPreferences.edit().putString("last_memorize_level", level).apply()
    }
    
    fun getLastCategory(): String? {
        return sharedPreferences.getString("last_category", null)
    }
    
    fun getLastIndex(): Int {
        return sharedPreferences.getInt("last_index", 0)
    }
    
    fun getLastMemorizeLevel(): String? {
        return sharedPreferences.getString("last_memorize_level", null)
    }
} 