package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.na982.opichelper.domain.repository.OnboardingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    fun isOnboardingCompleted(): Boolean =
        onboardingPreferences.isOnboardingCompleted()

    fun setOnboardingCompleted() {
        onboardingPreferences.setOnboardingCompleted()
    }

    fun isPipGuideCompleted(): Boolean =
        onboardingPreferences.isPipGuideCompleted()

    fun setPipGuideCompleted() {
        onboardingPreferences.setPipGuideCompleted()
    }
}
