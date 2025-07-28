package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.UserLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferencesRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    private val _userLevel = MutableStateFlow(UserLevel.IH)
    val userLevel: StateFlow<UserLevel> = _userLevel
    
    init {
        // 저장된 사용자 레벨 복원
        val savedLevel = prefs.getString("user_level", UserLevel.IH.name)
        _userLevel.value = UserLevel.valueOf(savedLevel ?: UserLevel.IH.name)
    }
    
    fun setUserLevel(level: UserLevel) {
        _userLevel.value = level
        
        // SharedPreferences에 저장
        prefs.edit().apply {
            putString("user_level", level.name)
            apply()
        }
    }
    
    fun getUserLevel(): UserLevel {
        return _userLevel.value
    }
} 