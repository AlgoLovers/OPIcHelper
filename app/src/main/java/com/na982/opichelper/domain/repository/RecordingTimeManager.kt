package com.na982.opichelper.domain.repository

/**
 * 영작테스트 녹음 시간 관리 인터페이스
 * 각 문장별 실제 녹음 시간을 저장하고 조회합니다.
 */
interface RecordingTimeManager {
    
    /**
     * 영작테스트 녹음 시간 저장
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param sentenceIndex 문장 인덱스
     * @param recordingTimeMs 녹음 시간 (밀리초)
     */
    fun saveRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int, recordingTimeMs: Long)
    
    /**
     * 영작테스트 녹음 시간 조회
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param sentenceIndex 문장 인덱스
     * @return 녹음 시간 (밀리초), 없으면 null
     */
    fun getRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int): Long?
    
    /**
     * 영작테스트의 모든 문장 녹음 시간 조회
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @return 문장별 녹음 시간 리스트 (인덱스 순서)
     */
    fun getAllRecordingTimes(category: String, scriptIndex: Int): List<Long>
    
    /**
     * 영작테스트 녹음 시간 존재 여부 확인
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @return 녹음 시간 데이터가 존재하면 true
     */
    fun hasRecordingTimes(category: String, scriptIndex: Int): Boolean
    
    /**
     * 영작테스트 녹음 시간 삭제
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     */
    fun clearRecordingTimes(category: String, scriptIndex: Int)
} 