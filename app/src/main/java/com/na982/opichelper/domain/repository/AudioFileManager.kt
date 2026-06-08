package com.na982.opichelper.domain.repository

import java.io.File

interface AudioFileManager {
    fun getRecordingFilePath(fileName: String): String

    fun getRecordingsDirectory(): File

    suspend fun saveRecordingFile(recordingFile: File, fileName: String): File

    suspend fun mergeAudioFiles(files: List<File>, mergedFileName: String): File?

    suspend fun getEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File?
}
