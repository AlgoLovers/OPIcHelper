package com.na982.opichelper.domain.entity

/**
 * 반복 듣기 모드에서 사용할 데이터를 캡슐화하는 도메인 엔티티
 */
data class RepeatListeningData(
    val category: String,
    val scriptIndex: Int,
    val koreanAnswer: String,
    val englishAnswer: String
) 