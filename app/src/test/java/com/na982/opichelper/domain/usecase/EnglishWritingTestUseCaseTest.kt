package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.repository.AudioFileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.After
import java.io.File
import org.junit.Assert.*

class EnglishWritingTestUseCaseTest {
    
    private lateinit var testDir: File
    private lateinit var mockTtsPlayer: TtsPlayer
    private lateinit var mockAudioRecorder: AudioRecorder
    private lateinit var mockAudioFileRepository: AudioFileRepository
    private var mergedFileCreated: File? = null
    
    @Before
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir"), "usecase_test_${System.currentTimeMillis()}")
        testDir.mkdirs()
        
        // Mock TtsPlayer
        mockTtsPlayer = object : TtsPlayer {
            override fun speakQuestion(text: String, rate: Float) {}
            override fun speakAnswer(text: String, rate: Float) {}
            override fun stopTts() {}
            override suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long = 1000L
            override suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {}
        }
        
        // Mock AudioRecorder
        mockAudioRecorder = object : AudioRecorder {
            override fun startRecording(): File {
                val file = File(testDir, "test_recording_${System.currentTimeMillis()}.m4a")
                createMockAudioFile(file)
                return file
            }
            
            override fun stopRecording(): File? {
                return null
            }
        }
        
        // Mock AudioFileRepository
        mockAudioFileRepository = object : AudioFileRepository {
            override suspend fun mergeAndSaveAudioFiles(files: List<File>): File? {
                if (files.isEmpty()) return null
                
                val mergedFile = File(testDir, "merged_${System.currentTimeMillis()}.m4a")
                createMockAudioFile(mergedFile)
                return mergedFile
            }
            
            override fun getLatestMergedAudioFile(): File? {
                return mergedFileCreated
            }
            
            override fun deleteAudioFile(file: File) {
                if (file.exists()) file.delete()
            }
        }
    }
    
    @After
    fun tearDown() {
        deleteDirectory(testDir)
    }
    
    @Test
    fun `execute should create merged audio file from recorded files`() = runBlocking {
        // Given: 영작 테스트 UseCase
        val useCase = EnglishWritingTestUseCase(
            answerEn = "This is a test sentence. It has multiple parts.",
            answerKo = "이것은 테스트 문장입니다. 여러 부분이 있습니다.",
            ttsPlayer = mockTtsPlayer,
            audioRecorder = mockAudioRecorder,
            audioFileRepository = mockAudioFileRepository,
            onAutoFlip = null,
            onMergedFileCreated = { file ->
                mergedFileCreated = file
                println("병합된 파일 생성됨: ${file.absolutePath}")
            }
        )
        
        // When: UseCase 실행
        useCase.execute()
        
        // Then: 병합된 파일이 생성되어야 함
        assertNotNull(mergedFileCreated)
        assertTrue(mergedFileCreated!!.exists())
        assertTrue(mergedFileCreated!!.length() > 0)
        
        println("테스트 완료: 병합된 파일 크기 = ${mergedFileCreated!!.length()} bytes")
    }
    
    @Test
    fun `execute should handle empty answer text`() = runBlocking {
        // Given: 빈 답변 텍스트
        val useCase = EnglishWritingTestUseCase(
            answerEn = "",
            answerKo = "",
            ttsPlayer = mockTtsPlayer,
            audioRecorder = mockAudioRecorder,
            audioFileRepository = mockAudioFileRepository,
            onAutoFlip = null,
            onMergedFileCreated = { file ->
                mergedFileCreated = file
            }
        )
        
        // When: UseCase 실행
        useCase.execute()
        
        // Then: 빈 텍스트여도 오류 없이 실행되어야 함
        // (병합된 파일이 생성되지 않을 수 있음)
    }
    
    // 헬퍼 메서드들
    private fun createMockAudioFile(file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            // 간단한 M4A 헤더 생성
            val header = byteArrayOf(
                0x00, 0x00, 0x00, 0x20, // box size
                0x66, 0x74, 0x79, 0x70, // "ftyp"
                0x4D, 0x34, 0x41, 0x20, // "M4A "
                0x00, 0x00, 0x00, 0x00, // minor version
                0x4D, 0x34, 0x41, 0x20, // compatible brand
                0x6D, 0x70, 0x34, 0x32, // "mp42"
                0x69, 0x73, 0x6F, 0x6D, // "isom"
                0x4D, 0x34, 0x41, 0x20  // "M4A "
            )
            out.write(header)
            
            // 더미 오디오 데이터
            val dummyData = ByteArray(1024) // 1KB 더미 데이터
            out.write(dummyData)
        }
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