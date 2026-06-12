package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.StudySessionRecorder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordStudySessionUseCase @Inject constructor(
    private val studySessionRecorder: StudySessionRecorder
) {
    @Volatile
    private var sessionStartTimeMs: Long = 0L

    fun startSession() {
        sessionStartTimeMs = System.currentTimeMillis()
    }

    fun endSession() {
        if (sessionStartTimeMs > 0L) {
            val durationMs = System.currentTimeMillis() - sessionStartTimeMs
            if (durationMs > 0L) {
                studySessionRecorder.recordSession(durationMs, 0)
            }
            sessionStartTimeMs = 0L
        }
    }
}
