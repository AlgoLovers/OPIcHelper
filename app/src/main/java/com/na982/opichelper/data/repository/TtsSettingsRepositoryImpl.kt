package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.repository.TtsSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TtsSettingsRepositoryImpl private constructor(private val context: Context) : TtsSettingsRepository {
    
    companion object {
        @Volatile
        private var INSTANCE: TtsSettingsRepositoryImpl? = null
        
        fun getInstance(context: Context): TtsSettingsRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TtsSettingsRepositoryImpl(context).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    private val _englishTtsRate = MutableStateFlow(1.0f)
    override val englishTtsRate: StateFlow<Float> = _englishTtsRate
    
    init {
        // 저장된 TTS 속도 복원 (기본값: 1.0f)
        val savedTtsRate = prefs.getFloat("english_tts_rate", 1.0f)
        _englishTtsRate.value = savedTtsRate
    }
    
    override fun getEnglishTtsRate(): Float {
        return _englishTtsRate.value
    }
    
    override fun setEnglishTtsRate(rate: Float) {
        _englishTtsRate.value = rate
        
        // SharedPreferences에 저장
        prefs.edit().apply {
            putFloat("english_tts_rate", rate)
            apply()
        }
    }
}
