package com.na982.opichelper.domain.repository

import java.io.File

/**
 * 오디오 파일 관리를 담당하는 Repository
 */
interface AudioFileRepository {
    /**
     * 녹음된 파일들을 병합하여 저장
     * @param files 병합할 녹음 파일들
     * @param scriptId 스크립트 식별자 (병합된 파일명에 사용)
     * @return 병합된 파일
     */
    suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File?
    
    /**
     * 녹음 파일을 저장
     * @param recordedFile 녹음된 파일 경로
     */
    suspend fun saveRecording(recordedFile: String)
    
    /**
     * 최근 병합된 오디오 파일 가져오기
     * @return 병합된 오디오 파일 (없으면 null)
     */
    fun getLatestMergedAudioFile(): File?
    
    /**
     * 오디오 파일 삭제
     * @param file 삭제할 파일
     */
    fun deleteAudioFile(file: File)
    
    /**
     * 스크립트별 오래된 녹음 파일들 정리
     * @param scriptId 스크립트 식별자 (예: "category_index")
     * @param keepLatestCount 유지할 최신 파일 개수 (기본값: 1)
     */
    suspend fun cleanupOldRecordings(scriptId: String, keepLatestCount: Int = 1)
    
    /**
     * 모든 오래된 녹음 파일들 정리
     * @param keepLatestCount 각 스크립트당 유지할 최신 파일 개수 (기본값: 1)
     */
    suspend fun cleanupAllOldRecordings(keepLatestCount: Int = 1)
    
    /**
     * 특정 스크립트의 녹음 파일 존재 여부 확인
     * @param scriptId 스크립트 식별자 (예: "category_index")
     * @return 녹음 파일이 존재하면 true, 없으면 false
     */
    suspend fun hasRecordingFile(scriptId: String): Boolean
    
    /**
     * 녹음 파일 경로 생성
     * @param fileName 파일명
     * @return 전체 파일 경로
     */
    fun getRecordingFilePath(fileName: String): String
    
    /**
     * 특정 파일 경로의 녹음 파일 존재 여부 확인
     * @param filePath 파일 경로
     * @return 파일이 존재하면 true, 없으면 false
     */
    suspend fun hasRecordingFileByPath(filePath: String): Boolean
    
    /**
     * 특정 파일 경로의 녹음 파일 삭제
     * @param filePath 파일 경로
     * @return 삭제 성공하면 true, 실패하면 false
     */
    suspend fun deleteRecordingFileByPath(filePath: String): Boolean
}