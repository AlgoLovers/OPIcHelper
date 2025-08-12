package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.entity.DataSource
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.LearningPreferencesRepository
import com.na982.opichelper.domain.repository.TtsSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val learningPreferencesRepository: LearningPreferencesRepository,
    private val ttsSettingsRepository: TtsSettingsRepository
) : ViewModel() {
    
    // 학습 설정 상태
    val userLevel = learningPreferencesRepository.userLevel
    val selectedDataSource = learningPreferencesRepository.selectedDataSource
    
    // TTS 설정 상태
    val englishTtsRate = ttsSettingsRepository.englishTtsRate
    
    // 학습 레벨 변경
    fun setUserLevel(level: UserLevel) {
        learningPreferencesRepository.setUserLevel(level)
    }
    
    // 데이터 소스 변경
    fun setDataSource(dataSource: DataSource) {
        learningPreferencesRepository.setDataSource(dataSource)
    }
    
    // TTS 속도 변경
    fun setEnglishTtsRate(rate: Float) {
        ttsSettingsRepository.setEnglishTtsRate(rate)
    }
    
    // 사용 가능한 학습 레벨 목록
    fun getAvailableUserLevels(): List<UserLevel> {
        return UserLevel.values().toList()
    }
    
    // 사용 가능한 데이터 소스 목록
    fun getAvailableDataSources(): List<DataSource> {
        return DataSource.values().toList()
    }
}
