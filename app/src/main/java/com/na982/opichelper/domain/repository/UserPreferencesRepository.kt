package com.na982.opichelper.domain.repository

interface UserPreferencesRepository :
    UserLevelPreferences,
    TtsPreferences,
    PlaybackPreferences,
    OnboardingPreferences,
    MemorizeLevelPreferences,
    AppDataPreferences
