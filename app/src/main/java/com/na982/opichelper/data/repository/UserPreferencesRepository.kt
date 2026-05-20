package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.UserPreferencesRepository as DomainUserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferencesRepository(private val context: Context) : DomainUserPreferencesRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userLevel = MutableStateFlow(UserLevel.IH)
    override val userLevel: StateFlow<UserLevel> = _userLevel

    private val _englishTtsRate = MutableStateFlow(0.8f)
    override val englishTtsRate: StateFlow<Float> = _englishTtsRate

    private val _repeatListeningCount = MutableStateFlow(5)
    override val repeatListeningCount: StateFlow<Int> = _repeatListeningCount

    private val _answerPlayCount = MutableStateFlow(1)
    override val answerPlayCount: StateFlow<Int> = _answerPlayCount

    init {
        val savedLevel = prefs.getString(KEY_USER_LEVEL, UserLevel.IH.name)
        _userLevel.value = UserLevel.valueOf(savedLevel ?: UserLevel.IH.name)

        _englishTtsRate.value = prefs.getFloat(KEY_ENGLISH_TTS_RATE, 0.8f)
        _repeatListeningCount.value = prefs.getInt(KEY_REPEAT_LISTENING_COUNT, 5)
        _answerPlayCount.value = prefs.getInt(KEY_ANSWER_PLAY_COUNT, 1)
    }

    override fun setUserLevel(level: UserLevel) {
        _userLevel.value = level
        prefs.edit().putString(KEY_USER_LEVEL, level.name).apply()
    }

    override fun getUserLevel(): UserLevel = _userLevel.value

    override fun setEnglishTtsRate(rate: Float) {
        _englishTtsRate.value = rate
        prefs.edit().putFloat(KEY_ENGLISH_TTS_RATE, rate).apply()
    }

    override fun getEnglishTtsRate(): Float = _englishTtsRate.value

    override fun getMemorizeLevel(): String {
        return prefs.getString(KEY_LAST_MEMORIZE_LEVEL, "") ?: ""
    }

    override fun setMemorizeLevel(level: String) {
        prefs.edit().putString(KEY_LAST_MEMORIZE_LEVEL, level).apply()
    }

    override fun getRepeatListeningCount(): Int = _repeatListeningCount.value

    override fun setRepeatListeningCount(count: Int) {
        _repeatListeningCount.value = count
        prefs.edit().putInt(KEY_REPEAT_LISTENING_COUNT, count).apply()
    }

    override fun getAnswerPlayCount(): Int = _answerPlayCount.value

    override fun setAnswerPlayCount(count: Int) {
        _answerPlayCount.value = count
        prefs.edit().putInt(KEY_ANSWER_PLAY_COUNT, count).apply()
    }

    override fun isAutoAdvance(): Boolean {
        return prefs.getBoolean(KEY_AUTO_ADVANCE, true)
    }

    override fun setAutoAdvance(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ADVANCE, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_USER_LEVEL = "user_level"
        private const val KEY_ENGLISH_TTS_RATE = "english_tts_rate"
        private const val KEY_LAST_MEMORIZE_LEVEL = "last_memorize_level"
        private const val KEY_REPEAT_LISTENING_COUNT = "repeat_listening_count"
        private const val KEY_ANSWER_PLAY_COUNT = "answer_play_count"
        private const val KEY_AUTO_ADVANCE = "auto_advance"
    }
}
