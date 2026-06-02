package com.na982.opichelper.domain.repository

interface MemorizeLevelPreferences {
    fun getMemorizeLevel(): String
    fun setMemorizeLevel(level: String)
}
