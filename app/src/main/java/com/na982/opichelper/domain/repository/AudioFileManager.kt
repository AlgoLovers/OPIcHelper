package com.na982.opichelper.domain.repository

import java.io.File

interface AudioFileManager {
    suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File?

    fun getRecordingFilePath(fileName: String): String

    suspend fun saveRecordingFile(recordingFile: File, fileName: String): File

    suspend fun mergeAudioFiles(files: List<File>, mergedFileName: String): File

    suspend fun hasEnglishWritingTestMergedFile(category: String, scriptIndex: Int): Boolean

    suspend fun getEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File?

    suspend fun hasFullMemorizationRecording(category: String, scriptIndex: Int): Boolean

    suspend fun getFullMemorizationRecording(category: String, scriptIndex: Int): File?
}
