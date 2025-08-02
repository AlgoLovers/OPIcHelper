package com.na982.opichelper.data.repository

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.na982.opichelper.domain.audio.AudioFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * AudioFileManager 구현체
 * 오디오 파일 관리 및 병합 기능을 담당
 */
class AudioFileManagerImpl(private val context: Context) : AudioFileManager {
    
    override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
        return withContext(Dispatchers.IO) {
            if (files.isEmpty()) {
                Log.d("AudioFileManager", "병합할 파일이 없습니다.")
                return@withContext null
            }

            Log.d("AudioFileManager", "병합 시작: ${files.size}개 파일")
            files.forEachIndexed { idx, file ->
                Log.d("AudioFileManager", "파일 ${idx + 1}: ${file.name}, 크기: ${file.length()} bytes, 존재: ${file.exists()}")
            }

            // 출력 디렉토리 생성 (앱 내부 저장소 사용)
            val outputDir = File(context.filesDir, "merged")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val output = File(outputDir, "english_writing_${scriptId}_merged.m4a")
            Log.d("AudioFileManager", "출력 파일: ${output.absolutePath}")

            // 단일 파일인 경우 그대로 복사
            if (files.size == 1) {
                files[0].copyTo(output, overwrite = true)
                Log.d("AudioFileManager", "단일 파일 복사 완료: ${output.length()} bytes")
                return@withContext output
            }

            // 여러 파일인 경우 MediaCodec을 우선적으로 사용한 병합
            var totalBytesWritten = 0L
            
            try {
                mergeWithMediaCodec(files, output)
                Log.d("AudioFileManager", "MediaCodec 병합 완료: ${output.length()} bytes")
            } catch (e: Exception) {
                Log.e("AudioFileManager", "MediaCodec 병합 실패, 헤더 분석 방식 사용", e)
                try {
                    mergeWithHeaderAnalysis(files, output)
                    Log.d("AudioFileManager", "헤더 분석 병합 완료: ${output.length()} bytes")
                } catch (e2: Exception) {
                    Log.e("AudioFileManager", "헤더 분석 병합 실패, fallback 방식 사용", e2)
                    // Fallback: 간단한 파일 연결
                    FileOutputStream(output).use { out ->
                        files.forEachIndexed { idx, file ->
                            Log.d("AudioFileManager", "파일 ${idx + 1} 처리 시작: ${file.name}, 크기: ${file.length()} bytes")
                            FileInputStream(file).use { input ->
                                val bytesCopied = input.copyTo(out)
                                totalBytesWritten += bytesCopied
                                Log.d("AudioFileManager", "파일 ${idx + 1} 복사 완료: $bytesCopied bytes")
                            }
                        }
                    }
                    
                    Log.d("AudioFileManager", "Fallback 병합 완료: 총 ${totalBytesWritten} bytes")
                }
            }
            
            // 원본 파일들 삭제
            files.forEach { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("AudioFileManager", "원본 파일 삭제: ${file.name}, 성공: $deleted")
                }
            }

