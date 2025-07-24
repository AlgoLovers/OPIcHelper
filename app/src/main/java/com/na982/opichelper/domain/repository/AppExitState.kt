package com.na982.opichelper.domain.repository

import com.google.gson.annotations.SerializedName

/**
 * 앱 종료 시 상태 정보
 */
data class AppExitState(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 카테고리별 진행 상황 정보
 */
data class CategoryProgress(
    val category: String,
    val scriptIndex: Int,
    val memorizeLevel: String,
    val currentSentenceIndex: Int,
    val totalSentences: Int,
    val isMemorizeTestRunning: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 카테고리와 스크립트 인덱스로 고유 키 생성
     */
    fun getKey(): String = "${category}_${scriptIndex}"
} 