package com.na982.opichelper.domain.repository

interface StudySessionRecorder {
    fun recordSession(durationMs: Long, completedCount: Int)
}
