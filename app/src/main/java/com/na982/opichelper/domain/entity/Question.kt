package com.na982.opichelper.domain.entity

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

// 질문/답변 쌍을 모두 포함하는 데이터 모델
data class QaItem(
    val id: String = "",
    val category: String, // 카테고리명을 문자열로 관리
    val questionEn: String,
    val questionKo: String = "",
    val answerEn: String = "",
    val answerKo: String = "",
    val keywords: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val recordingPath: String? = null // 녹음 파일 경로 추가
) 