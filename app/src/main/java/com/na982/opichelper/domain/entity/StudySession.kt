package com.na982.opichelper.domain.entity

import java.util.Date

data class StudySession(
    val id: Long = 0,
    val questionId: Long,
    val userAnswer: String,
    val score: Int,
    val feedback: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) 