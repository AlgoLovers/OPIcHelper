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
    fun getKey(): String = progressKey(category, scriptIndex, memorizeLevel)

    fun toPersistable(): ScriptProgress = copy(needsSave = false)

    companion object {
        fun progressKey(category: String, scriptIndex: Int, memorizeLevel: String): String =
            "${category}_${scriptIndex}_${memorizeLevel}"
    }
}
