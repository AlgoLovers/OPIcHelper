package com.na982.opichelper.presentation.ui.component

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.media.MediaPlayer
import android.util.Log
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import com.na982.opichelper.data.repository.AudioFileRepositoryImpl

@RunWith(AndroidJUnit4::class)
class MergedAudioPlaybackTest {
    
    private lateinit var context: android.content.Context
    private lateinit var testDir: File
    private lateinit var audioFile1: File
    private lateinit var audioFile2: File
    private lateinit var audioFileRepository: AudioFileRepositoryImpl
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.filesDir, "merged_test")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        
        // assets에서 테스트 파일들을 복사
        copyAssetFile("test_recording_1.m4a", "test1.m4a")
        copyAssetFile("test_recording_2.m4a", "test2.m4a")
        
        audioFile1 = File(testDir, "test1.m4a")
        audioFile2 = File(testDir, "test2.m4a")
        
        audioFileRepository = AudioFileRepositoryImpl(context)
        
        Log.d("MergedTest", "테스트 파일 준비 완료: ${audioFile1.length()} bytes, ${audioFile2.length()} bytes")
    }
    
    @Test
    fun testMergeAndPlayback() {
        Log.d("MergedTest", "병합 및 재생 테스트 시작")
        
        // 1. 파일 병합
        val inputFiles = listOf(audioFile1, audioFile2)
        val mergedFile = runBlocking {
            audioFileRepository.mergeAndSaveAudioFiles(inputFiles)
        }
        
        assertNotNull("병합된 파일이 null입니다", mergedFile)
        assertTrue("병합된 파일이 존재하지 않습니다", mergedFile!!.exists())
        assertTrue("병합된 파일 크기가 0입니다", mergedFile.length() > 0)
        
        Log.d("MergedTest", "병합 완료: ${mergedFile.absolutePath}, 크기: ${mergedFile.length()} bytes")
        
        // 2. 병합된 파일 재생
        val mediaPlayer = MediaPlayer()
        
        try {
            mediaPlayer.setDataSource(mergedFile.absolutePath)
            mediaPlayer.prepare()
            
            val startTime = System.currentTimeMillis()
            mediaPlayer.start()
            
            // 재생 완료 대기
            while (mediaPlayer.isPlaying) {
                Thread.sleep(100) // 100ms 대기
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            Log.d("MergedTest", "병합된 파일 재생 완료: ${duration}ms")
            
            // 재생 시간이 합리적인지 확인 (최소 1초 이상)
            assertTrue("재생 시간이 너무 짧습니다: ${duration}ms", duration > 1000)
            
        } catch (e: Exception) {
            Log.e("MergedTest", "병합된 파일 재생 중 오류 발생", e)
            throw e
        } finally {
            mediaPlayer.release()
        }
    }
    
    @Test
    fun testMergeAndPlaybackWithCallback() {
        Log.d("MergedTest", "콜백을 사용한 병합 및 재생 테스트 시작")
        
        // 1. 파일 병합
        val inputFiles = listOf(audioFile1, audioFile2)
        val mergedFile = runBlocking {
            audioFileRepository.mergeAndSaveAudioFiles(inputFiles)
        }
        
        assertNotNull("병합된 파일이 null입니다", mergedFile)
        assertTrue("병합된 파일이 존재하지 않습니다", mergedFile!!.exists())
        
        Log.d("MergedTest", "병합 완료: ${mergedFile.absolutePath}, 크기: ${mergedFile.length()} bytes")
        
        // 2. 콜백을 사용한 병합된 파일 재생
        val mediaPlayer = MediaPlayer()
        var playbackCompleted = false
        
        try {
            mediaPlayer.setDataSource(mergedFile.absolutePath)
            mediaPlayer.prepare()
            
            mediaPlayer.setOnCompletionListener {
                Log.d("MergedTest", "병합된 파일 재생 완료 (콜백)")
                playbackCompleted = true
            }
            
            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e("MergedTest", "재생 오류: what=$what, extra=$extra")
                playbackCompleted = true
                true
            }
            
            val startTime = System.currentTimeMillis()
            mediaPlayer.start()
            
            // 재생 완료까지 대기
            while (!playbackCompleted) {
                Thread.sleep(100)
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            Log.d("MergedTest", "콜백을 사용한 병합된 파일 재생 완료: ${duration}ms")
            
            // 재생 시간이 합리적인지 확인
            assertTrue("재생 시간이 너무 짧습니다: ${duration}ms", duration > 1000)
            
        } catch (e: Exception) {
            Log.e("MergedTest", "콜백을 사용한 병합된 파일 재생 중 오류 발생", e)
            throw e
        } finally {
            mediaPlayer.release()
        }
    }
    
    @Test
    fun testMergeFileSizeValidation() {
        Log.d("MergedTest", "병합 파일 크기 검증 테스트 시작")
        
        val inputFiles = listOf(audioFile1, audioFile2)
        val originalTotalSize = inputFiles.sumOf { it.length() }
        
        Log.d("MergedTest", "원본 파일들 총 크기: $originalTotalSize bytes")
        
        val mergedFile = runBlocking {
            audioFileRepository.mergeAndSaveAudioFiles(inputFiles)
        }
        
        assertNotNull("병합된 파일이 null입니다", mergedFile)
        assertTrue("병합된 파일이 존재하지 않습니다", mergedFile!!.exists())
        
        val mergedSize = mergedFile.length()
        Log.d("MergedTest", "병합된 파일 크기: $mergedSize bytes")
        
        // 병합된 파일 크기는 원본 파일들의 합보다 클 수 있음 (헤더 포함)
        assertTrue("병합된 파일 크기가 너무 작습니다", mergedSize >= originalTotalSize * 0.8) // 80% 이상
        assertTrue("병합된 파일 크기가 너무 큽니다", mergedSize <= originalTotalSize * 2) // 200% 이하
        
        Log.d("MergedTest", "병합 파일 크기 검증 완료")
    }
    
    @Test
    fun testPlaybackDurationComparison() {
        Log.d("MergedTest", "재생 시간 비교 테스트 시작")
        
        // 1. 첫 번째 파일 재생 시간 측정
        val duration1 = measurePlaybackDuration(audioFile1)
        Log.d("MergedTest", "파일 1 재생 시간: ${duration1}ms")
        
        // 2. 두 번째 파일 재생 시간 측정
        val duration2 = measurePlaybackDuration(audioFile2)
        Log.d("MergedTest", "파일 2 재생 시간: ${duration2}ms")
        
        // 3. 파일 병합
        val inputFiles = listOf(audioFile1, audioFile2)
        val mergedFile = runBlocking {
            audioFileRepository.mergeAndSaveAudioFiles(inputFiles)
        }
        
        assertNotNull("병합된 파일이 null입니다", mergedFile)
        assertTrue("병합된 파일이 존재하지 않습니다", mergedFile!!.exists())
        
        // 4. 병합된 파일 재생 시간 측정
        val mergedDuration = measurePlaybackDuration(mergedFile!!)
        Log.d("MergedTest", "병합된 파일 재생 시간: ${mergedDuration}ms")
        
        // 5. 시간 비교
        val expectedTotalDuration = duration1 + duration2
        val tolerance = 1000L // 1초 허용 오차
        
        Log.d("MergedTest", "예상 총 재생 시간: ${expectedTotalDuration}ms")
        Log.d("MergedTest", "실제 병합 재생 시간: ${mergedDuration}ms")
        Log.d("MergedTest", "차이: ${Math.abs(mergedDuration - expectedTotalDuration)}ms")
        
        // 병합된 파일의 재생 시간이 개별 파일들의 합과 거의 같아야 함
        val isDurationCorrect = Math.abs(mergedDuration - expectedTotalDuration) <= tolerance
        
        assertTrue(
            "재생 시간이 일치하지 않습니다. 예상: ${expectedTotalDuration}ms, 실제: ${mergedDuration}ms, 차이: ${Math.abs(mergedDuration - expectedTotalDuration)}ms",
            isDurationCorrect
        )
        
        Log.d("MergedTest", "재생 시간 비교 테스트 성공!")
    }
    
    private fun measurePlaybackDuration(audioFile: File): Long {
        val mediaPlayer = MediaPlayer()
        
        try {
            mediaPlayer.setDataSource(audioFile.absolutePath)
            mediaPlayer.prepare()
            
            val startTime = System.currentTimeMillis()
            mediaPlayer.start()
            
            // 재생 완료 대기
            while (mediaPlayer.isPlaying) {
                Thread.sleep(50) // 50ms 대기
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            return duration
            
        } catch (e: Exception) {
            Log.e("MergedTest", "재생 시간 측정 중 오류 발생", e)
            throw e
        } finally {
            mediaPlayer.release()
        }
    }
    
    private fun copyAssetFile(assetName: String, fileName: String) {
        try {
            val inputStream = context.assets.open(assetName)
            val outputFile = File(testDir, fileName)
            FileOutputStream(outputFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            Log.d("MergedTest", "파일 복사 완료: $assetName -> $fileName")
        } catch (e: IOException) {
            Log.e("MergedTest", "파일 복사 실패: $assetName", e)
            throw e
        }
    }
} 