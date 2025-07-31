package com.na982.opichelper.domain.entity

data class QaItem(
    val id: String,
    val category: String,
    val questionEn: String,
    val questionKo: String,
    val answers: Map<UserLevel, LeveledAnswer> // 레벨별 답변
)

data class LeveledAnswer(
    val answerEn: String,
    val answerKo: String,
    val vocabulary: List<String> = emptyList(), // 레벨별 어휘
    val grammar: List<String> = emptyList(),   // 레벨별 문법 포인트
    val tips: List<String> = emptyList()       // 레벨별 학습 팁
) 