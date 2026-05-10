package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import com.na982.opichelper.domain.entity.RepeatListeningData

interface RepeatListeningRepository {
    suspend fun executeRepeatListening(
        data: RepeatListeningData,
        uiCallback: RepeatListeningUiCallback,
        repeatCount: Int = 5
    )

    suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData?
    suspend fun updateProgress(progressData: ProgressData)
    suspend fun clearProgress(category: String, scriptIndex: Int)
}
