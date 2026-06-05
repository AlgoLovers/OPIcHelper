package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.QaContentReader
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val qaContentReader: QaContentReader,
    private val recordingTimeManager: RecordingTimeManager,
    private val appLogger: AppLogger
) : java.io.Closeable {

    companion object {
        private const val QUESTION_TO_RECORDING_DELAY_MS = 500L
        private const val HIGHLIGHT_START_DELAY_MS = 1200L
        private const val DEFAULT_SENTENCE_DURATION_MS = 2000L
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

    suspend fun startFullMemorization(
        category: String,
        scriptIndex: Int
    ) = mutex.withLock {
        try {
            _state.update { FullMemorizationState.QuestionPlaying }

            val qaItem = qaContentReader.getCurrentQaItem()
            if (qaItem != null) {
                val result = ttsOrchestrator.speakWithHighlight(
                    text = qaItem.questionEn,
                    onHighlight = { index, _ -> _highlightIndex.update { index } }
                )
                if (result is TtsSpeakResult.Unavailable || result is TtsSpeakResult.Error) {
                    _state.update { FullMemorizationState.Idle }
                    _highlightIndex.update { null }
                    return@withLock
                }
                _highlightIndex.update { null }
                delay(QUESTION_TO_RECORDING_DELAY_MS)

                val recordingPath = recordingFileRepository.createRecordingFile(category, scriptIndex)
                currentRecordingPath = recordingPath
                _state.update { FullMemorizationState.Recording }

                audioRecorder.startRecording(recordingPath)
            }
        } catch (e: Exception) {
            appLogger.e("FullMemorizationUseCase", "통암기 테스트 시작 실패", e)
            _state.update { FullMemorizationState.Idle }
            _highlightIndex.update { null }
        }
    }

    suspend fun stopRecording() = mutex.withLock {
        try {
            if (_state.value is FullMemorizationState.Recording && currentRecordingPath != null) {
                audioRecorder.stopRecording()
                _state.update { FullMemorizationState.WithFile(hasRecording = true) }
            }
        } catch (e: Exception) {
            appLogger.e("FullMemorizationUseCase", "녹음 종료 실패", e)
            _state.update { FullMemorizationState.Idle }
        }
    }

    suspend fun playRecordingWithHighlight() = mutex.withLock {
        try {
            val qaItem = qaContentReader.getCurrentQaItem() ?: return@withLock
            val category = qaItem.category
            val scriptIndex = qaContentReader.getCurrentIndex()

            if (!recordingFileRepository.hasRecordingFile(category, scriptIndex)) return@withLock

            _state.update { FullMemorizationState.Playing }

            val filePath = recordingFileRepository.getRecordingFilePath(category, scriptIndex)
            if (filePath != null) {
                playbackJob?.cancel()
                playbackJob = scope.launch {
                    val times = if (recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                        recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
                    } else {
                        val answerText = qaContentReader.getCurrentAnswer(qaItem)
                        val sentenceCount = SentenceSplitter.split(answerText).size
                        List(sentenceCount) { DEFAULT_SENTENCE_DURATION_MS }
                    }

                    val highlightJob = launch {
                        delay(HIGHLIGHT_START_DELAY_MS)
                        for (i in times.indices) {
                            ensureActive()
                            _highlightIndex.update { i }
                            delay(times[i])
                        }
                        _highlightIndex.update { null }
                    }

                    try {
                        recordingFileRepository.playRecordingFileSimple(category, scriptIndex) { playing ->
                            if (!playing) {
                                _state.update { FullMemorizationState.WithFile(hasRecording = true) }
                            }
                        }
                    } finally {
                        highlightJob.cancel()
                        _highlightIndex.update { null }
                    }
                }
            }
        } catch (e: Exception) {
            appLogger.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _state.update { FullMemorizationState.WithFile(hasRecording = true) }
            _highlightIndex.update { null }
        }
    }

    suspend fun hasRecording() = mutex.withLock {
        val qaItem = qaContentReader.getCurrentQaItem()
        if (qaItem != null) {
            recordingFileRepository.hasRecordingFile(qaItem.category, qaContentReader.getCurrentIndex())
        } else {
            false
        }
    }

    suspend fun clearRecording() = mutex.withLock {
        val qaItem = qaContentReader.getCurrentQaItem()
        if (qaItem != null) {
            recordingFileRepository.deleteRecordingFile(qaItem.category, qaContentReader.getCurrentIndex())
        }
    }

    suspend fun cancelPlayback() = mutex.withLock {
        playbackJob?.cancel()
        playbackJob = null
        _state.update { FullMemorizationState.WithFile(hasRecording = true) }
        _highlightIndex.update { null }
    }

    override fun close() {
        playbackJob?.cancel()
        playbackJob = null
        _state.update { FullMemorizationState.Idle }
        _highlightIndex.update { null }
        scope.cancel()
    }
}