            // 최종 파일 검증
            if (output.exists() && output.length() > 0) {
                Log.d("AudioFileManager", "병합된 파일 검증 성공: ${output.absolutePath}, 크기: ${output.length()} bytes")
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
                Log.d("AudioFileManager", "녹음 파일 저장 완료: ${destinationFile.absolutePath}")

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
            val deleted = file.delete()
            Log.d("AudioFileManager", "오디오 파일 삭제: ${file.name}, 성공: $deleted")
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
                val deleted = file.delete()
                Log.d("AudioFileManager", "오래된 녹음 파일 삭제: ${file.name}, 성공: $deleted")
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
            
            filesByScript?.forEach { (_, files) ->
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                sortedFiles.drop(keepLatestCount).forEach { file ->
                    val deleted = file.delete()
                    Log.d("AudioFileManager", "오래된 녹음 파일 삭제: ${file.name}, 성공: $deleted")
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

    override fun getMergedFilePath(fileName: String): String {
        val mergedDir = File(context.filesDir, "merged")
        if (!mergedDir.exists()) {
            mergedDir.mkdirs()
        }
        return File(mergedDir, fileName).absolutePath
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
                    val deleted = file.delete()
                    Log.d("AudioFileManager", "녹음 파일 삭제: $filePath, 성공: $deleted")
                    deleted
                } else {
                    Log.d("AudioFileManager", "삭제할 녹음 파일이 존재하지 않음: $filePath")
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
            files.forEachIndexed { _, file ->
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
                        val bufferInfo = MediaCodec.BufferInfo()

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
            
            Log.d("AudioFileManager", "녹음 파일 저장: ${outputFile.absolutePath}, 크기: ${outputFile.length()} bytes")
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
            Log.d("AudioFileManager", "오디오 파일 병합 시작: 파일명=${mergedFileName}, 출력경로=${outputFile.absolutePath}")
            Log.d("AudioFileManager", "병합할 파일들: ${files.map { it.absolutePath }}")
            
            if (files.size == 1) {
                files[0].copyTo(outputFile, overwrite = true)
                Log.d("AudioFileManager", "단일 파일 복사 완료")
            } else {
                mergeWithMediaCodec(files, outputFile)
                Log.d("AudioFileManager", "MediaCodec 병합 완료")
            }
            
            Log.d("AudioFileManager", "오디오 파일 병합 완료: ${outputFile.absolutePath}, 크기: ${outputFile.length()} bytes")
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
                Log.d("AudioFileManager", "영작테스트 파일 확인: merged 디렉토리가 존재하지 않음")
                return@withContext false
            }
            
            val pattern = Regex("영작테스트_${category}_${scriptIndex}_.*")
            Log.d("AudioFileManager", "영작테스트 파일 확인: 패턴=${pattern.pattern}")
            
            val files = mergedDir.listFiles { file ->
                val matches = file.name.matches(pattern)
                Log.d("AudioFileManager", "영작테스트 파일 확인: 파일=${file.name}, 매치=${matches}")
                matches
            }
            
            val hasFile = files?.isNotEmpty() == true
            Log.d("AudioFileManager", "영작테스트 파일 확인: category=$category, scriptIndex=$scriptIndex, 결과=$hasFile")
            hasFile
        }
    }
    
    override suspend fun getEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File? {
        return withContext(Dispatchers.IO) {
            val mergedDir = File(context.filesDir, "merged")
            if (!mergedDir.exists()) {
                Log.d("AudioFileManager", "영작테스트 파일 조회: merged 디렉토리가 존재하지 않음")
                return@withContext null
            }
            
            val pattern = Regex("영작테스트_${category}_${scriptIndex}_.*")
            Log.d("AudioFileManager", "영작테스트 파일 조회: 패턴=${pattern.pattern}")
            
            val files = mergedDir.listFiles { file ->
                val matches = file.name.matches(pattern)
                Log.d("AudioFileManager", "영작테스트 파일 조회: 파일=${file.name}, 매치=${matches}")
                matches
            }
            
            val result = files?.maxByOrNull { it.lastModified() }
            Log.d("AudioFileManager", "영작테스트 파일 조회: category=$category, scriptIndex=$scriptIndex, 결과=${result?.absolutePath}")
            result
        }
    }

    override suspend fun hasFullMemorizationRecording(category: String, scriptIndex: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                Log.d("AudioFileManager", "통암기 파일 확인: recordings 디렉토리가 존재하지 않음")
                return@withContext false
            }

            val pattern = Regex("통암기_${category}_${scriptIndex}_.*")
            Log.d("AudioFileManager", "통암기 파일 확인: 패턴=${pattern.pattern}")

            val files = recordingsDir.listFiles { file ->
                val matches = file.name.matches(pattern)
                Log.d("AudioFileManager", "통암기 파일 확인: 파일=${file.name}, 매치=${matches}")
                matches
            }

            val hasFile = files?.isNotEmpty() == true
            Log.d("AudioFileManager", "통암기 파일 확인: category=$category, scriptIndex=$scriptIndex, 결과=$hasFile")
            hasFile
        }
    }

    override suspend fun getFullMemorizationRecording(category: String, scriptIndex: Int): File? {
        return withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                Log.d("AudioFileManager", "통암기 파일 조회: recordings 디렉토리가 존재하지 않음")
                return@withContext null
            }

            val pattern = Regex("통암기_${category}_${scriptIndex}_.*")
            Log.d("AudioFileManager", "통암기 파일 조회: 패턴=${pattern.pattern}")

            val files = recordingsDir.listFiles { file ->
                val matches = file.name.matches(pattern)
                Log.d("AudioFileManager", "통암기 파일 조회: 파일=${file.name}, 매치=${matches}")
                matches
            }

            val result = files?.maxByOrNull { it.lastModified() }
            Log.d("AudioFileManager", "통암기 파일 조회: category=$category, scriptIndex=$scriptIndex, 결과=${result?.absolutePath}")
            result
        }
    }
} 