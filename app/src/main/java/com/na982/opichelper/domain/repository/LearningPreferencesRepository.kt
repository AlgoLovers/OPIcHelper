package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.DataSource
import com.na982.opichelper.domain.entity.UserLevel
import kotlinx.coroutines.flow.StateFlow

/**
 * 학습 관련 설정 관리 인터페이스
 * 학습 레벨과 데이터 소스만 담당
 */
interface LearningPreferencesRepository {
    /**
     * 사용자 레벨 가져오기
     */
    fun getUserLevel(): UserLevel
    
    /**
     * 사용자 레벨 설정
     */
    fun setUserLevel(level: UserLevel)
    
    /**
     * 사용자 레벨 StateFlow
     */
    val userLevel: StateFlow<UserLevel>
    
    /**
     * 데이터 소스 가져오기
     */
    fun getDataSource(): DataSource
    
    /**
     * 데이터 소스 설정
     */
    fun setDataSource(dataSource: DataSource)
    
    /**
     * 데이터 소스 StateFlow
     */
    val selectedDataSource: StateFlow<DataSource>
}
