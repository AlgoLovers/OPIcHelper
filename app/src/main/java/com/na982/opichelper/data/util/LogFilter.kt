package com.na982.opichelper.data.util

import android.util.Log

/**
 * 로그 필터링 및 관리 유틸리티
 * 
 * 암기레벨 영작테스트에서 녹음 파일 분석을 위한 로그 관리
 */
object LogFilter {
    
    // 로그 레벨 설정
    private const val LOG_LEVEL_INFO = true
    private const val LOG_LEVEL_DEBUG = false
    private const val LOG_LEVEL_VERBOSE = false
    
    // 특정 태그 필터링
    private val FILTERED_TAGS = setOf(
        "AudioFileManager",
        "EnglishWritingTest", 
        "RecordingFileRepositoryImpl",
        "FullMemorizationRepositoryImpl",
        "PlayRecordingUseCase"
    )
    
    // 녹음 관련 키워드
    private val RECORDING_KEYWORDS = setOf(
        "녹음", "recording", "merge", "병합", "파일", "file", "audio"
    )
    
    /**
     * 정보 로그 출력 (중요한 진행 상황)
     */
    fun i(tag: String, message: String) {
        if (LOG_LEVEL_INFO && shouldLog(tag, message)) {
            Log.i(tag, message)
        }
    }
    
    /**
     * 디버그 로그 출력 (상세 정보)
     */
    fun d(tag: String, message: String) {
        if (LOG_LEVEL_DEBUG && shouldLog(tag, message)) {
            Log.d(tag, message)
        }
    }
    
    /**
     * 상세 로그 출력 (매우 상세한 정보)
     */
    fun v(tag: String, message: String) {
        if (LOG_LEVEL_VERBOSE && shouldLog(tag, message)) {
            Log.v(tag, message)
        }
    }
    
    /**
     * 경고 로그 출력
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(tag, message)) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * 에러 로그 출력
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(tag, message)) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * 로그 출력 여부 결정
     */
    private fun shouldLog(tag: String, message: String): Boolean {
        // 필터링된 태그인지 확인
        val isFilteredTag = FILTERED_TAGS.any { tag.contains(it) }
        
        // 녹음 관련 키워드가 포함되어 있는지 확인
        val hasRecordingKeyword = RECORDING_KEYWORDS.any { 
            message.contains(it, ignoreCase = true) 
        }
        
        return isFilteredTag || hasRecordingKeyword
    }
    
    /**
     * 녹음 파일 분석 전용 로그
     */
    fun logRecordingAnalysis(tag: String, message: String) {
        if (message.contains("녹음") || message.contains("recording") || 
            message.contains("merge") || message.contains("병합")) {
            Log.i(tag, "[녹음분석] $message")
        }
    }
    
    /**
     * 파일 병합 과정 로그
     */
    fun logMergeProcess(tag: String, step: String, details: String = "") {
        val message = "[병합] $step${if (details.isNotEmpty()) " - $details" else ""}"
        Log.i(tag, message)
    }
    
    /**
     * 녹음 시간 정보 로그
     */
    fun logRecordingTime(tag: String, sentenceIndex: Int, duration: Long, type: String = "실제") {
        Log.i(tag, "[녹음시간] 문장${sentenceIndex + 1}: ${duration}ms ($type)")
    }
} 