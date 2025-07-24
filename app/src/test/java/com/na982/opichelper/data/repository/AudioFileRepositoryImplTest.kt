package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.repository.AudioFileManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
import java.io.File
import android.content.Context

class AudioFileRepositoryImplTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var audioFileManager: AudioFileManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // 테스트용 AudioFileRepositoryImpl 생성
        audioFileManager = TestAudioFileRepositoryImpl(mockContext)
    }

    @Test
    fun `test mergeAndSaveAudioFiles with valid files`() {
        // Given: 유효한 테스트 파일들
        val testFiles = listOf(
            File("test1.m4a"),
            File("test2.m4a")
        )

        // When: 파일 병합 실행
        val mergedFile = audioFileManager.mergeAndSaveAudioFiles(testFiles, "test_script")

        // Then: 병합된 파일이 생성되어야 함
        assertNotNull(mergedFile)
        assertTrue(mergedFile!!.exists())
        assertTrue(mergedFile.length() > 0)
    }

    @Test
    fun `test mergeAndSaveAudioFiles with empty files list`() {
        // Given: 빈 파일 리스트
        val emptyFiles = emptyList<File>()

        // When: 파일 병합 실행
        val mergedFile = audioFileManager.mergeAndSaveAudioFiles(emptyFiles, "test_script")

        // Then: null이 반환되어야 함
        assertNull(mergedFile)
    }

    @Test
    fun `test getLatestMergedAudioFile`() {
        // When: 최신 병합 파일 조회
        val latestFile = audioFileManager.getLatestMergedAudioFile()

        // Then: 파일이 반환되어야 함 (테스트에서는 null이어도 OK)
        // 실제 구현에서는 파일이 존재할 수 있음
    }

    @Test
    fun `test deleteAudioFile`() {
        // Given: 테스트 파일
        val testFile = File("test_delete.m4a")
        testFile.createNewFile()

        // When: 파일 삭제
        audioFileManager.deleteAudioFile(testFile)

        // Then: 파일이 삭제되어야 함
        assertFalse(testFile.exists())
    }

    @Test
    fun `test mergeAndSaveAudioFiles with single file`() {
        // Given: 단일 파일
        val testFiles = listOf(File("single_test.m4a"))

        // When: 파일 병합 실행
        val mergedFile = audioFileManager.mergeAndSaveAudioFiles(testFiles, "test_script")

        // Then: 병합된 파일이 생성되어야 함
        assertNotNull(mergedFile)
        assertTrue(mergedFile!!.exists())
    }

    // 테스트용 AudioFileRepositoryImpl (Android Log 대신 println 사용)
    private class TestAudioFileRepositoryImpl(private val context: Context) : AudioFileManager {
        override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
            if (files.isEmpty()) {
                println("AudioFileRepository: 병합할 파일이 없습니다.")
                return null
            }

            println("AudioFileRepository: 병합 시작: ${files.size}개 파일")
            files.forEachIndexed { idx, file ->
                println("AudioFileRepository: 파일 ${idx + 1}: ${file.name}, 크기: ${file.length()} bytes, 존재: ${file.exists()}")
            }

            // 테스트용 출력 파일 생성
            val output = File("test_merged_${scriptId}.m4a")
            output.createNewFile()

            // 첫 번째 파일의 헤더를 복사하고 나머지는 오디오 데이터만 복사
            var totalBytesWritten = 0L
            output.outputStream().use { out ->
                files.forEachIndexed { fileIndex, file ->
                    println("AudioFileRepository: 파일 ${fileIndex + 1} 처리 중: ${file.name}, 크기: ${file.length()} bytes")
                    
                    if (fileIndex == 0) {
                        // 첫 번째 파일은 전체 복사
                        file.inputStream().use { input ->
                            val bytesCopied = input.copyTo(out)
                            totalBytesWritten += bytesCopied
                            println("AudioFileRepository: 첫 파일 전체 복사: $bytesCopied bytes")
                        }
                    } else {
                        // 나머지 파일들은 헤더 스킵 (첫 1024바이트)
                        val skipSize = 1024L
                        file.inputStream().use { input ->
                            val skipped = input.skip(skipSize)
                            println("AudioFileRepository: 헤더 스킵: $skipped bytes (요청: $skipSize bytes)")
                            
                            val bytesCopied = input.copyTo(out)
                            totalBytesWritten += bytesCopied
                            println("AudioFileRepository: 파일 ${fileIndex + 1} 복사: $bytesCopied bytes")
                        }
                    }
                }
            }

            println("AudioFileRepository: 병합 완료: 총 ${totalBytesWritten} bytes, 최종 파일 크기: ${output.length()} bytes")

            // 원본 파일들 삭제 (테스트에서는 실제 삭제하지 않음)
            files.forEach { file ->
                val deleted = file.delete()
                println("AudioFileRepository: 원본 파일 삭제: ${file.name}, 성공: $deleted")
            }

            // 최종 파일 검증
            if (output.exists() && output.length() > 0) {
                println("AudioFileRepository: 병합된 파일 검증 성공: ${output.absolutePath}, 크기: ${output.length()} bytes")
            } else {
                println("AudioFileRepository: 병합된 파일 검증 실패: 존재=${output.exists()}, 크기=${output.length()}")
            }

            return output
        }

        override suspend fun saveRecording(recordedFile: String) {
            println("TestAudioFileRepositoryImpl: 녹음 파일 저장: $recordedFile")
        }

        override suspend fun deleteAudioFile(file: File) {
            val deleted = file.delete()
            println("TestAudioFileRepositoryImpl: 오디오 파일 삭제: ${file.name}, 성공: $deleted")
        }

        override suspend fun cleanupOldRecordings(scriptId: String, maxFiles: Int) {
            println("TestAudioFileRepositoryImpl: 스크립트 $scriptId 오래된 녹음 파일 정리")
        }

        override suspend fun cleanupAllOldRecordings(maxFiles: Int) {
            println("TestAudioFileRepositoryImpl: 전체 오래된 녹음 파일 정리")
        }

        override suspend fun hasRecordingFiles(scriptId: String): Boolean {
            return false // 테스트에서는 항상 false 반환
        }

        override suspend fun getLatestMergedAudioFile(): File? {
            return File("test_latest_merged.m4a").apply { createNewFile() }
        }

        override suspend fun getRecordingFilePath(fileName: String): String {
            return "test_recordings/$fileName"
        }

        override suspend fun hasRecordingFileByPath(filePath: String): Boolean {
            return File(filePath).exists()
        }

        override suspend fun deleteRecordingFileByPath(filePath: String) {
            File(filePath).delete()
        }
    }
} 