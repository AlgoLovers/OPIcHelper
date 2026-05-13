package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@javax.inject.Singleton
class ExecuteEnglishWritingTestUseCase @Inject constructor(
    private val englishWritingTestRepository: EnglishWritingTestRepository
) {
    val events: SharedFlow<MemorizeTestEvent> get() = englishWritingTestRepository.events

    suspend fun execute(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int
    ) {
        englishWritingTestRepository.executeEnglishWritingTest(
            answerKo = answerKo,
            answerEn = answerEn,
            category = category,
            scriptIndex = scriptIndex
        )
    }
}
