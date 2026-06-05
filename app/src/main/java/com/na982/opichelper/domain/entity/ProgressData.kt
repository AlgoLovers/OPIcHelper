package com.na982.opichelper.domain.entity

data class CategoryProgress(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getKey(): String = "${category}_${scriptIndex}_${memorizeLevel}"
}
