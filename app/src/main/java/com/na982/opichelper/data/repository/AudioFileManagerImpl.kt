package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.repository.AudioFileManager
import java.io.File
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
        private const val ENGLISH_WRITING_PREFIX = "영작테스트"
    }

    override fun getRecordingFilePath(fileName: String): String {
        val recordingsDir = getRecordingsDirectory()
        return File(recordingsDir, fileName).absolutePath
    }

    override fun getRecordingsDirectory(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
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
            try { muxer.stop() } catch (_: Exception) {}
            muxer.release()
        }
    }

    override suspend fun saveRecordingFile(recordingFile: File, fileName: String): File {
        return withContext(Dispatchers.IO) {
            val outputDir = getRecordingsDirectory()

            val outputFile = File(outputDir, "${fileName}.m4a")
            recordingFile.copyTo(outputFile, overwrite = true)

            outputFile
        }
    }

    override suspend fun mergeAudioFiles(files: List<File>, mergedFileName: String): File? {
        return withContext(Dispatchers.IO) {
            val outputDir = File(context.filesDir, "merged")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, "${mergedFileName}.m4a")

            if (files.size == 1) {
                files[0].copyTo(outputFile, overwrite = true)
                outputFile
            } else {
                try {
                    mergeWithMediaCodec(files, outputFile)
                    if (outputFile.exists() && outputFile.length() > 0) outputFile else null
                } catch (e: Exception) {
                    appLogger.e("AudioFileManager", "MediaCodec 병합 실패", e)
                    null
                }
            }
        }
    }

    override suspend fun getEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File? {
        return withContext(Dispatchers.IO) {
            findEnglishWritingTestMergedFile(category, scriptIndex)
        }
    }

    private fun findEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File? {
        val mergedDir = File(context.filesDir, "merged")
        if (!mergedDir.exists()) return null

        val prefix = "${ENGLISH_WRITING_PREFIX}_${category}_${scriptIndex}_"
        return mergedDir.listFiles { file -> file.name.startsWith(prefix) }
            ?.maxByOrNull { it.lastModified() }
    }

}
