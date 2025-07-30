package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.isActive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFileRepositoryImpl @Inject constructor(
    private val audioFileManager: AudioFileManager,
    private val audioRecorder: AudioRecorder,
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val recordingTimeManager: RecordingTimeManager
) : RecordingFileRepository {

    private var currentRecordingPath: String? = null
    private var currentPlayingPath: String? = null

    override suspend fun hasRecordingFile(category: String, scriptIndex: Int): Boolean {
        val recordingsDir = getRecordingsDirectory()
        val files = recordingsDir.listFiles()
        
        if (files != null) {
            for (file in files) {
                if (file.name.startsWith("통암기_${category}_${scriptIndex}_") && file.name.endsWith(".m4a")) {
                    Log.d("RecordingFileRepositoryImpl", "hasRecordingFile: 파일 발견 - ${file.name}")
                    currentRecordingPath = file.absolutePath
                    return true
                }
            }
        }
        
        Log.d("RecordingFileRepositoryImpl", "hasRecordingFile: 파일 없음 - category=$category, scriptIndex=$scriptIndex")
        return false
    }

    override suspend fun getRecordingFilePath(category: String, scriptIndex: Int): String? {
        return if (hasRecordingFile(category, scriptIndex)) {
            currentRecordingPath
        } else {
            null
        }
    }

    override suspend fun createRecordingFile(category: String, scriptIndex: Int): String {
        val timestamp = System.currentTimeMillis()
        val recordingFileName = "통암기_${category}_${scriptIndex}_${timestamp}.m4a"
        currentRecordingPath = audioFileManager.getRecordingFilePath(recordingFileName)
        
        Log.d("RecordingFileRepositoryImpl", "createRecordingFile: $currentRecordingPath")
        return currentRecordingPath!!
    }

    override suspend fun deleteRecordingFile(category: String, scriptIndex: Int): Boolean {
        return try {
            val filePath = getRecordingFilePath(category, scriptIndex)
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d("RecordingFileRepositoryImpl", "deleteRecordingFile: 파일 삭제 성공 - $filePath")
                        if (currentRecordingPath == filePath) {
                            currentRecordingPath = null
                        }
                    }
                    deleted
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "deleteRecordingFile 실패", e)
            false
        }
    }

    override suspend fun playRecordingFile(
        category: String,
        scriptIndex: Int,
        onPlayingStateChange: (Boolean) -> Unit,
        onHighlight: (Int?) -> Unit
    ) {
        try {
            val filePath = getRecordingFilePath(category, scriptIndex)
            if (filePath == null) {
                Log.e("RecordingFileRepositoryImpl", "playRecordingFile: 재생할 녹음 파일이 없음")
                return
            }
            Log.d("RecordingFileRepositoryImpl", "playRecordingFile: 단순 재생 시작 - $filePath")
            currentPlayingPath = filePath
            onPlayingStateChange(true)
            recordingAudioPlayer.startRecordingPlayback(filePath)
            val recordingDuration = recordingAudioPlayer.getDuration(filePath)
            kotlinx.coroutines.delay(recordingDuration.toLong())
            onPlayingStateChange(false)
            currentPlayingPath = null
            Log.d("RecordingFileRepositoryImpl", "playRecordingFile: 단순 재생 완료")
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "playRecordingFile 실패", e)
            onPlayingStateChange(false)
            currentPlayingPath = null
        }
    }
    
    override suspend fun playRecordingFileSimple(
        category: String,
        scriptIndex: Int,
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            val filePath = getRecordingFilePath(category, scriptIndex)
            if (filePath == null) {
                Log.e("RecordingFileRepositoryImpl", "playRecordingFileSimple: 재생할 녹음 파일이 없음")
                return
            }
            Log.d("RecordingFileRepositoryImpl", "playRecordingFileSimple: 동기 재생 시작 - $filePath")
            currentPlayingPath = filePath
            onPlayingStateChange(true)
            
            // 동기 재생 시작
            recordingAudioPlayer.startRecordingPlayback(filePath)
            
            // 녹음 재생 시간만큼 대기
            val recordingDuration = recordingAudioPlayer.getDuration(filePath)
            kotlinx.coroutines.delay(recordingDuration.toLong())
            
            onPlayingStateChange(false)
            currentPlayingPath = null
            Log.d("RecordingFileRepositoryImpl", "playRecordingFileSimple: 동기 재생 완료")
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "playRecordingFileSimple 실패", e)
            onPlayingStateChange(false)
            currentPlayingPath = null
        }
    }

    override suspend fun stopPlayingRecording() {
        try {
            recordingAudioPlayer.stopRecording()
            currentPlayingPath = null
            Log.d("RecordingFileRepositoryImpl", "stopPlayingRecording: 재생 중지")
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "stopPlayingRecording 실패", e)
        }
    }

    private fun getRecordingsDirectory(): File {
        val dummyPath = audioFileManager.getRecordingFilePath("dummy.m4a")
        return File(dummyPath).parentFile
    }
} 