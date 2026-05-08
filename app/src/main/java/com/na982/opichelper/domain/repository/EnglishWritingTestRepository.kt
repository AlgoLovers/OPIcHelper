package com.na982.opichelper.domain.repository

interface EnglishWritingTestRepository {
    suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int,
        onCardFlip: (Boolean) -> Unit,
        onKoreanHighlight: (Int?) -> Unit,
        onRecordingHighlight: (Int?) -> Unit,
        onRecordingStateChange: (Boolean) -> Unit,
        onMergedFileCreated: () -> Unit
    )

    suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData?
    suspend fun updateProgress(progressData: ProgressData)
    suspend fun clearProgress(category: String, scriptIndex: Int)
}

data class ProgressData(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean
)
