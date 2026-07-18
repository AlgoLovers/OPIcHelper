package com.na982.opichelper.domain.entity

import com.na982.opichelper.domain.entity.UserLevel

data class QaItem(
    val id: String,
    val category: String,
    val questionEn: String,
    val questionKo: String,
    val answers: Map<UserLevel, LeveledAnswer>, // 레벨별 답변
    val isModified: Boolean = false // 사용자가 스크립트를 원본에서 수정했는지 (Room 편집분에서만 true)
)

data class LeveledAnswer(
    val answerEn: String,
    val answerKo: String,
    val vocabulary: List<String> = emptyList(), // 레벨별 어휘
    val grammar: List<String> = emptyList(),   // 레벨별 문법 포인트
    val tips: List<String> = emptyList()       // 레벨별 학습 팁
) 