package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.UserPreferencesRepository as DomainUserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferencesRepository(private val context: Context) : DomainUserPreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userLevel = MutableStateFlow(UserLevel.IH)
    override val userLevel: StateFlow<UserLevel> = _userLevel

    private val _englishTtsRate = MutableStateFlow(0.8f)
    override val englishTtsRate: StateFlow<Float> = _englishTtsRate

    init {
        val savedLevel = prefs.getString("user_level", UserLevel.IH.name)
        _userLevel.value = UserLevel.valueOf(savedLevel ?: UserLevel.IH.name)

        val savedRate = prefs.getFloat("english_tts_rate", 0.8f)
        _englishTtsRate.value = savedRate
    }

    override fun setUserLevel(level: UserLevel) {
        _userLevel.value = level
        prefs.edit().putString("user_level", level.name).apply()
    }

    override fun getUserLevel(): UserLevel = _userLevel.value

    override fun setEnglishTtsRate(rate: Float) {
        _englishTtsRate.value = rate
        prefs.edit().putFloat("english_tts_rate", rate).apply()
    }

    override fun getEnglishTtsRate(): Float = _englishTtsRate.value

    override fun getMemorizeLevel(): String {
        return prefs.getString("last_memorize_level", "") ?: ""
    }

    override fun setMemorizeLevel(level: String) {
        prefs.edit().putString("last_memorize_level", level).apply()
    }
}
