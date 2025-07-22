package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.repository.AudioFileRepository
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
 * AudioFileRepository 구현체
 */
class AudioFileRepositoryImpl(private val context: Context) : AudioFileRepository {
    
    override suspend fun mergeAndSaveAudioFiles(files: List<File>): File? {
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

            val output = File(outputDir, "merged_${System.currentTimeMillis()}.m4a")
            Log.d("AudioFileRepository", "출력 파일: ${output.absolutePath}")

            // 단일 파일인 경우 그대로 복사
            if (files.size == 1) {
                files[0].copyTo(output, overwrite = true)
                Log.d("AudioFileRepository", "단일 파일 복사 완료: ${output.length()} bytes")
                return@withContext output
            }

            // 여러 파일인 경우 MediaCodec을 우선적으로 사용   한 병합
            var totalBytesWritten = 0L
            
            try {
                mergeWithMediaCodec(files, output)
                Log.d("AudioFileRepository", "MediaCodec 병합 완료: ${output.length()} bytes")
            } catch (e: Exception) {
                Log.e("AudioFileRepository", "MediaCodec 병합 실패, FFmpeg 방식 사용", e)
                try {
                    mergeWithFFmpeg(files, output)
                    Log.d("AudioFileRepository", "FFmpeg 병합 완료: ${output.length()} bytes")
                } catch (e2: Exception) {
                    Log.e("AudioFileRepository", "FFmpeg 병합 실패, 헤더 분석 방식 사용", e2)
                    try {
                        mergeWithHeaderAnalysis(files, output)
                        Log.d("AudioFileRepository", "헤더 분석 병합 완료: ${output.length()} bytes")
                    } catch (e3: Exception) {
                        Log.e("AudioFileRepository", "헤더 분석 병합 실패, fallback 방식 사용", e3)
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

    // FFmpeg를 사용한 오디오 파일 병합
    private fun mergeWithFFmpeg(files: List<File>, output: File) {
        Log.d("AudioFileRepository", "FFmpeg 병합 시작: ${files.size}개 파일")
        
        try {
            // FFmpeg 명령어 구성 (concat demuxer 사용)
            val ffmpegCommand = mutableListOf<String>()
            ffmpegCommand.add("ffmpeg")
            
            // 입력 파일들 추가
            files.forEach { file ->
                ffmpegCommand.add("-i")
                ffmpegCommand.add(file.absolutePath)
            }
            
            // 필터 복합체 구성 (파일들을 순차적으로 연결)
            val filterComplex = StringBuilder()
            for (i in files.indices) {
                filterComplex.append("[$i:0]")
            }
            filterComplex.append("concat=n=${files.size}:v=0:a=1[out]")
            
            ffmpegCommand.add("-filter_complex")
            ffmpegCommand.add(filterComplex.toString())
            ffmpegCommand.add("-map")
            ffmpegCommand.add("[out]")
            ffmpegCommand.add("-c:a") // 오디오 코덱 설정
            ffmpegCommand.add("aac") // AAC 코덱 사용
            ffmpegCommand.add("-b:a") // 비트레이트 설정
            ffmpegCommand.add("128k") // 128kbps
            ffmpegCommand.add("-y") // 기존 파일 덮어쓰기
            ffmpegCommand.add(output.absolutePath)
            
            Log.d("AudioFileRepository", "FFmpeg 명령어: ${ffmpegCommand.joinToString(" ")}")
            
            // FFmpeg 프로세스 실행
            val process = ProcessBuilder(ffmpegCommand)
                .redirectErrorStream(true)
                .start()
            
            // FFmpeg 실행 결과 읽기
            val inputStream = process.inputStream
            val outputBuilder = StringBuilder()
            inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    outputBuilder.append(line).append("\n")
                    Log.d("AudioFileRepository", "FFmpeg: $line")
                }
            }
            
            // 프로세스 종료 대기
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.d("AudioFileRepository", "FFmpeg 병합 성공: ${output.length()} bytes")
            } else {
                Log.e("AudioFileRepository", "FFmpeg 병합 실패. 종료 코드: $exitCode")
                Log.e("AudioFileRepository", "FFmpeg 출력: ${outputBuilder.toString()}")
                throw Exception("FFmpeg 병합 실패. 종료 코드: $exitCode")
            }
            
        } catch (e: Exception) {
            Log.e("AudioFileRepository", "FFmpeg 병합 중 오류 발생", e)
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
                                flags = extractor.sampleFlags
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
} 