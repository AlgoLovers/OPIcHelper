package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

class RecordingFileRepositoryImpl(
    private val audioFileManager: AudioFileManager,
    private val audioRecorder: AudioRecorder,
    private val recordingAudioPlayer: RecordingAudioPlayer,
    private val recordingTimeManager: RecordingTimeManager
) : RecordingFileRepository {

    private val mutex = Mutex()
    private var currentRecordingPath: String? = null
    private var currentPlayingPath: String? = null

    override suspend fun hasRecordingFile(category: String, scriptIndex: Int): Boolean {
        return mutex.withLock {
            val path = findRecordingFilePath(category, scriptIndex)
            if (path != null) {
                currentRecordingPath = path
                true
            } else {
                false
            }
        }
    }

    override suspend fun getRecordingFilePath(category: String, scriptIndex: Int): String? {
        return mutex.withLock {
            val path = findRecordingFilePath(category, scriptIndex)
            if (path != null) {
                currentRecordingPath = path
                path
            } else {
                currentRecordingPath = null
                null
            }
        }
    }

    override suspend fun createRecordingFile(category: String, scriptIndex: Int): String {
        return mutex.withLock {
            val timestamp = System.currentTimeMillis()
            val recordingFileName = "통암기_${category}_${scriptIndex}_${timestamp}.m4a"
            val path = audioFileManager.getRecordingFilePath(recordingFileName)
            currentRecordingPath = path
            path
        }
    }

    override suspend fun deleteRecordingFile(category: String, scriptIndex: Int): Boolean {
        return mutex.withLock {
            try {
                val filePath = findRecordingFilePath(category, scriptIndex)
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted && currentRecordingPath == filePath) {
                            currentRecordingPath = null
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
    }

    override suspend fun playRecordingFile(
        category: String,
        scriptIndex: Int,
        onPlayingStateChange: (Boolean) -> Unit,
        onHighlight: (Int?) -> Unit
    ) {
        playRecordingInternal(category, scriptIndex, onPlayingStateChange)
    }

    override suspend fun playRecordingFileSimple(
        category: String,
        scriptIndex: Int,
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        playRecordingInternal(category, scriptIndex, onPlayingStateChange)
    }

    private suspend fun playRecordingInternal(
        category: String,
        scriptIndex: Int,
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        val filePath = mutex.withLock {
            val path = findRecordingFilePath(category, scriptIndex)
            if (path != null) {
                currentPlayingPath = path
            }
            path
        }

        if (filePath == null) {
            Log.e("RecordingFileRepositoryImpl", "재생할 녹음 파일이 없음")
            return
        }

        try {
            onPlayingStateChange(true)
            awaitPlaybackCompletion(filePath)
            onPlayingStateChange(false)
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "녹음 재생 실패", e)
            onPlayingStateChange(false)
        } finally {
            mutex.withLock { currentPlayingPath = null }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitPlaybackCompletion(filePath: String) {
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                try {
                    recordingAudioPlayer.stopRecording()
                } catch (e: Exception) {
                    Log.e("RecordingFileRepositoryImpl", "취소 시 재생 중지 실패", e)
                }
            }
            recordingAudioPlayer.playRecording(filePath) {
                if (cont.isActive) cont.resume(Unit) {}
            }
        }
    }

    override suspend fun stopPlayingRecording() {
        try {
            recordingAudioPlayer.stopRecording()
            mutex.withLock { currentPlayingPath = null }
        } catch (e: Exception) {
            Log.e("RecordingFileRepositoryImpl", "stopPlayingRecording 실패", e)
        }
    }

    private fun findRecordingFilePath(category: String, scriptIndex: Int): String? {
        val recordingsDir = getRecordingsDirectory()
        val files = recordingsDir.listFiles() ?: return null

        for (file in files) {
            if (file.name.startsWith("통암기_${category}_${scriptIndex}_") && file.name.endsWith(".m4a")) {
                return file.absolutePath
            }
        }
        return null
    }

    private fun getRecordingsDirectory(): File {
        val dummyPath = audioFileManager.getRecordingFilePath("dummy.m4a")
        return File(dummyPath).parentFile!!
    }
}
