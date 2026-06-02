package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ensureActive
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
import com.na982.opichelper.domain.manager.AppLogger

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
    private val recordingTimeManager: RecordingTimeManager,
    private val logger: AppLogger
) : java.io.Closeable {

    companion object {
        private const val QUESTION_TO_RECORDING_DELAY_MS = 500L
        private const val HIGHLIGHT_START_DELAY_MS = 1200L
        private val DEFAULT_RECORDING_TIMES = List(5) { 2000L }
    }
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
                val result = ttsOrchestrator.speakWithHighlight(
                    text = qaItem.questionEn,
                    onHighlight = { index, _ -> _highlightIndex.value = index }
                )
                if (result is TtsSpeakResult.Unavailable || result is TtsSpeakResult.Error) {
                    _state.value = FullMemorizationState.Idle
                    _highlightIndex.value = null
                    return@withLock
                }
                _highlightIndex.value = null
                delay(QUESTION_TO_RECORDING_DELAY_MS)

                val recordingPath = recordingFileRepository.createRecordingFile(category, scriptIndex)
                currentRecordingPath = recordingPath
                _state.value = FullMemorizationState.Recording

                audioRecorder.startRecording(recordingPath)
            }
        } catch (e: Exception) {
            logger.e("FullMemorizationUseCase", "통암기 테스트 시작 실패", e)
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
            logger.e("FullMemorizationUseCase", "녹음 종료 실패", e)
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
                    val times = if (recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                        recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
                    } else {
                        DEFAULT_RECORDING_TIMES
                    }

                    val highlightJob = launch {
                        delay(HIGHLIGHT_START_DELAY_MS)
                        for (i in times.indices) {
                            ensureActive()
                            _highlightIndex.value = i
                            delay(times[i])
                        }
                        _highlightIndex.value = null
                    }

                    try {
                        recordingFileRepository.playRecordingFileSimple(category, scriptIndex) { playing ->
                            if (!playing) {
                                _state.value = FullMemorizationState.WithFile(hasRecording = true)
                            }
                        }
                    } finally {
                        highlightJob.cancel()
                        _highlightIndex.value = null
                    }
                }
            }
        } catch (e: Exception) {
            logger.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _state.value = FullMemorizationState.WithFile(hasRecording = true)
            _highlightIndex.value = null
        }
    }

    suspend fun hasRecording() = mutex.withLock {
        val qaItem = qaDataManager.getCurrentQaItem()
        if (qaItem != null) {
            recordingFileRepository.hasRecordingFile(qaItem.category, qaDataManager.getCurrentIndex())
        } else {
            false
        }
    }

    suspend fun clearRecording() = mutex.withLock {
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
        _state.value = FullMemorizationState.Idle
        _highlightIndex.value = null
        scope.cancel()
    }
}
