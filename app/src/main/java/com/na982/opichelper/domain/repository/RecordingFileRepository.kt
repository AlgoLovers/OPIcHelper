package com.na982.opichelper.domain.repository

import java.io.File

/**
 * 녹음 파일 관리를 전담하는 Repository
 * 책임: 녹음 파일의 존재 확인, 경로 관리, 재생 상태 관리
 */
interface RecordingFileRepository {
    /**
     * 현재 스크립트의 녹음 파일 존재 여부 확인
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @return 녹음 파일이 존재하면 true
     */
    suspend fun hasRecordingFile(category: String, scriptIndex: Int): Boolean
    
    /**
     * 현재 스크립트의 녹음 파일 경로 가져오기
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @return 녹음 파일 경로 (없으면 null)
     */
    suspend fun getRecordingFilePath(category: String, scriptIndex: Int): String?
    
    /**
     * 녹음 파일 생성 (새 녹음 시작)
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @return 생성된 파일 경로
     */
    suspend fun createRecordingFile(category: String, scriptIndex: Int): String
    
    /**
     * 녹음 파일 재생 (하이라이트 없음)
     * @param category 카테고리
     * @param scriptIndex 스크립트 인덱스
     * @param onPlayingStateChange 재생 상태 변경 콜백
     */
    suspend fun playRecordingFileSimple(
        category: String,
        scriptIndex: Int,
        onPlayingStateChange: (Boolean) -> Unit
    )
    
} 