package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.isActive
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFileRepositoryImpl @Inject constructor(
    private val audioFileManager: AudioFileManager,
    private val audioRecorder: AudioRecorder,
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val recordingTimeManager: RecordingTimeManager
) : RecordingFileRepository {

    private val currentRecordingPath = AtomicReference<String?>(null)
    private val currentPlayingPath = AtomicReference<String?>(null)

    override suspend fun hasRecordingFile(category: String, scriptIndex: Int): Boolean {
        val recordingsDir = getRecordingsDirectory()
        val files = recordingsDir.listFiles()
        
        if (files != null) {
            for (file in files) {
                if (file.name.startsWith("통암기_${category}_${scriptIndex}_") && file.name.endsWith(".m4a")) {
                    Log.d("RecordingFileRepositoryImpl", "hasRecordingFile: 파일 발견 - ${file.name}")
                    currentRecordingPath.set(file.absolutePath)
                    return true
                }
            }
        }
        
        Log.d("RecordingFileRepositoryImpl", "hasRecordingFile: 파일 없음 - category=$category, scriptIndex=$scriptIndex")
        return false
    }

    override suspend fun getRecordingFilePath(category: String, scriptIndex: Int): String? {
        return if (hasRecordingFile(category, scriptIndex)) {
            currentRecordingPath.get()
        } else {
            null
        }
    }

    override suspend fun createRecordingFile(category: String, scriptIndex: Int): String {
        val timestamp = System.currentTimeMillis()
        val recordingFileName = "통암기_${category}_${scriptIndex}_${timestamp}.m4a"
        currentRecordingPath.set(audioFileManager.getRecordingFilePath(recordingFileName))

        return currentRecordingPath.get()!!
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
                        if (currentRecordingPath.get() == filePath) {
                            currentRecordingPath.set(null)
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
            currentPlayingPath.set(filePath)
            onPlayingStateChange(true)
            recordingAudioPlayer.startRecordingPlayback(filePath)
            val recordingDuration = recordingAudioPlayer.getDuration(filePath)
            kotlinx.coroutines.delay(recordingDuration.toLong())
            onPlayingStateChange(false)
            currentPlayingPath.set(null)
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "playRecordingFile 실패", e)
            onPlayingStateChange(false)
            currentPlayingPath.set(null)
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
            currentPlayingPath.set(filePath)
            onPlayingStateChange(true)

            recordingAudioPlayer.startRecordingPlayback(filePath)

            val recordingDuration = recordingAudioPlayer.getDuration(filePath)
            kotlinx.coroutines.delay(recordingDuration.toLong())

            onPlayingStateChange(false)
            currentPlayingPath.set(null)
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "playRecordingFileSimple 실패", e)
            onPlayingStateChange(false)
            currentPlayingPath.set(null)
        }
    }

    override suspend fun stopPlayingRecording() {
        try {
            recordingAudioPlayer.stopRecording()
            currentPlayingPath.set(null)
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "stopPlayingRecording 실패", e)
        }
    }

    private fun getRecordingsDirectory(): File {
        val dummyPath = audioFileManager.getRecordingFilePath("dummy.m4a")
        return File(dummyPath).parentFile!!
    }
} 