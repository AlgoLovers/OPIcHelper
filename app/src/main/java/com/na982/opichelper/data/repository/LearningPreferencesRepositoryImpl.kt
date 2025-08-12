package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.entity.DataSource
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.LearningPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LearningPreferencesRepositoryImpl(private val context: Context) : LearningPreferencesRepository {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("learning_prefs", Context.MODE_PRIVATE)
    
    private val _userLevel = MutableStateFlow(UserLevel.IH)
    override val userLevel: StateFlow<UserLevel> = _userLevel
    
    private val _selectedDataSource = MutableStateFlow(DataSource.SAMPLE_SCRIPT)
    override val selectedDataSource: StateFlow<DataSource> = _selectedDataSource
    
    init {
        // 저장된 사용자 레벨 복원
        val savedLevel = prefs.getString("user_level", UserLevel.IH.name)
        _userLevel.value = UserLevel.valueOf(savedLevel ?: UserLevel.IH.name)
        
        // 저장된 데이터 소스 복원
        val savedDataSource = prefs.getString("data_source", DataSource.SAMPLE_SCRIPT.name)
        _selectedDataSource.value = DataSource.valueOf(savedDataSource ?: DataSource.SAMPLE_SCRIPT.name)
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
}
