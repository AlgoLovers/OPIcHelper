package com.na982.opichelper.data.repository

import android.content.Context
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * 오디오 파일 병합 기능 단위 테스트
 * 
 * 테스트 목표:
 * 1. AudioFileManager의 병합 기능 검증
 * 2. 파일 저장 및 관리 기능 검증
 * 3. 파일 형식 검증
 */
@RunWith(AndroidJUnit4::class)
class AudioFileMergeUnitTest {

    private lateinit var audioFileManager: AudioFileManagerImpl
    private lateinit var testDir: File
    private lateinit var mergedDir: File
    private lateinit var recordingsDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        audioFileManager = AudioFileManagerImpl(context)
        
        // 테스트용 디렉토리 설정
        testDir = File(context.filesDir, "test_merge")
        mergedDir = File(context.filesDir, "merged")
        recordingsDir = File(context.filesDir, "recordings")
        
        // 테스트 디렉토리 생성
        testDir.mkdirs()
        mergedDir.mkdirs()
        recordingsDir.mkdirs()
        
        // 기존 테스트 파일 정리
        cleanupTestFiles()
    }

    /**
     * 테스트용 M4A 파일 생성
     */
    private fun createTestM4AFile(fileName: String, size: Int = 1024): File {
        val file = File(testDir, fileName)
        
        FileOutputStream(file).use { fos ->
            // M4A 파일 헤더 생성
            val header = byteArrayOf(
                0x00, 0x00, 0x00, 0x20, // box size
                0x66, 0x74, 0x79, 0x70, // 'ftyp'
                0x4D, 0x34, 0x41, 0x20, // 'M4A '
                0x00, 0x00, 0x00, 0x00, // minor version
                0x4D, 0x34, 0x41, 0x20, // compatible brand
                0x6D, 0x70, 0x34, 0x32, // 'mp42'
                0x69, 0x73, 0x6F, 0x6D, // 'isom'
                0x4D, 0x34, 0x41, 0x20  // 'M4A '
            )
            
            fos.write(header)
            
            // 나머지 공간을 더미 데이터로 채움
            val dummyData = ByteArray(size - header.size)
            fos.write(dummyData)
        }
        
        return file
    }

    /**
     * 파일 병합 기능 테스트
     */
    @Test
    fun testMergeAudioFiles() = runBlocking {
        // 1. 테스트용 M4A 파일들 생성
        val file1 = createTestM4AFile("test1.m4a", 2048)
        val file2 = createTestM4AFile("test2.m4a", 1536)
        val file3 = createTestM4AFile("test3.m4a", 1024)
        
        val files = listOf(file1, file2, file3)
        
        // 2. 파일 병합 실행
        val mergedFile = audioFileManager.mergeAudioFiles(files, "test_merged")
        
        // 3. 병합 결과 검증
        assertNotNull(mergedFile, "병합된 파일이 null입니다")
        assertTrue(mergedFile.exists(), "병합된 파일이 존재하지 않습니다")
        assertTrue(mergedFile.length() > 0, "병합된 파일 크기가 0입니다")
        
        // 4. 파일 형식 검증
        assertTrue(isValidM4AFile(mergedFile), "병합된 파일이 올바른 M4A 형식이 아닙니다")
        
        // 5. 파일 크기 검증 (개별 파일들의 합과 비슷해야 함)
        val totalSize = files.sumOf { it.length() }
        val mergedSize = mergedFile.length()
        
        // 병합된 파일 크기가 개별 파일들의 합과 비슷한지 확인 (헤더 오버헤드 고려)
        assertTrue(mergedSize > 0, "병합된 파일 크기가 0입니다")
        assertTrue(mergedSize <= totalSize + 1024, "병합된 파일 크기가 예상보다 큽니다")
        
        println("개별 파일 크기 합: $totalSize bytes")
        println("병합된 파일 크기: $mergedSize bytes")
    }

    /**
     * 단일 파일 복사 테스트
     */
    @Test
    fun testSingleFileCopy() = runBlocking {
        // 1. 테스트용 M4A 파일 생성
        val sourceFile = createTestM4AFile("single_test.m4a", 2048)
        
        // 2. 단일 파일 병합 실행
        val mergedFile = audioFileManager.mergeAudioFiles(listOf(sourceFile), "single_merged")
        
        // 3. 결과 검증
        assertNotNull(mergedFile, "병합된 파일이 null입니다")
        assertTrue(mergedFile.exists(), "병합된 파일이 존재하지 않습니다")
        assertEquals(sourceFile.length(), mergedFile.length(), "파일 크기가 일치하지 않습니다")
        assertTrue(isValidM4AFile(mergedFile), "병합된 파일이 올바른 M4A 형식이 아닙니다")
    }

    /**
     * 빈 파일 리스트 테스트
     */
    @Test
    fun testEmptyFileList() = runBlocking {
        // 1. 빈 리스트로 병합 시도
        val mergedFile = audioFileManager.mergeAudioFiles(emptyList(), "empty_test")
        
        // 2. 결과 검증 (null이 반환되어야 함)
        assertTrue(mergedFile == null, "빈 리스트로 병합 시 null이 반환되어야 합니다")
    }

    /**
     * 파일 저장 기능 테스트
     */
    @Test
    fun testSaveRecordingFile() = runBlocking {
        // 1. 테스트용 녹음 파일 생성
        val recordingFile = createTestM4AFile("recording_test.m4a", 1024)
        
        // 2. 파일 저장 실행
        val savedFile = audioFileManager.saveRecordingFile(recordingFile, "saved_recording")
        
        // 3. 결과 검증
        assertNotNull(savedFile, "저장된 파일이 null입니다")
        assertTrue(savedFile.exists(), "저장된 파일이 존재하지 않습니다")
        assertEquals(recordingFile.length(), savedFile.length(), "파일 크기가 일치하지 않습니다")
        assertTrue(savedFile.name.endsWith(".m4a"), "저장된 파일이 .m4a 확장자가 아닙니다")
    }

    /**
     * 영작테스트 병합 파일 확인 테스트
     */
    @Test
    fun testHasEnglishWritingTestMergedFile() = runBlocking {
        // 1. 테스트용 병합 파일 생성
        val testFile = createTestM4AFile("영작테스트_bank_0_20241201_143022.m4a", 2048)
        val destFile = File(mergedDir, testFile.name)
        testFile.copyTo(destFile, overwrite = true)
        
        // 2. 파일 존재 확인
        val hasFile = audioFileManager.hasEnglishWritingTestMergedFile("bank", 0)
        
        // 3. 결과 검증
        assertTrue(hasFile, "영작테스트 병합 파일이 존재해야 합니다")
        
        // 4. 파일 조회
        val foundFile = audioFileManager.getEnglishWritingTestMergedFile("bank", 0)
        assertNotNull(foundFile, "영작테스트 병합 파일을 찾을 수 없습니다")
        assertEquals(destFile.name, foundFile?.name, "파일명이 일치하지 않습니다")
    }

    /**
     * 통암기 녹음 파일 확인 테스트
     */
    @Test
    fun testHasFullMemorizationRecording() = runBlocking {
        // 1. 테스트용 통암기 파일 생성
        val testFile = createTestM4AFile("통암기_bank_0_20241201_143022.m4a", 2048)
        val destFile = File(recordingsDir, testFile.name)
        testFile.copyTo(destFile, overwrite = true)
        
        // 2. 파일 존재 확인
        val hasFile = audioFileManager.hasFullMemorizationRecording("bank", 0)
        
        // 3. 결과 검증
        assertTrue(hasFile, "통암기 녹음 파일이 존재해야 합니다")
        
        // 4. 파일 조회
        val foundFile = audioFileManager.getFullMemorizationRecording("bank", 0)
        assertNotNull(foundFile, "통암기 녹음 파일을 찾을 수 없습니다")
        assertEquals(destFile.name, foundFile?.name, "파일명이 일치하지 않습니다")
    }

    /**
     * M4A 파일 형식 검증
     */
    private fun isValidM4AFile(file: File): Boolean {
        if (!file.exists() || file.length() < 8) {
            return false
        }

        return try {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8)
                val bytesRead = fis.read(buffer)
                
                if (bytesRead < 8) {
                    return false
                }

                // M4A 파일의 시그니처 확인 (ftyp box)
                val signature = String(buffer, 4, 4)
                signature == "ftyp"
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 테스트 파일 정리
     */
    private fun cleanupTestFiles() {
        // 테스트 디렉토리 정리
        if (testDir.exists()) {
            testDir.listFiles()?.forEach { it.delete() }
        }
        
        // merged 디렉토리 정리
        if (mergedDir.exists()) {
            mergedDir.listFiles()?.forEach { it.delete() }
        }
        
        // recordings 디렉토리 정리
        if (recordingsDir.exists()) {
            recordingsDir.listFiles()?.forEach { it.delete() }
        }
    }
} 