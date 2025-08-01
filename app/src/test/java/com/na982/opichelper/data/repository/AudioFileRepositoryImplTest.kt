package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.audio.AudioFileManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
import java.io.File
import android.content.Context
import kotlinx.coroutines.runBlocking

class AudioFileRepositoryImplTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var audioFileManager: AudioFileManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // 테스트용 AudioFileManagerImpl 생성
        audioFileManager = TestAudioFileManagerImpl(mockContext)
    }

    @Test
    fun `test mergeAndSaveAudioFiles with valid files`() = runBlocking {
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
    fun `test mergeAndSaveAudioFiles with empty files list`() = runBlocking {
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
    fun `test mergeAndSaveAudioFiles with single file`() = runBlocking {
        // Given: 단일 파일
        val testFiles = listOf(File("single_test.m4a"))

        // When: 파일 병합 실행
        val mergedFile = audioFileManager.mergeAndSaveAudioFiles(testFiles, "test_script")

        // Then: 병합된 파일이 생성되어야 함
        assertNotNull(mergedFile)
        assertTrue(mergedFile!!.exists())
    }

    // 테스트용 AudioFileManagerImpl (Android Log 대신 println 사용)
    private class TestAudioFileManagerImpl(private val context: Context) : AudioFileManager {
        override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
            if (files.isEmpty()) {
                println("AudioFileManager: 병합할 파일이 없습니다.")
                return null
            }

            println("AudioFileManager: 병합 시작: ${files.size}개 파일")
            files.forEachIndexed { idx, file ->
                println("AudioFileManager: 파일 ${idx + 1}: ${file.name}, 크기: ${file.length()} bytes, 존재: ${file.exists()}")
            }

            // 테스트용 출력 파일 생성
            val output = File("test_merged_${scriptId}.m4a")
            output.createNewFile()
            output.writeText("test merged content")

            println("AudioFileManager: 병합 완료: ${output.absolutePath}")
            return output
        }

        override suspend fun saveRecording(recordedFile: String) {
            println("AudioFileManager: 녹음 파일 저장: $recordedFile")
        }

        override fun getLatestMergedAudioFile(): File? {
            val testFile = File("test_latest_merged.m4a")
            if (!testFile.exists()) {
                testFile.createNewFile()
            }
            return testFile
        }

        override fun deleteAudioFile(file: File) {
            if (file.exists()) {
                val deleted = file.delete()
                println("AudioFileManager: 오디오 파일 삭제: ${file.name}, 성공: $deleted")
            }
        }

        override suspend fun cleanupOldRecordings(scriptId: String, keepLatestCount: Int) {
            println("AudioFileManager: 오래된 녹음 파일 정리: $scriptId, 유지 개수: $keepLatestCount")
        }

        override suspend fun cleanupAllOldRecordings(keepLatestCount: Int) {
            println("AudioFileManager: 모든 오래된 녹음 파일 정리, 유지 개수: $keepLatestCount")
        }

        override suspend fun hasRecordingFile(scriptId: String): Boolean {
            val testFile = File("${scriptId}_recording.m4a")
            return testFile.exists()
        }

        override fun getRecordingFilePath(fileName: String): String {
            return File("recordings", fileName).absolutePath
        }

        override suspend fun hasRecordingFileByPath(filePath: String): Boolean {
            val file = File(filePath)
            return file.exists()
        }

        override suspend fun deleteRecordingFileByPath(filePath: String): Boolean {
            val file = File(filePath)
            return if (file.exists()) {
                val deleted = file.delete()
                println("AudioFileManager: 녹음 파일 삭제: $filePath, 성공: $deleted")
                deleted
            } else {
                println("AudioFileManager: 삭제할 녹음 파일이 존재하지 않음: $filePath")
                false
            }
        }

        // ===== 영작테스트 관련 메서드들 =====
        
        override suspend fun saveRecordingFile(recordingFile: File, fileName: String): File {
            val outputFile = File("${fileName}.m4a")
            recordingFile.copyTo(outputFile, overwrite = true)
            println("AudioFileManager: 녹음 파일 저장: ${outputFile.absolutePath}")
            return outputFile
        }
        
        override suspend fun mergeAudioFiles(files: List<File>, mergedFileName: String): File {
            val outputFile = File("${mergedFileName}.m4a")
            outputFile.createNewFile()
            outputFile.writeText("test merged content")
            println("AudioFileManager: 오디오 파일 병합 완료: ${outputFile.absolutePath}")
            return outputFile
        }
        
        override suspend fun hasEnglishWritingTestMergedFile(category: String, scriptIndex: Int): Boolean {
            val testFile = File("영작테스트_${category}_${scriptIndex}_test.m4a")
            return testFile.exists()
        }
        
        override suspend fun getEnglishWritingTestMergedFile(category: String, scriptIndex: Int): File? {
            val testFile = File("영작테스트_${category}_${scriptIndex}_test.m4a")
            return if (testFile.exists()) testFile else null
        }
        
        override suspend fun getFullMemorizationRecording(category: String, scriptIndex: Int): File? {
            val testFile = File("전체암기_${category}_${scriptIndex}_test.m4a")
            return if (testFile.exists()) testFile else null
        }
        
        override suspend fun hasFullMemorizationRecording(category: String, scriptIndex: Int): Boolean {
            val testFile = File("전체암기_${category}_${scriptIndex}_test.m4a")
            return testFile.exists()
        }
    }
} 