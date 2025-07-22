package com.na982.opichelper.presentation.ui.component

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.media.MediaPlayer
import android.util.Log
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class SequentialAudioPlaybackTest {
    
    private lateinit var context: android.content.Context
    private lateinit var testDir: File
    private lateinit var audioFile1: File
    private lateinit var audioFile2: File
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.filesDir, "sequential_test")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        
        // assets에서 테스트 파일들을 복사
        copyAssetFile("test_recording_1.m4a", "test1.m4a")
        copyAssetFile("test_recording_2.m4a", "test2.m4a")
        
        audioFile1 = File(testDir, "test1.m4a")
        audioFile2 = File(testDir, "test2.m4a")
        
        Log.d("SequentialTest", "테스트 파일 준비 완료: ${audioFile1.length()} bytes, ${audioFile2.length()} bytes")
    }
    
    @Test
    fun testSequentialPlayback() {
        Log.d("SequentialTest", "순차적 재생 테스트 시작")
        
        val mediaPlayer = MediaPlayer()
        val audioFiles = listOf(audioFile1, audioFile2)
        
        try {
            for ((index, audioFile) in audioFiles.withIndex()) {
                Log.d("SequentialTest", "파일 ${index + 1} 재생 시작: ${audioFile.name}")
                
                mediaPlayer.reset()
                mediaPlayer.setDataSource(audioFile.absolutePath)
                mediaPlayer.prepare()
                
                val startTime = System.currentTimeMillis()
                mediaPlayer.start()
                
                // 재생 완료 대기 (동기 방식)
                while (mediaPlayer.isPlaying) {
                    Thread.sleep(100) // 100ms 대기
                }
                
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                Log.d("SequentialTest", "파일 ${index + 1} 재생 완료: ${duration}ms")
                
                // 다음 파일 재생 전 잠시 대기
                Thread.sleep(500)
            }
            
            Log.d("SequentialTest", "모든 파일 순차적 재생 완료")
            
        } catch (e: Exception) {
            Log.e("SequentialTest", "순차적 재생 중 오류 발생", e)
            throw e
        } finally {
            mediaPlayer.release()
        }
    }
    
    @Test
    fun testSequentialPlaybackWithCallback() {
        Log.d("SequentialTest", "콜백을 사용한 순차적 재생 테스트 시작")
        
        val mediaPlayer = MediaPlayer()
        val audioFiles = listOf(audioFile1, audioFile2)
        var currentIndex = 0
        var playbackCompleted = false
        
        try {
            fun playNextFile() {
                if (currentIndex >= audioFiles.size) {
                    playbackCompleted = true
                    return
                }
                
                val audioFile = audioFiles[currentIndex]
                Log.d("SequentialTest", "콜백으로 파일 ${currentIndex + 1} 재생 시작: ${audioFile.name}")
                
                mediaPlayer.reset()
                mediaPlayer.setDataSource(audioFile.absolutePath)
                mediaPlayer.prepare()
                
                mediaPlayer.setOnCompletionListener {
                    Log.d("SequentialTest", "파일 ${currentIndex + 1} 재생 완료")
                    currentIndex++
                    playNextFile() // 다음 파일 재생
                }
                
                mediaPlayer.start()
            }
            
            // 첫 번째 파일부터 시작
            playNextFile()
            
            // 모든 파일 재생 완료까지 대기
            while (!playbackCompleted) {
                Thread.sleep(100)
            }
            
            Log.d("SequentialTest", "콜백을 사용한 모든 파일 순차적 재생 완료")
            
        } catch (e: Exception) {
            Log.e("SequentialTest", "콜백 순차적 재생 중 오류 발생", e)
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
            Log.d("SequentialTest", "파일 복사 완료: $assetName -> $fileName")
        } catch (e: IOException) {
            Log.e("SequentialTest", "파일 복사 실패: $assetName", e)
            throw e
        }
    }
} 