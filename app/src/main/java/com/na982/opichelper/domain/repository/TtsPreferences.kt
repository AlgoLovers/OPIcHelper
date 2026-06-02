package com.na982.opichelper.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface TtsPreferences {
    fun getEnglishTtsRate(): Float
    fun setEnglishTtsRate(rate: Float)
    val englishTtsRate: StateFlow<Float>
}
