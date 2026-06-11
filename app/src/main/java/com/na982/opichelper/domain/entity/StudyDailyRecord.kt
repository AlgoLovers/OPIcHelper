package com.na982.opichelper.domain.entity

data class StudyDailyRecord(
    val date: String,
    val studyDurationMs: Long = 0L,
    val completedScripts: Int = 0
)
