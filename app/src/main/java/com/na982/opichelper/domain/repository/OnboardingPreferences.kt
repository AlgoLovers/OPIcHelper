package com.na982.opichelper.domain.repository

interface OnboardingPreferences {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted()
    fun isPipGuideCompleted(): Boolean
    fun setPipGuideCompleted()
}
