package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.entity.RepeatListeningData
import kotlinx.coroutines.flow.SharedFlow

interface RepeatListeningRepository {
    val events: SharedFlow<MemorizeTestEvent>

    suspend fun executeRepeatListening(
        data: RepeatListeningData,
        repeatCount: Int = 5
    )

    suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData?
    suspend fun updateProgress(progressData: ProgressData)
    suspend fun clearProgress(category: String, scriptIndex: Int)
}
