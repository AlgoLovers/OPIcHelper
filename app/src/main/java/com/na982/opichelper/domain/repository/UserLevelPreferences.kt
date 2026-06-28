package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.UserLevel
import kotlinx.coroutines.flow.StateFlow

interface UserLevelPreferences {
    fun getUserLevel(): UserLevel
    fun setUserLevel(level: UserLevel)
    val userLevel: StateFlow<UserLevel>
}
