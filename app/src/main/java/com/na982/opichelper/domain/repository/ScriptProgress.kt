package com.na982.opichelper.domain.repository

/**
 * 스크립트별 진행 상황 정보
 */
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
    /**
     * 카테고리, 스크립트 인덱스, 암기레벨로 고유 키 생성
     */
    fun getKey(): String = "${category}_${scriptIndex}_${memorizeLevel}"
    
    /**
     * 저장용 복사본 생성 (needsSave 플래그 제거)
     */
    fun toPersistable(): ScriptProgress = copy(needsSave = false)
} 