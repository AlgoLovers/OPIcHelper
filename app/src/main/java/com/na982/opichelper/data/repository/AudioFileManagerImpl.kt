package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.repository.AudioFileManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import com.na982.opichelper.domain.manager.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.media.MediaMuxer
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.os.Build
import java.nio.ByteBuffer

class AudioFileManagerImpl(
    private val context: Context,
    private val appLogger: AppLogger
) : AudioFileManager {

    companion object {
        private const val MERGE_BUFFER_SIZE = 1024 * 1024
        private const val ENGLISH_WRITING_FILE_PREFIX = "english_writing"
        private const val ENGLISH_WRITING_PREFIX = "영작테스트"
        private const val FULL_MEMORIZATION_PREFIX = "통암기"
    }

    override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
        return withContext(Dispatchers.IO) {
            if (files.isEmpty()) {
                return@withContext null
            }

            val outputDir = File(context.filesDir, "merged")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val output = File(outputDir, "${ENGLISH_WRITING_FILE_PREFIX}_${scriptId}_merged.m4a")

            if (files.size == 1) {
                files[0].copyTo(output, overwrite = true)
                return@withContext output
            }

            var mergeSuccess = false

            try {
                mergeWithMediaCodec(files, output)
                mergeSuccess = output.exists() && output.length() > 0
            } catch (e: Exception) {
                appLogger.e("AudioFileManager", "MediaCodec 병합 실패, 파일 연결 방식 사용", e)
                try {
                    FileOutputStream(output).use { out ->
                        files.forEach { file ->
                            FileInputStream(file).use { input ->
                                input.copyTo(out)
                            }
                        }
                    }
                    mergeSuccess = output.exists() && output.length() > 0
                } catch (e2: Exception) {
                    appLogger.e("AudioFileManager", "파일 연결 병합도 실패", e2)
                }
            }

            if (mergeSuccess) {
                files.forEach { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } else {
                appLogger.e("AudioFileManager", "병합 실패로 원본 파일 유지")
            }

            output
        }
    }

    override fun getRecordingFilePath(fileName: String): String {
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return File(recordingsDir, fileName).absolutePath
    }

    private fun mergeWithMediaCodec(files: List<File>, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrackIndex = -1
        var totalDuration = 0L

        try {
            val buffer = ByteBuffer.allocate(MERGE_BUFFER_SIZE)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            files.forEachIndexed { _, file ->
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)

                    for (trackIndex in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(trackIndex)
                        if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                            if (audioTrackIndex == -1) {
                                audioTrackIndex = muxer.addTrack(format)
                                muxer.start()
                            }

                            extractor.selectTrack(trackIndex)

                            while (true) {
                                val sampleSize = extractor.readSampleData(buffer, 0)
                                if (sampleSize < 0) break

                                bufferInfo.offset = 0
                                bufferInfo.size = sampleSize
                                bufferInfo.presentationTimeUs = extractor.sampleTime + totalDuration

                                val extractorFlags = extractor.sampleFlags
                                var codecFlags = 0
                                if (extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                                    codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                    && extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
                                    codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                }
                                bufferInfo.flags = codecFlags

                                muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                                extractor.advance()
                            }

                            totalDuration += extractor.getTrackFormat(trackIndex).getLong(MediaFormat.KEY_DURATION)
                            break
                        }
                    }
                } finally {
                    extractor.release()
                }
            }
        } finally {
            muxer.release()
        }
    }

    override suspend fun saveRecordingFile(recordingFile: File, fileName: String): File {
        return withContext(Dispatchers.IO) {
            val outputDir = File(context.filesDir, "recordings")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, "${fileName}.m4a")
            recordingFile.copyTo(outputFile, overwrite = true)

            outputFile
        }
    }

    override suspend fun mergeAudioFiles(files: List<File>, mergedFileName: String): File {
        return withContext(Dispatchers.IO) {
            val outputDir = File(context.filesDir, "merged")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, "${mergedFileName}.m4a")

            if (files.size == 1) {
                files[0].copyTo(outputFile, overwrite = true)
            } else {
                try {
                    mergeWithMediaCodec(files, outputFile)
                } catch (e: Exception) {
                    appLogger.e("AudioFileManager", "MediaCodec 병합 실패, 파일 연결 방식 사용", e)
                    FileOutputStream(outputFile).use { out ->
                        files.forEach { file ->
                            FileInputStream(file).use { input ->
                                input.copyTo(out)
                            }
                        }
                    }
                }
            }
            outputFile
        }
    }

    override suspend fun hasEnglishWritingTestMergedFile(category: String, scriptIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val mergedDir = File(context.filesDir, "merged")
            if (!mergedDir.exists()) {
                return@withContext false
            }

            val pattern = Regex("${ENGLISH_WRITING_PREFIX}_${category}_${scriptIndex}_.*")

            val files = mergedDir.listFiles { file ->
                file.name.matches(pattern)
            }

            files?.isNotEmpty() == true
        }
    }

    override suspend fun getEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File? {
        return withContext(Dispatchers.IO) {
            val mergedDir = File(context.filesDir, "merged")
            if (!mergedDir.exists()) {
                return@withContext null
            }

            val pattern = Regex("${ENGLISH_WRITING_PREFIX}_${category}_${scriptIndex}_.*")

            val files = mergedDir.listFiles { file ->
                file.name.matches(pattern)
            }

            files?.maxByOrNull { it.lastModified() }
        }
    }

    override suspend fun hasFullMemorizationRecording(category: String, scriptIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                return@withContext false
            }

            val pattern = Regex("${FULL_MEMORIZATION_PREFIX}_${category}_${scriptIndex}_.*")

            val files = recordingsDir.listFiles { file ->
                file.name.matches(pattern)
            }

            files?.isNotEmpty() == true
        }
    }

    override suspend fun getFullMemorizationRecording(category: String, scriptIndex: Int): File? {
        return withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                return@withContext null
            }

            val pattern = Regex("${FULL_MEMORIZATION_PREFIX}_${category}_${scriptIndex}_.*")

            val files = recordingsDir.listFiles { file ->
                file.name.matches(pattern)
            }

            files?.maxByOrNull { it.lastModified() }
        }
    }
}
