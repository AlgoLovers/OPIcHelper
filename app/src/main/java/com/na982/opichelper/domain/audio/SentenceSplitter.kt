package com.na982.opichelper.domain.audio

object SentenceSplitter {
    private val REGEX = Regex("(?<=[.!?。])\\s*")

    fun split(text: String): List<String> =
        text.split(REGEX).map { it.trim() }.filter { it.isNotEmpty() }
}
