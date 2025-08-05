package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.DataSource
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    private val _userLevel = MutableStateFlow(UserLevel.IH)
    override val userLevel: StateFlow<UserLevel> = _userLevel
    
    private val _selectedDataSource = MutableStateFlow(DataSource.SAMPLE_SCRIPT)
    override val selectedDataSource: StateFlow<DataSource> = _selectedDataSource
    
    private val _englishTtsRate = MutableStateFlow(1.0f)
    override val englishTtsRate: StateFlow<Float> = _englishTtsRate
    
    init {
        // 저장된 사용자 레벨 복원
        val savedLevel = prefs.getString("user_level", UserLevel.IH.name)
        _userLevel.value = UserLevel.valueOf(savedLevel ?: UserLevel.IH.name)
        
        // 저장된 데이터 소스 복원
        val savedDataSource = prefs.getString("data_source", DataSource.SAMPLE_SCRIPT.name)
        _selectedDataSource.value = DataSource.valueOf(savedDataSource ?: DataSource.SAMPLE_SCRIPT.name)
        
        // 저장된 TTS 속도 복원
        val savedTtsRate = prefs.getFloat("english_tts_rate", 1.0f)
        _englishTtsRate.value = savedTtsRate
    }
    
    override fun setUserLevel(level: UserLevel) {
        _userLevel.value = level
        
        // SharedPreferences에 저장
        prefs.edit().apply {
            putString("user_level", level.name)
            apply()
        }
    }
    
    override fun getUserLevel(): UserLevel {
        return _userLevel.value
    }
    
    override fun setDataSource(dataSource: DataSource) {
        _selectedDataSource.value = dataSource
        
        // SharedPreferences에 저장
        prefs.edit().apply {
            putString("data_source", dataSource.name)
            apply()
        }
    }
    
    override fun getDataSource(): DataSource {
        return _selectedDataSource.value
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