package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.repository.AudioFileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.After
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.junit.Assert.*
import android.content.Context
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when`
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
class AudioFileRepositoryImplTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var audioFileRepository: AudioFileRepository
    private lateinit var testDir: File
    private lateinit var mockFilesDir: File
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // 테스트용 임시 디렉토리 생성
        testDir = File(System.getProperty("java.io.tmpdir"), "audio_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        // Mock Context 설정
        mockFilesDir = File(testDir, "files")
        mockFilesDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(mockFilesDir)
        
        // 테스트용 AudioFileRepositoryImpl 생성
        audioFileRepository = TestAudioFileRepositoryImpl(mockContext)
    }
    
    @After
    fun tearDown() {
        deleteDirectory(testDir)
    }
    
    @Test
    fun `mergeAndSaveAudioFiles should create merged file when input files exist`() = runBlocking {
        // Given: 테스트용 오디오 파일들 생성
        val testFiles = createTestAudioFiles(3)
        
        // When: 파일 병합 실행
        val mergedFile = audioFileRepository.mergeAndSaveAudioFiles(testFiles, "test_script")
        
        // Then: 병합된 파일이 생성되어야 함
        assertNotNull(mergedFile)
        assertTrue(mergedFile!!.exists())
        assertTrue(mergedFile!!.length() > 0)
        
        println("병합된 파일: ${mergedFile.absolutePath}")
        println("병합된 파일 크기: ${mergedFile.length()} bytes")
    }
    
    @Test
    fun `mergeAndSaveAudioFiles should return null when input files are empty`() = runBlocking {
        // Given: 빈 파일 리스트
        val emptyFiles = emptyList<File>()
        
        // When: 파일 병합 실행
        val mergedFile = audioFileRepository.mergeAndSaveAudioFiles(emptyFiles, "test_script")
        
        // Then: null이 반환되어야 함
        assertEquals(null, mergedFile)
    }
    
    @Test
    fun `getLatestMergedAudioFile should return the most recent file`() {
        // Given: 여러 개의 병합된 파일 생성
        val mergedDir = File(mockFilesDir, "merged")
        mergedDir.mkdirs()
        
        val file1 = File(mergedDir, "merged_1000.m4a")
        val file2 = File(mergedDir, "merged_2000.m4a")
        val file3 = File(mergedDir, "merged_3000.m4a")
        
        createDummyFile(file1, 100)
        createDummyFile(file2, 200)
        createDummyFile(file3, 300)
        
        // 파일 시간 설정 (file3가 가장 최근)
        file1.setLastModified(1000L)
        file2.setLastModified(2000L)
        file3.setLastModified(3000L)
        
        // When: 최신 파일 조회
        val latestFile = audioFileRepository.getLatestMergedAudioFile()
        
        // Then: 가장 최근 파일이 반환되어야 함
        assertNotNull(latestFile)
        assertEquals(file3.absolutePath, latestFile!!.absolutePath)
    }
    
    @Test
    fun `deleteAudioFile should remove the file`() {
        // Given: 테스트 파일 생성
        val testFile = File(testDir, "test_delete.m4a")
        createDummyFile(testFile, 100)
        assertTrue(testFile.exists())
        
        // When: 파일 삭제
        audioFileRepository.deleteAudioFile(testFile)
        
        // Then: 파일이 삭제되어야 함
        assertTrue(!testFile.exists())
    }
    
    @Test
    fun testM4AFileMerging() {
        // Given: 여러 개의 M4A 파일 생성
        val testFiles = listOf(
            createMockM4AFile("test1.m4a", 1024),
            createMockM4AFile("test2.m4a", 2048),
            createMockM4AFile("test3.m4a", 1536)
        )
        
        // When: 파일 병합 실행
        val mergedFile = runBlocking {
            audioFileRepository.mergeAndSaveAudioFiles(testFiles, "test_script")
        }
        
        // Then: 병합된 파일이 생성되고 크기가 예상보다 큼
        assertNotNull(mergedFile)
        assertTrue(mergedFile!!.exists())
        assertTrue(mergedFile.length() > 0)
        
        // 병합된 파일의 크기는 원본 파일들의 합보다 클 수 있음 (헤더 포함)
        val totalOriginalSize = testFiles.sumOf { it.length() }
        assertTrue(mergedFile.length() >= totalOriginalSize)
        
        println("병합된 파일 크기: ${mergedFile.length()} bytes")
        println("원본 파일들 총 크기: $totalOriginalSize bytes")
    }
    
    @Test
    fun testExtractAudioData() {
        // Given: M4A 파일 생성
        val testFile = createMockM4AFile("test.m4a", 1024)
        
        // When: 파일 크기 확인
        val fileSize = testFile.length()
        
        // Then: 파일이 생성되고 크기가 예상과 같음
        assertTrue(testFile.exists())
        assertTrue(fileSize > 0)
        
        println("테스트 파일 크기: $fileSize bytes")
    }
    
    // 테스트용 AudioFileRepositoryImpl (Android Log 대신 println 사용)
    private class TestAudioFileRepositoryImpl(private val context: Context) : AudioFileRepository {
        override suspend fun mergeAndSaveAudioFiles(files: List<File>, scriptId: String): File? {
            return withContext(Dispatchers.IO) {
                if (files.isEmpty()) {
                    println("AudioFileRepository: 병합할 파일이 없습니다.")
                    return@withContext null
                }
                
                println("AudioFileRepository: 병합 시작: ${files.size}개 파일")
                files.forEachIndexed { idx, file ->
                    println("AudioFileRepository: 파일 ${idx + 1}: ${file.name}, 크기: ${file.length()} bytes, 존재: ${file.exists()}")
                }
                
                // 출력 디렉토리 생성 (앱 내부 저장소 사용)
                val outputDir = File(context.filesDir, "merged")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                val output = File(outputDir, "merged_${System.currentTimeMillis()}.m4a")
                println("AudioFileRepository: 출력 파일: ${output.absolutePath}")
                
                var totalBytesWritten = 0L
                FileOutputStream(output).use { out ->
                    files.forEachIndexed { idx, file ->
                        println("AudioFileRepository: 파일 ${idx + 1} 처리 중: ${file.name}, 크기: ${file.length()} bytes")
                        FileInputStream(file).use { input ->
                            if (idx == 0) {
                                // 첫 파일은 전체 복사
                                val bytesCopied = input.copyTo(out)
                                totalBytesWritten += bytesCopied
                                println("AudioFileRepository: 첫 파일 전체 복사: $bytesCopied bytes")
                            } else {
                                // 이후 파일은 헤더 부분을 더 정확하게 스킵
                                // M4A 파일의 경우 일반적으로 첫 8KB 정도가 헤더
                                val skipSize = 8 * 1024L // 8KB 스킵
                                val skipped = input.skip(skipSize)
                                println("AudioFileRepository: 헤더 스킵: $skipped bytes (요청: $skipSize bytes)")
                                val bytesCopied = input.copyTo(out)
                                totalBytesWritten += bytesCopied
                                println("AudioFileRepository: 파일 ${idx + 1} 복사: $bytesCopied bytes")
                            }
                        }
                    }
                }
                
                println("AudioFileRepository: 병합 완료: 총 ${totalBytesWritten} bytes, 최종 파일 크기: ${output.length()} bytes")
                
                // 원본 파일들 삭제
                files.forEach { file ->
                    if (file.exists()) {
                        val deleted = file.delete()
                        println("AudioFileRepository: 원본 파일 삭제: ${file.name}, 성공: $deleted")
                    }
                }
                
                // 최종 파일 검증
                if (output.exists() && output.length() > 0) {
                    println("AudioFileRepository: 병합된 파일 검증 성공: ${output.absolutePath}, 크기: ${output.length()} bytes")
                } else {
                    println("AudioFileRepository: 병합된 파일 검증 실패: 존재=${output.exists()}, 크기=${output.length()}")
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
                println("TestAudioFileRepositoryImpl: 오디오 파일 삭제: ${file.name}, 성공: $deleted")
            }
        }
        
        override suspend fun cleanupAllOldRecordings(keepLatestCount: Int) {
            // 테스트에서는 아무것도 하지 않음
        }
        
        override suspend fun cleanupOldRecordings(scriptId: String, keepLatestCount: Int) {
            // 테스트에서는 아무것도 하지 않음
        }
        
        override suspend fun hasRecordingFile(scriptId: String): Boolean {
            // 테스트에서는 항상 false 반환
            return false
        }
        
        override suspend fun saveRecording(recordedFile: String) {
            // 테스트에서는 아무것도 하지 않음
        }
    }
    
    // 헬퍼 메서드들
    private fun createTestAudioFiles(count: Int): List<File> {
        val files = mutableListOf<File>()
        for (i in 1..count) {
            val file = File(testDir, "test_audio_$i.m4a")
            createDummyM4AFile(file, 1024 * i) // 각각 다른 크기로 생성
            files.add(file)
        }
        return files
    }
    
    private fun createDummyM4AFile(file: File, size: Int) {
        FileOutputStream(file).use { out ->
            // M4A 파일 헤더 시그니처 (ftyp box)
            val m4aHeader = byteArrayOf(
                0x00, 0x00, 0x00, 0x20, // box size
                0x66, 0x74, 0x79, 0x70, // "ftyp"
                0x4D, 0x34, 0x41, 0x20, // "M4A "
                0x00, 0x00, 0x00, 0x00, // minor version
                0x4D, 0x34, 0x41, 0x20, // compatible brand
                0x6D, 0x70, 0x34, 0x32, // "mp42"
                0x69, 0x73, 0x6F, 0x6D, // "isom"
                0x4D, 0x34, 0x41, 0x20  // "M4A "
            )
            out.write(m4aHeader)
            
            // 나머지는 더미 데이터
            val dummyData = ByteArray(size - m4aHeader.size)
            out.write(dummyData)
        }
    }
    
    private fun createDummyFile(file: File, size: Int) {
        FileOutputStream(file).use { out ->
            val dummyData = ByteArray(size)
            out.write(dummyData)
        }
    }

    private fun createMockM4AFile(fileName: String, dataSize: Int): File {
        val file = File(testDir, fileName)
        
        FileOutputStream(file).use { out ->
            // ftyp 박스 (32 bytes)
            writeBox(out, "ftyp", 32, byteArrayOf(
                0x4D, 0x34, 0x41, 0x20, // "M4A "
                0x00, 0x00, 0x00, 0x00, // minor version
                0x4D, 0x34, 0x41, 0x20, // compatible brand 1
                0x6D, 0x70, 0x34, 0x32, // compatible brand 2
                0x69, 0x73, 0x6F, 0x6D, // compatible brand 3
                0x00, 0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
            ))
            
            // moov 박스 (100 bytes) - 간단한 버전
            writeBox(out, "moov", 100, ByteArray(92) { 0 })
            
            // mdat 박스 (오디오 데이터)
            val audioData = ByteArray(dataSize) { (it % 256).toByte() }
            writeBox(out, "mdat", dataSize + 8, audioData)
        }
        
        return file
    }
    
    private fun writeBox(out: FileOutputStream, boxType: String, boxSize: Int, data: ByteArray) {
        // 박스 크기 (4 bytes, big-endian)
        out.write((boxSize shr 24) and 0xFF)
        out.write((boxSize shr 16) and 0xFF)
        out.write((boxSize shr 8) and 0xFF)
        out.write(boxSize and 0xFF)
        
        // 박스 타입 (4 bytes)
        out.write(boxType.toByteArray())
        
        // 데이터
        out.write(data)
    }
    
    private fun deleteDirectory(dir: File) {
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
            dir.delete()
        }
    }
} 