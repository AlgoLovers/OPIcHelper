package com.na982.opichelper.domain.repository

import java.io.File

/**
 * 오디오 파일 관리를 담당하는 Repository
 */
interface AudioFileRepository {
    /**
     * 녹음된 파일들을 병합하여 저장
     * @param files 병합할 녹음 파일들
     * @return 병합된 파일
     */
    suspend fun mergeAndSaveAudioFiles(files: List<File>): File?
    
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
} 