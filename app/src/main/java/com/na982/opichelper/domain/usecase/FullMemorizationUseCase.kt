package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class FullMemorizationUseCase @Inject constructor(
    private val recordingFileRepository: RecordingFileRepository,
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioRecorder: AudioRecorder,
    private val qaDataManager: QaDataManager,
    private val recordingTimeManager: RecordingTimeManager
) {
    private val _highlightIndex = MutableStateFlow<Int?>(null)
    val highlightIndex: StateFlow<Int?> = _highlightIndex.asStateFlow()

    private val _isRecording = AtomicBoolean(false)
    private val _isPlaying = AtomicBoolean(false)

    private var currentRecordingPath: String? = null
    private var playbackJob: Job? = null

    fun isRecording(): Boolean = _isRecording.get()
    fun isPlaying(): Boolean = _isPlaying.get()

    suspend fun startFullMemorization(
        category: String,
        scriptIndex: Int,
        onRecordingStateChange: (Boolean) -> Unit,
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            onPlayingStateChange(true)

            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem != null) {
                ttsOrchestrator.speakAndWaitForCompletion(qaItem.questionEn, isKorean = false, rate = 1.0f)
                delay(500L)

                currentRecordingPath = recordingFileRepository.createRecordingFile(category, scriptIndex)

                _isRecording.set(true)
                onRecordingStateChange(true)

                audioRecorder.startRecording(currentRecordingPath!!)
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "통암기 테스트 시작 실패", e)
            _isRecording.set(false)
            onRecordingStateChange(false)
        }
    }

    suspend fun stopRecording() {
        try {
            if (_isRecording.get() && currentRecordingPath != null) {
                audioRecorder.stopRecording()
                _isRecording.set(false)
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 종료 실패", e)
            _isRecording.set(false)
        }
    }

    suspend fun playRecordingWithHighlight(
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            val qaItem = qaDataManager.getCurrentQaItem() ?: return
            val category = qaItem.category
            val scriptIndex = qaDataManager.getCurrentIndex()

            if (!recordingFileRepository.hasRecordingFile(category, scriptIndex)) return

            _isPlaying.set(true)
            onPlayingStateChange(true)

            val filePath = recordingFileRepository.getRecordingFilePath(category, scriptIndex)
            if (filePath != null) {
                // 녹음 재생 — withContext로 올바른 스코프 관리
                playbackJob?.cancel()
                playbackJob = CoroutineScope(Dispatchers.IO).launch {
                    recordingFileRepository.playRecordingFileSimple(category, scriptIndex) { playing ->
                        if (!playing) _isPlaying.set(false)
                    }
                }

                delay(1200L)

                val times = if (recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                    recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
                } else {
                    listOf(2000L, 2000L, 2000L, 2000L, 2000L)
                }

                for (i in times.indices) {
                    if (!currentCoroutineContext().isActive) break
                    _highlightIndex.value = i
                    delay(times[i])
                }
                _highlightIndex.value = null
            }
            onPlayingStateChange(false)
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _isPlaying.set(false)
            onPlayingStateChange(false)
            _highlightIndex.value = null
        }
    }

    suspend fun playRecordingSimple(
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            val qaItem = qaDataManager.getCurrentQaItem() ?: return
            val category = qaItem.category
            val scriptIndex = qaDataManager.getCurrentIndex()

            if (!recordingFileRepository.hasRecordingFile(category, scriptIndex)) return

            _isPlaying.set(true)

            recordingFileRepository.playRecordingFileSimple(
                category = category,
                scriptIndex = scriptIndex,
                onPlayingStateChange = { playing ->
                    onPlayingStateChange(playing)
                    if (!playing) _isPlaying.set(false)
                }
            )
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _isPlaying.set(false)
            onPlayingStateChange(false)
        }
    }

    suspend fun hasRecording(): Boolean {
        val qaItem = qaDataManager.getCurrentQaItem()
        return if (qaItem != null) {
            recordingFileRepository.hasRecordingFile(qaItem.category, qaDataManager.getCurrentIndex())
        } else {
            false
        }
    }

    suspend fun clearRecording() {
        val qaItem = qaDataManager.getCurrentQaItem()
        if (qaItem != null) {
            recordingFileRepository.deleteRecordingFile(qaItem.category, qaDataManager.getCurrentIndex())
        }
    }

    fun cancelPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _isPlaying.set(false)
        _highlightIndex.value = null
    }
}
