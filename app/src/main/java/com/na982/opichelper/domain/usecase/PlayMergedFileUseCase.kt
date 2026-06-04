package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaContentReader
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayMergedFileUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val audioFileManager: AudioFileManager,
    private val qaContentReader: QaContentReader,
    private val recordingTimeManager: RecordingTimeManager
) : java.io.Closeable {

    companion object {
        private const val CHAR_DURATION_MS = 50L
        private const val MIN_HIGHLIGHT_DURATION_MS = 1000L
        private const val FILE_CHECK_RETRY_COUNT = 3
        private const val FILE_CHECK_RETRY_DELAY_MS = 500L
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playJob: Job? = null
    private var checkFileJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _highlightIndex = MutableStateFlow<Int?>(null)
    val highlightIndex: StateFlow<Int?> = _highlightIndex.asStateFlow()

    private val _hasFile = MutableStateFlow(false)
    val hasFile: StateFlow<Boolean> = _hasFile.asStateFlow()

    fun play() {
        playJob?.cancel()
        playJob = scope.launch {
            try {
                val currentItem = qaContentReader.getCurrentQaItem() ?: return@launch
                val category = currentItem.category
                val scriptIndex = qaContentReader.getCurrentIndex()
                val mergedFile = audioFileManager.getEnglishWritingTestMergedFile(category, scriptIndex)

                if (mergedFile == null || !mergedFile.exists()) return@launch

                if (!recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                    playWithDefaultHighlight(mergedFile)
                } else {
                    playWithExactHighlight(mergedFile, category, scriptIndex)
                }
            } catch (e: Exception) {
                _isPlaying.value = false
                _highlightIndex.value = null
            } finally {
                playJob = null
            }
        }
    }

    private suspend fun playWithDefaultHighlight(mergedFile: File) {
        _isPlaying.value = true
        audioPlayer.playAudio(mergedFile.absolutePath)

        val answerText = qaContentReader.getCurrentAnswer(qaContentReader.getCurrentQaItem() ?: return)
        val sentences = SentenceSplitter.split(answerText)

        for (i in sentences.indices) {
            if (!currentCoroutineContext().isActive) break
            if (!_isPlaying.value) break

            _highlightIndex.value = i
            val duration = (sentences[i].length * CHAR_DURATION_MS).coerceAtLeast(MIN_HIGHLIGHT_DURATION_MS)
            kotlinx.coroutines.delay(duration)
        }

        _isPlaying.value = false
        _highlightIndex.value = null
    }

    private suspend fun playWithExactHighlight(mergedFile: File, category: String, scriptIndex: Int) {
        _isPlaying.value = true
        audioPlayer.playAudio(mergedFile.absolutePath)

        val recordingTimes = recordingTimeManager.getAllRecordingTimes(category, scriptIndex)

        for (i in recordingTimes.indices) {
            if (!currentCoroutineContext().isActive) break
            if (!_isPlaying.value) break

            _highlightIndex.value = i
            kotlinx.coroutines.delay(recordingTimes[i])
        }

        _isPlaying.value = false
        _highlightIndex.value = null
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        _isPlaying.value = false
        _highlightIndex.value = null
        audioPlayer.stop()
    }

    fun checkFile() {
        checkFileJob?.cancel()
        checkFileJob = scope.launch {
            val currentItem = qaContentReader.getCurrentQaItem() ?: return@launch
            val category = currentItem.category
            val scriptIndex = qaContentReader.getCurrentIndex()

            var exists = false
            var mergedFile: File? = null

            for (attempt in 1..FILE_CHECK_RETRY_COUNT) {
                exists = audioFileManager.hasEnglishWritingTestMergedFile(category, scriptIndex)
                mergedFile = audioFileManager.getEnglishWritingTestMergedFile(category, scriptIndex)
                if (exists && mergedFile != null && mergedFile.exists()) break
                if (attempt < FILE_CHECK_RETRY_COUNT) kotlinx.coroutines.delay(FILE_CHECK_RETRY_DELAY_MS)
            }

            _hasFile.value = exists && mergedFile != null && mergedFile.exists()
        }
    }

    fun release() {
        playJob?.cancel()
        playJob = null
        checkFileJob?.cancel()
        checkFileJob = null
        _isPlaying.value = false
        _highlightIndex.value = null
        audioPlayer.stop()
    }

    override fun close() {
        release()
        scope.cancel()
    }
}
