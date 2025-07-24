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

/**
 * AudioFileManager 구현체
 */
class AudioFileRepositoryImpl(private val context: Context) : AudioFileManager {
    
    override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
        return withContext(Dispatchers.IO) {
            if (files.isEmpty()) {
                Log.d("AudioFileRepository", "병합할 파일이 없습니다.")
                return@withContext null
            }

            Log.d("AudioFileRepository", "병합 시작: ${files.size}개 파일")
            files.forEachIndexed { idx, file ->
                Log.d("AudioFileRepository", "파일 ${idx + 1}: ${file.name}, 크기: ${file.length()} bytes, 존재: ${file.exists()}")
            }

            // 출력 디렉토리 생성 (앱 내부 저장소 사용)
            val outputDir = File(context.filesDir, "merged")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val output = File(outputDir, "english_writing_${scriptId}_merged.m4a")
            Log.d("AudioFileRepository", "출력 파일: ${output.absolutePath}")

            // 단일 파일인 경우 그대로 복사
            if (files.size == 1) {
                files[0].copyTo(output, overwrite = true)
                Log.d("AudioFileRepository", "단일 파일 복사 완료: ${output.length()} bytes")
                return@withContext output
            }

            // 여러 파일인 경우 MediaCodec을 우선적으로 사용한 병합
            var totalBytesWritten = 0L
            
            try {
                mergeWithMediaCodec(files, output)
                Log.d("AudioFileRepository", "MediaCodec 병합 완료: ${output.length()} bytes")
            } catch (e: Exception) {
                Log.e("AudioFileRepository", "MediaCodec 병합 실패, 헤더 분석 방식 사용", e)
                try {
                    mergeWithHeaderAnalysis(files, output)
                    Log.d("AudioFileRepository", "헤더 분석 병합 완료: ${output.length()} bytes")
                } catch (e2: Exception) {
                    Log.e("AudioFileRepository", "헤더 분석 병합 실패, fallback 방식 사용", e2)
                    // Fallback: 간단한 파일 연결
                    FileOutputStream(output).use { out ->
                        files.forEachIndexed { idx, file ->
                            Log.d("AudioFileRepository", "파일 ${idx + 1} 처리 시작: ${file.name}, 크기: ${file.length()} bytes")
                            FileInputStream(file).use { input ->
                                val bytesCopied = input.copyTo(out)
                                totalBytesWritten += bytesCopied
                                Log.d("AudioFileRepository", "파일 ${idx + 1} 복사 완료: $bytesCopied bytes")
                            }
                        }
                    }
                    
                    Log.d("AudioFileRepository", "Fallback 병합 완료: 총 ${totalBytesWritten} bytes")
                }
            }
            
            // 원본 파일들 삭제
            files.forEach { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("AudioFileRepository", "원본 파일 삭제: ${file.name}, 성공: $deleted")
                }
            }

            // 최종 파일 검증
            if (output.exists() && output.length() > 0) {
                Log.d("AudioFileRepository", "병합된 파일 검증 성공: ${output.absolutePath}, 크기: ${output.length()} bytes")
            } else {
                Log.e("AudioFileRepository", "병합된 파일 검증 실패: 존재=${output.exists()}, 크기=${output.length()}")
            }

