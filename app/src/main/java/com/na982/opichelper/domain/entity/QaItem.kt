package com.na982.opichelper.domain.entity

/**
 * QA 아이템 엔티티
 */
data class QaItem(
    val id: String,
    val category: String,
    val questionEn: String,
    val questionKo: String,
    val answers: Map<UserLevel, LeveledAnswer>,
    // 파싱된 문장 리스트들
    val questionEnSentences: List<String> = emptyList(), // 영어 질문 문장들
    val questionKoSentences: List<String> = emptyList(), // 한글 질문 문장들
    val answerEnSentences: List<String> = emptyList(),   // 영어 답변 문장들
    val answerKoSentences: List<String> = emptyList()    // 한글 답변 문장들
)

data class LeveledAnswer(
    val answerEn: String,
    val answerKo: String,
    val vocabulary: List<String> = emptyList(), // 레벨별 어휘
    val grammar: List<String> = emptyList(),   // 레벨별 문법 포인트
    val tips: List<String> = emptyList()       // 레벨별 학습 팁
) 