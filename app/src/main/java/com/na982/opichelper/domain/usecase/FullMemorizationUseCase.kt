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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

sealed class FullMemorizationState {
    object Idle : FullMemorizationState()
    object QuestionPlaying : FullMemorizationState()
    object Recording : FullMemorizationState()
    object Playing : FullMemorizationState()
    data class WithFile(val hasRecording: Boolean) : FullMemorizationState()
}

@Singleton
class FullMemorizationUseCase @Inject constructor(
    private val recordingFileRepository: RecordingFileRepository,
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioRecorder: AudioRecorder,
    private val qaDataManager: QaDataManager,
    private val recordingTimeManager: RecordingTimeManager
) : java.io.Closeable {
    private val _highlightIndex = MutableStateFlow<Int?>(null)
    val highlightIndex: StateFlow<Int?> = _highlightIndex.asStateFlow()

    private val _state = MutableStateFlow<FullMemorizationState>(FullMemorizationState.Idle)
    val state: StateFlow<FullMemorizationState> = _state.asStateFlow()

    private val mutex = Mutex()
    @Volatile
    private var currentRecordingPath: String? = null
    @Volatile
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isRecording(): Boolean = _state.value is FullMemorizationState.Recording
    fun isPlaying(): Boolean = _state.value is FullMemorizationState.Playing

    suspend fun startFullMemorization(
        category: String,
        scriptIndex: Int
    ) = mutex.withLock {
        try {
            _state.value = FullMemorizationState.QuestionPlaying

            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem != null) {
                ttsOrchestrator.speakWithHighlight(
                    text = qaItem.questionEn,
                    onHighlight = { index, _ -> _highlightIndex.value = index }
                )
                _highlightIndex.value = null
                delay(500L)

                currentRecordingPath = recordingFileRepository.createRecordingFile(category, scriptIndex)
                _state.value = FullMemorizationState.Recording

                audioRecorder.startRecording(currentRecordingPath!!)
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "통암기 테스트 시작 실패", e)
            _state.value = FullMemorizationState.Idle
            _highlightIndex.value = null
        }
    }

    suspend fun stopRecording() = mutex.withLock {
        try {
            if (_state.value is FullMemorizationState.Recording && currentRecordingPath != null) {
                audioRecorder.stopRecording()
                _state.value = FullMemorizationState.WithFile(hasRecording = true)
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 종료 실패", e)
            _state.value = FullMemorizationState.Idle
        }
    }

    suspend fun playRecordingWithHighlight() = mutex.withLock {
        try {
            val qaItem = qaDataManager.getCurrentQaItem() ?: return@withLock
            val category = qaItem.category
            val scriptIndex = qaDataManager.getCurrentIndex()

            if (!recordingFileRepository.hasRecordingFile(category, scriptIndex)) return@withLock

            _state.value = FullMemorizationState.Playing

            val filePath = recordingFileRepository.getRecordingFilePath(category, scriptIndex)
            if (filePath != null) {
                playbackJob?.cancel()
                playbackJob = scope.launch {
                    try {
                        recordingFileRepository.playRecordingFileSimple(category, scriptIndex) { playing ->
                            if (!playing) {
                                _state.value = FullMemorizationState.WithFile(hasRecording = true)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
                        _state.value = FullMemorizationState.WithFile(hasRecording = true)
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
            _state.value = FullMemorizationState.WithFile(hasRecording = true)
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _state.value = FullMemorizationState.WithFile(hasRecording = true)
            _highlightIndex.value = null
        }
    }

    suspend fun playRecordingSimple() = mutex.withLock {
        try {
            val qaItem = qaDataManager.getCurrentQaItem() ?: return@withLock
            val category = qaItem.category
            val scriptIndex = qaDataManager.getCurrentIndex()

            if (!recordingFileRepository.hasRecordingFile(category, scriptIndex)) return@withLock

            _state.value = FullMemorizationState.Playing

            recordingFileRepository.playRecordingFileSimple(
                category = category,
                scriptIndex = scriptIndex,
                onPlayingStateChange = { playing ->
                    if (!playing) {
                        _state.value = FullMemorizationState.WithFile(hasRecording = true)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _state.value = FullMemorizationState.WithFile(hasRecording = true)
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

    suspend fun cancelPlayback() = mutex.withLock {
        playbackJob?.cancel()
        playbackJob = null
        _state.value = FullMemorizationState.WithFile(hasRecording = true)
        _highlightIndex.value = null
    }

    override fun close() {
        playbackJob?.cancel()
        playbackJob = null
        _state.value = FullMemorizationState.WithFile(hasRecording = true)
        _highlightIndex.value = null
        scope.cancel()
    }
}
