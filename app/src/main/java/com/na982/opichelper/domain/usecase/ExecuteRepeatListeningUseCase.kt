package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@javax.inject.Singleton
class ExecuteRepeatListeningUseCase @Inject constructor(
    private val repeatListeningRepository: RepeatListeningRepository
) {
    val events: SharedFlow<MemorizeTestEvent> get() = repeatListeningRepository.events

    suspend fun execute(
        data: RepeatListeningData,
        repeatCount: Int = 5
    ) {
        repeatListeningRepository.executeRepeatListening(
            data = data,
            repeatCount = repeatCount
        )
    }

    suspend fun getResumeIndex(category: String, scriptIndex: Int, totalCount: Int): Int {
        return repeatListeningRepository.getResumeIndex(category, scriptIndex, totalCount)
    }

    suspend fun hasSavedProgress(category: String, scriptIndex: Int): Boolean {
        return repeatListeningRepository.hasSavedProgress(category, scriptIndex)
    }
}
