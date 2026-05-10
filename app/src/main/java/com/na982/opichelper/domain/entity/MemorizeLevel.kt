package com.na982.opichelper.domain.entity

enum class MemorizeLevel(val displayName: String) {
    REPEAT_LISTENING("반복 듣기"),
    ENGLISH_WRITING("영작 테스트"),
    FULL_MEMORIZATION("통암기");

    companion object {
        fun fromDisplayName(name: String): MemorizeLevel =
            entries.find { it.displayName == name } ?: REPEAT_LISTENING

        val allDisplayNames: List<String> = entries.map { it.displayName }
    }
}
