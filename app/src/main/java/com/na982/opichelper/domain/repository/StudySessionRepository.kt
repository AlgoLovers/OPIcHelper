package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.StudyDailyRecord

interface StudySessionRepository {
    fun recordSession(durationMs: Long, completedCount: Int)
    fun getDailyRecords(days: Int): List<StudyDailyRecord>
    fun getStreak(): Int
    fun getLongestStreak(): Int
    fun getTotalStudyDurationMs(): Long
    fun getTotalCompletedScripts(): Int
}
