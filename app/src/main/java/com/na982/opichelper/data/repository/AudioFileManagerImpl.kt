package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.repository.AudioFileManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.media.MediaMuxer
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import java.nio.ByteBuffer
import kotlinx.coroutines.delay

/**
 * AudioFileManager 구현체
 * 오디오 파일 관리 및 병합 기능을 담당
 */
class AudioFileManagerImpl(private val context: Context) : AudioFileManager {
    
    override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
        return withContext(Dispatchers.IO) {
            if (files.isEmpty()) {
                return@withContext null
            }

            // 출력 디렉토리 생성 (앱 내부 저장소 사용)
            val outputDir = File(context.filesDir, "merged")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val output = File(outputDir, "english_writing_${scriptId}_merged.m4a")

            // 단일 파일인 경우 그대로 복사
            if (files.size == 1) {
                files[0].copyTo(output, overwrite = true)
                return@withContext output
            }

            // 여러 파일인 경우 MediaCodec을 우선적으로 사용한 병합
            var totalBytesWritten = 0L
            
            try {
                mergeWithMediaCodec(files, output)
            } catch (e: Exception) {
                Log.e("AudioFileManager", "MediaCodec 병합 실패, 헤더 분석 방식 사용", e)
                try {
                    mergeWithHeaderAnalysis(files, output)
                } catch (e2: Exception) {
                    Log.e("AudioFileManager", "헤더 분석 병합 실패, fallback 방식 사용", e2)
                    // Fallback: 간단한 파일 연결
                    FileOutputStream(output).use { out ->
                        files.forEach { file ->
                            FileInputStream(file).use { input ->
                                val bytesCopied = input.copyTo(out)
                                totalBytesWritten += bytesCopied
                            }
                        }
                    }
                }
            }
            
            // 원본 파일들 삭제
            files.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }

            // 최종 파일 검증
            if (output.exists() && output.length() > 0) {
                // 병합 성공
            } else {
                Log.e("AudioFileManager", "병합된 파일 검증 실패: 존재=${output.exists()}, 크기=${output.length()}")
            }

            output
        }
    }

    override suspend fun saveRecording(recordedFile: String) {
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(recordedFile)
                if (!sourceFile.exists()) {
                    Log.e("AudioFileManager", "녹음 파일이 존재하지 않음: $recordedFile")
                    return@withContext
                }

                val recordingsDir = File(context.filesDir, "recordings")
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs()
                }

                val fileName = "recording_${System.currentTimeMillis()}.m4a"
                val destinationFile = File(recordingsDir, fileName)

                sourceFile.copyTo(destinationFile, overwrite = true)

                // 원본 파일 삭제
                sourceFile.delete()
            } catch (e: Exception) {
                Log.e("AudioFileManager", "녹음 파일 저장 실패", e)
            }
        }
    }

    override fun getLatestMergedAudioFile(): File? {
        val mergedDir = File(context.filesDir, "merged")
        if (!mergedDir.exists()) return null
        
        val mergedFiles = mergedDir.listFiles { file ->
            file.name.startsWith("english_writing_") && file.name.endsWith("_merged.m4a")
        }
        
        return mergedFiles?.maxByOrNull { it.lastModified() }
    }
    
    override fun deleteAudioFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
    
    override suspend fun cleanupOldRecordings(scriptId: String, keepLatestCount: Int) {
        withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) return@withContext
            
            val scriptFiles = recordingsDir.listFiles { file ->
                file.name.startsWith("${scriptId}_") && file.name.endsWith(".m4a")
            }?.sortedByDescending { it.lastModified() }
            
            scriptFiles?.drop(keepLatestCount)?.forEach { file ->
                file.delete()
            }
        }
    }
    
    override suspend fun cleanupAllOldRecordings(keepLatestCount: Int) {
        withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) return@withContext
            
            val allFiles = recordingsDir.listFiles { file ->
                file.name.endsWith(".m4a")
            }
            
            // 스크립트별로 그룹화
            val filesByScript = allFiles?.groupBy { file ->
                file.name.substringBeforeLast("_").substringBeforeLast("_")
            }
            
            filesByScript?.forEach { (scriptId, files) ->
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                sortedFiles.drop(keepLatestCount).forEach { file ->
                    file.delete()
                }
            }
        }
    }
    
    override fun getRecordingFilePath(fileName: String): String {
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return File(recordingsDir, fileName).absolutePath
    }

    override suspend fun hasRecordingFile(scriptId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(getRecordingFilePath("${scriptId}_recording.m4a"))
            file.exists()
        }
    }

    override suspend fun hasRecordingFileByPath(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            file.exists()
        }
    }

    override suspend fun deleteRecordingFileByPath(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("AudioFileManager", "녹음 파일 삭제 실패: $filePath", e)
                false
            }
        }
    }

    private fun mergeWithMediaCodec(files: List<File>, output: File) {
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrackIndex = -1
        var totalDuration = 0L

        try {
            files.forEachIndexed { fileIndex, file ->
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)

                for (trackIndex in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(trackIndex)
                    if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        if (audioTrackIndex == -1) {
                            audioTrackIndex = muxer.addTrack(format)
                            muxer.start()
                        }

                        extractor.selectTrack(trackIndex)
                        val buffer = ByteBuffer.allocate(1024 * 1024)
                        val bufferInfo = android.media.MediaCodec.BufferInfo()

                        while (true) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break

                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = extractor.sampleTime + totalDuration
                            
                            // MediaExtractor의 플래그를 MediaCodec의 플래그로 변환
                            val extractorFlags = extractor.sampleFlags
                            var codecFlags = 0
                            if (extractorFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                                codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                            }
                            if (extractorFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
                                codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                            }
                            bufferInfo.flags = codecFlags

                            muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                            extractor.advance()
                        }

                        totalDuration += extractor.getTrackFormat(trackIndex).getLong(MediaFormat.KEY_DURATION)
                        extractor.release()
                        break
                    }
                }
            }
        } finally {
            muxer.release()
        }
    }

    private fun mergeWithHeaderAnalysis(files: List<File>, output: File) {
        FileOutputStream(output).use { out ->
            files.forEachIndexed { index, file ->
                val input = FileInputStream(file)
                val buffer = ByteArray(8192)
                var bytesRead: Int

                // 첫 번째 파일이 아닌 경우 헤더 스킵
                if (index > 0) {
                    // 간단한 헤더 스킵 (실제로는 더 정교한 분석 필요)
                    input.skip(1024) // 대략적인 헤더 크기
                }

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                }
                input.close()
            }
        }
    }

    // ===== 영작테스트 관련 메서드들 =====
    
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
                mergeWithMediaCodec(files, outputFile)
            }
            waitForFileReady(outputFile)
            outputFile
        }
    }

    private suspend fun waitForFileReady(file: File) {
        while (!file.exists() || file.length() == 0L) {
            delay(100L)
        }
    }
    
    override suspend fun hasEnglishWritingTestMergedFile(category: String, scriptIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val mergedDir = File(context.filesDir, "merged")
            if (!mergedDir.exists()) {
                return@withContext false
            }

            val pattern = Regex("영작테스트_${category}_${scriptIndex}_.*")

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

            val pattern = Regex("영작테스트_${category}_${scriptIndex}_.*")

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

            val pattern = Regex("통암기_${category}_${scriptIndex}_.*")

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

            val pattern = Regex("통암기_${category}_${scriptIndex}_.*")

            val files = recordingsDir.listFiles { file ->
                file.name.matches(pattern)
            }

            files?.maxByOrNull { it.lastModified() }
        }
    }
} 