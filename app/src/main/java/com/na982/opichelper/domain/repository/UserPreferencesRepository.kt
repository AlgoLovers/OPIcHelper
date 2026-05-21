package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.UserLevel
import kotlinx.coroutines.flow.StateFlow

/**
 * 사용자 설정 관리 인터페이스
 * Domain Layer에서 정의하여 의존성 역전 원칙 준수
 */
interface UserPreferencesRepository {
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
     * 영문 TTS 속도 가져오기
     */
    fun getEnglishTtsRate(): Float
    
    /**
     * 영문 TTS 속도 설정
     */
    fun setEnglishTtsRate(rate: Float)
    
    /**
     * 영문 TTS 속도 StateFlow
     */
    val englishTtsRate: StateFlow<Float>

    fun getMemorizeLevel(): String
    fun setMemorizeLevel(level: String)

    fun getRepeatListeningCount(): Int
    fun setRepeatListeningCount(count: Int)
    val repeatListeningCount: StateFlow<Int>

    fun getAnswerPlayCount(): Int
    fun setAnswerPlayCount(count: Int)
    val answerPlayCount: StateFlow<Int>

    fun isAutoAdvance(): Boolean
    fun setAutoAdvance(enabled: Boolean)

    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()

    fun isPipGuideCompleted(): Boolean
    fun setPipGuideCompleted()
} 