            output
        }
    }

    override suspend fun saveRecording(recordedFile: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(recordedFile)
                if (file.exists()) {
                    Log.d("AudioFileRepository", "녹음 파일 저장: $recordedFile")
                    // 여기서는 단순히 로그만 남기고, 실제 파일은 이미 존재하므로 추가 작업 불필요
                } else {
                    Log.w("AudioFileRepository", "녹음 파일이 존재하지 않음: $recordedFile")
                }
            } catch (e: Exception) {
                Log.e("AudioFileRepository", "녹음 파일 저장 실패", e)
            }
        }
    }

    override fun getLatestMergedAudioFile(): File? {
        // merged 디렉토리에서 가장 최근 파일 찾기
        val mergedDir = File(context.filesDir, "merged")
        if (!mergedDir.exists()) return null
        
        val files = mergedDir.listFiles { file -> file.extension == "m4a" }
        return files?.maxByOrNull { it.lastModified() }
    }
    
    override fun deleteAudioFile(file: File) {
        if (file.exists()) {
            val deleted = file.delete()
            Log.d("AudioFileRepository", "오디오 파일 삭제: ${file.name}, 성공: $deleted")
        }
    }
    
    override suspend fun cleanupOldRecordings(scriptId: String, keepLatestCount: Int) {
        withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                Log.d("AudioFileRepository", "녹음 디렉토리가 존재하지 않음: ${recordingsDir.absolutePath}")
                return@withContext
            }
            
            // 스크립트 ID로 시작하는 파일들 찾기 (예: "집_0_1", "집_0_2" 등)
            val scriptFiles = recordingsDir.listFiles { file ->
                file.name.startsWith("${scriptId}_") && file.extension == "m4a"
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            
            Log.d("AudioFileRepository", "스크립트 $scriptId 녹음 파일들: ${scriptFiles.size}개")
            
            // 최신 파일들만 유지하고 나머지 삭제
            val filesToDelete = scriptFiles.drop(keepLatestCount)
            filesToDelete.forEach { file ->
                val deleted = file.delete()
                Log.d("AudioFileRepository", "오래된 녹음 파일 삭제: ${file.name}, 성공: $deleted")
            }
            
            Log.d("AudioFileRepository", "스크립트 $scriptId 정리 완료: ${filesToDelete.size}개 파일 삭제")
        }
    }
    
    override suspend fun cleanupAllOldRecordings(keepLatestCount: Int) {
        withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                Log.d("AudioFileRepository", "녹음 디렉토리가 존재하지 않음: ${recordingsDir.absolutePath}")
                return@withContext
            }
            
            // 모든 녹음 파일들을 스크립트별로 그룹화
            val allFiles = recordingsDir.listFiles { file -> file.extension == "m4a" } ?: emptyArray()
            val filesByScript = allFiles.groupBy { file ->
                // 파일명에서 스크립트 ID 추출 (예: "집_0_1_20231201_123456.m4a" -> "집_0")
                val nameWithoutExt = file.nameWithoutExtension
                val parts = nameWithoutExt.split("_")
                if (parts.size >= 2) {
                    "${parts[0]}_${parts[1]}"
                } else {
                    nameWithoutExt // 스크립트 ID를 추출할 수 없는 경우 파일명 그대로 사용
                }
            }.toMutableMap()
            
            // 병합된 파일들도 스크립트별로 그룹화
            val mergedDir = File(context.filesDir, "merged")
            if (mergedDir.exists()) {
                val mergedFiles = mergedDir.listFiles { file -> file.extension == "m4a" } ?: emptyArray()
                mergedFiles.forEach { file ->
                    val nameWithoutExt = file.nameWithoutExtension
                    val parts = nameWithoutExt.split("_")
                    if (parts.size >= 2 && nameWithoutExt.contains("_merged_")) {
                        val scriptId = "${parts[0]}_${parts[1]}"
                        val existingFiles = filesByScript[scriptId] ?: emptyList()
                        filesByScript[scriptId] = existingFiles + file
                    }
                }
            }
            
            Log.d("AudioFileRepository", "발견된 스크립트 수: ${filesByScript.size}")
            
            var totalDeleted = 0
            filesByScript.forEach { (scriptId, files) ->
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                val filesToDelete = sortedFiles.drop(keepLatestCount)
                
                filesToDelete.forEach { file ->
                    val deleted = file.delete()
                    if (deleted) totalDeleted++
                    Log.d("AudioFileRepository", "오래된 녹음 파일 삭제: ${file.name}, 성공: $deleted")
                }
                
                Log.d("AudioFileRepository", "스크립트 $scriptId 정리: ${filesToDelete.size}개 파일 삭제")
            }
            
            Log.d("AudioFileRepository", "전체 정리 완료: 총 $totalDeleted 개 파일 삭제")
        }
    }
    
    override suspend fun hasRecordingFile(scriptId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                Log.d("AudioFileRepository", "녹음 디렉토리가 존재하지 않음: ${recordingsDir.absolutePath}")
                return@withContext false
            }
            
            // 스크립트 ID로 시작하는 파일들 찾기 (예: "집_0_1", "집_0_2" 등)
            val scriptFiles = recordingsDir.listFiles { file ->
                file.name.startsWith("${scriptId}_") && file.extension == "m4a"
            }
            
            // 스크립트 ID 기반 병합된 파일들도 확인 (merged 디렉토리에서)
            val mergedDir = File(context.filesDir, "merged")
            val mergedFiles = if (mergedDir.exists()) {
                mergedDir.listFiles { file ->
                    file.name.startsWith("${scriptId}_merged_") && file.extension == "m4a"
                }
            } else {
                null
            }
            
            val hasFile = scriptFiles?.isNotEmpty() == true || (mergedFiles?.isNotEmpty() == true)
            Log.d("AudioFileRepository", "스크립트 $scriptId 녹음 파일 존재 여부: $hasFile (개별 파일: ${scriptFiles?.size ?: 0}개, 병합 파일: ${mergedFiles?.size ?: 0}개)")
            hasFile
        }
    }

    // MediaCodec을 사용한 오디오 파일 병합
    private fun mergeWithMediaCodec(files: List<File>, output: File) {
        Log.d("AudioFileRepository", "MediaCodec 병합 시작: ${files.size}개 파일")
        
        try {
            val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            var totalDuration = 0L
            
            // 각 파일을 순차적으로 처리
            files.forEachIndexed { fileIndex, file ->
                Log.d("AudioFileRepository", "파일 ${fileIndex + 1} 처리: ${file.name}")
                
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)
                
                // 오디오 트랙 찾기
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    
                    if (mime?.startsWith("audio/") == true) {
                        extractor.selectTrack(i)
                        
                        // 첫 번째 파일에서 오디오 트랙 설정
                        if (fileIndex == 0) {
                            audioTrackIndex = muxer.addTrack(format)
                            muxer.start()
                        }
                        
                        // 오디오 데이터 복사
                        val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB 버퍼
                        val bufferInfo = MediaCodec.BufferInfo()
                        
                        while (true) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break
                            
                            bufferInfo.apply {
                                offset = 0
                                size = sampleSize
                                presentationTimeUs = extractor.sampleTime + totalDuration
                                flags = 0 // MediaCodec.BUFFER_FLAG_SYNC_FRAME 대신 0 사용
                            }
                            
                            muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                            extractor.advance()
                        }
                        
                        totalDuration += extractor.getTrackFormat(i).getLong(MediaFormat.KEY_DURATION)
                        break
                    }
                }
                
                extractor.release()
            }
            
            muxer.stop()
            muxer.release()
            
            Log.d("AudioFileRepository", "MediaCodec 병합 완료: 총 지속시간=${totalDuration}μs")
            
        } catch (e: Exception) {
            Log.e("AudioFileRepository", "MediaCodec 병합 중 오류 발생", e)
            throw e
        }
    }
    
    // 헤더 분석을 통한 파일 병합
    private fun mergeWithHeaderAnalysis(files: List<File>, output: File) {
        Log.d("AudioFileRepository", "헤더 분석 병합 시작: ${files.size}개 파일")
        
        if (files.isEmpty()) {
            throw Exception("병합할 파일이 없습니다.")
        }
        
        // 첫 번째 파일을 기본으로 사용
        val firstFile = files[0]
        val firstFileBytes = firstFile.readBytes()
        
        // 첫 번째 파일의 구조 분석
        var mdatStart = -1
        var mdatSize = 0L
        var moovStart = -1
        var moovSize = 0L
        
        for (i in 0 until firstFileBytes.size - 7) {
            val boxSize = ((firstFileBytes[i].toInt() and 0xFF) shl 24) or
                         ((firstFileBytes[i + 1].toInt() and 0xFF) shl 16) or
                         ((firstFileBytes[i + 2].toInt() and 0xFF) shl 8) or
                         (firstFileBytes[i + 3].toInt() and 0xFF)
            
            val boxType = String(firstFileBytes, i + 4, 4)
            
            when (boxType) {
                "mdat" -> {
                    mdatStart = i
                    mdatSize = boxSize.toLong()
                    Log.d("AudioFileRepository", "mdat 박스 발견: 시작=$mdatStart, 크기=$mdatSize")
                }
                "moov" -> {
                    moovStart = i
                    moovSize = boxSize.toLong()
                    Log.d("AudioFileRepository", "moov 박스 발견: 시작=$moovStart, 크기=$moovSize")
                }
            }
        }
        
        if (mdatStart == -1) {
            throw Exception("첫 번째 파일에서 mdat 박스를 찾을 수 없습니다.")
        }
        
        // 모든 파일의 오디오 데이터 수집
        var totalAudioSize = 0L
        val audioDataList = mutableListOf<ByteArray>()
        
        files.forEachIndexed { fileIndex, file ->
            val fileBytes = file.readBytes()
            var fileMdatStart = -1
            var fileMdatSize = 0L
            
            for (i in 0 until fileBytes.size - 7) {
                val boxSize = ((fileBytes[i].toInt() and 0xFF) shl 24) or
                             ((fileBytes[i + 1].toInt() and 0xFF) shl 16) or
                             ((fileBytes[i + 2].toInt() and 0xFF) shl 8) or
                             (fileBytes[i + 3].toInt() and 0xFF)
                
                val boxType = String(fileBytes, i + 4, 4)
                
                if (boxType == "mdat") {
                    fileMdatStart = i
                    fileMdatSize = boxSize.toLong()
                    break
                }
            }
            
            if (fileMdatStart != -1) {
                // 배열 범위 검증 추가
                val endIndex = fileMdatStart + fileMdatSize.toInt()
                if (endIndex <= fileBytes.size) {
                    val audioData = fileBytes.copyOfRange(fileMdatStart + 8, endIndex)
                    audioDataList.add(audioData)
                    totalAudioSize += audioData.size.toLong()
                    Log.d("AudioFileRepository", "파일 ${fileIndex + 1} 오디오 데이터 크기: ${audioData.size} bytes")
                } else {
                    Log.e("AudioFileRepository", "파일 ${fileIndex + 1} mdat 박스 범위 오류: 시작=${fileMdatStart}, 크기=${fileMdatSize}, 파일크기=${fileBytes.size}")
                    throw Exception("파일 ${fileIndex + 1}의 mdat 박스 범위가 잘못되었습니다.")
                }
            }
        }
        
        // 새로운 mdat 박스 크기 계산 (헤더 8바이트 포함)
        val newMdatSize = totalAudioSize + 8
        
        // 첫 번째 파일의 헤더 부분 복사 (mdat 박스 제외)
        val headerBytes = firstFileBytes.copyOfRange(0, mdatStart)
        
        // 새로운 mdat 박스 헤더 생성
        val newMdatHeader = ByteArray(8)
        newMdatHeader[0] = ((newMdatSize shr 24) and 0xFF).toByte()
        newMdatHeader[1] = ((newMdatSize shr 16) and 0xFF).toByte()
        newMdatHeader[2] = ((newMdatSize shr 8) and 0xFF).toByte()
        newMdatHeader[3] = (newMdatSize and 0xFF).toByte()
        newMdatHeader[4] = 'm'.code.toByte()
        newMdatHeader[5] = 'd'.code.toByte()
        newMdatHeader[6] = 'a'.code.toByte()
        newMdatHeader[7] = 't'.code.toByte()
        
        // 병합된 파일 생성
        FileOutputStream(output).use { out ->
            // 헤더 쓰기
            out.write(headerBytes)
            
            // 새로운 mdat 박스 헤더 쓰기
            out.write(newMdatHeader)
            
            // 모든 오디오 데이터 쓰기
            audioDataList.forEach { audioData ->
                out.write(audioData)
            }
        }
        
        Log.d("AudioFileRepository", "헤더 분석 병합 완료: 총 오디오 크기=${totalAudioSize} bytes, 새 mdat 크기=${newMdatSize} bytes")
    }

    /**
     * 녹음 파일 경로 생성
     */
    override fun getRecordingFilePath(fileName: String): String {
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return File(recordingsDir, fileName).absolutePath
    }
    
    /**
     * 특정 파일 경로의 녹음 파일 존재 여부 확인
     */
    override suspend fun hasRecordingFileByPath(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            file.exists() && file.length() > 0
        }
    }
    
    /**
     * 녹음 파일 삭제
     */
    override suspend fun deleteRecordingFileByPath(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("AudioFileRepository", "녹음 파일 삭제: $filePath, 성공: $deleted")
                    deleted
                } else {
                    Log.d("AudioFileRepository", "삭제할 녹음 파일이 존재하지 않음: $filePath")
                    false
                }
            } catch (e: Exception) {
                Log.e("AudioFileRepository", "녹음 파일 삭제 실패: $filePath", e)
                false
            }
        }
    }
} 