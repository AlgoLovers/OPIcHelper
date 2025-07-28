package com.na982.opichelper.domain.entity

enum class UserLevel(val displayName: String, val description: String) {
    AL("AL (Advanced Low)", "가장 높은 등급 - 고급 영어 구사"),
    IH("IH (Intermediate High)", "중간 등급 - 중상급 영어 구사"),
    IH_RAW("IH_RAW (Intermediate High Raw)", "원본 중상급 영어 구사"),
    IM("IM (Intermediate Mid)", "기본 등급 - 중급 영어 구사")
} 