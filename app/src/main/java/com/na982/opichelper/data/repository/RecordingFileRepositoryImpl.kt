package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.manager.AppLogger
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
    private val recordingTimeManager: RecordingTimeManager,
    private val appLogger: AppLogger
) : RecordingFileRepository {

    private val mutex = Mutex()

    override suspend fun hasRecordingFile(category: String, scriptIndex: Int): Boolean {
        return mutex.withLock {
            findRecordingFilePath(category, scriptIndex) != null
        }
    }

    override suspend fun getRecordingFilePath(category: String, scriptIndex: Int): String? {
        return mutex.withLock {
            findRecordingFilePath(category, scriptIndex)
        }
    }

    override suspend fun createRecordingFile(category: String, scriptIndex: Int): String {
        return mutex.withLock {
            val timestamp = System.currentTimeMillis()
            val recordingFileName = "${AudioFileManager.FULL_MEMORIZATION_PREFIX}_${category}_${scriptIndex}_${timestamp}.m4a"
            audioFileManager.getRecordingFilePath(recordingFileName)
        }
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
            findRecordingFilePath(category, scriptIndex)
        }

        if (filePath == null) {
            appLogger.e("RecordingFileRepositoryImpl", "재생할 녹음 파일이 없음")
            return
        }

        try {
            onPlayingStateChange(true)
            awaitPlaybackCompletion(filePath)
            onPlayingStateChange(false)
        } catch (e: Exception) {
            appLogger.e("RecordingFileRepositoryImpl", "녹음 재생 실패", e)
            onPlayingStateChange(false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun awaitPlaybackCompletion(filePath: String) {
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                try {
                    recordingAudioPlayer.stopRecording()
                } catch (e: Exception) {
                    appLogger.e("RecordingFileRepositoryImpl", "취소 시 재생 중지 실패", e)
                }
            }
            recordingAudioPlayer.playRecording(filePath) {
                if (cont.isActive) cont.resume(Unit) {}
            }
        }
    }

    private fun findRecordingFilePath(category: String, scriptIndex: Int): String? {
        val recordingsDir = getRecordingsDirectory()
        val prefix = "${AudioFileManager.FULL_MEMORIZATION_PREFIX}_${category}_${scriptIndex}_"
        return recordingsDir.listFiles { file -> file.name.startsWith(prefix) && file.name.endsWith(".m4a") }
            ?.maxByOrNull { it.lastModified() }?.absolutePath
    }

    private fun getRecordingsDirectory(): File {
        return audioFileManager.getRecordingsDirectory()
    }
}
