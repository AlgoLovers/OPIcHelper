package com.na982.opichelper.domain.entity

data class ScriptProgress(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val needsSave: Boolean = false
) {
    fun getKey(): String = "${category}_${scriptIndex}_${memorizeLevel}"

    fun toPersistable(): ScriptProgress = copy(needsSave = false)
}
