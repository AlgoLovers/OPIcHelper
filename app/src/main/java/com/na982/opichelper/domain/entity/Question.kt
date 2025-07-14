package com.na982.opichelper.domain.entity

data class Question(
    val id: String = "",
    val question: String,
    val category: QuestionCategory,
    val sampleAnswer: String = "",
    val keywords: List<String> = emptyList(),
    val isFavorite: Boolean = false
)

enum class QuestionCategory {
    PERSONAL,
    WORK,
    EDUCATION,
    HOBBIES,
    TRAVEL,
    TECHNOLOGY,
    HEALTH,
    ENVIRONMENT,
    SOCIAL_ISSUES,
    OTHER
} 