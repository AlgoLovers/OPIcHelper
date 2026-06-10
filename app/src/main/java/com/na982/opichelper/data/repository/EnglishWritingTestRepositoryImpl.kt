package com.na982.opichelper.data.repository

import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsSpeakResult
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EnglishWritingTestRepositoryImpl(
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioRecorder: AudioRecorder,
    private val audioFileManager: AudioFileManager,
    private val recordingTimeManager: RecordingTimeManager,
    progressPersistenceService: ProgressPersistenceService,
    private val appLogger: AppLogger
) : BaseMemorizeTestRepository(progressPersistenceService), EnglishWritingTestRepository {

    companion object {
        private const val RECORDING_CHAR_DURATION_MS = 100L
        private const val MIN_RECORDING_DURATION_MS = 3000L
        private const val ENGLISH_WRITING_FILE_PREFIX = "english_writing"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }

    override val memorizeLevel = MemorizeLevel.ENGLISH_WRITING

    override suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int
    ) {
        val koSentences = splitSentences(answerKo)
        val enSentences = splitSentences(answerEn)
        val count = minOf(koSentences.size, enSentences.size)
        val startIndex = resolveStartIndex(category, scriptIndex, count)

        val recordingFiles = recordSentences(koSentences, enSentences, category, scriptIndex, startIndex, count)

        emit(MemorizeTestEvent.CardFlip(false))
        emit(MemorizeTestEvent.KoreanHighlight(null))
        emit(MemorizeTestEvent.RecordingHighlight(null))

        mergeRecordingFiles(recordingFiles, category, scriptIndex)

        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, scriptIndex, 0)
        )

        emit(MemorizeTestEvent.Completed)
    }

    private suspend fun recordSentences(
        koSentences: List<String>,
        enSentences: List<String>,
        category: String,
        scriptIndex: Int,
        startIndex: Int,
        count: Int
    ): List<File> {
        val recordingFiles = mutableListOf<File>()

        try {
            for (idx in startIndex until count) {
                if (!currentCoroutineContext().isActive) break

                progressPersistenceService.saveNavigationState(
                    ProgressPersistenceService.NavigationState(category, scriptIndex, idx)
                )

                val koResult = playKoreanWithHighlight(ttsOrchestrator, koSentences[idx], idx)
                if (koResult is TtsSpeakResult.Unavailable) break

                if (!currentCoroutineContext().isActive) break

                val recordingFile = recordSentence(enSentences[idx], category, scriptIndex, idx)
                recordingFiles.add(recordingFile)

                emit(MemorizeTestEvent.RecordingStateChange(false))
                emit(MemorizeTestEvent.RecordingHighlight(null))
                emit(MemorizeTestEvent.KoreanHighlight(null))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            recordingFiles.forEach { file -> if (file.exists()) file.delete() }
            throw e
        }

        return recordingFiles
    }

    private suspend fun recordSentence(
        enSentence: String,
        category: String,
        scriptIndex: Int,
        sentenceIndex: Int
    ): File {
        emit(MemorizeTestEvent.RecordingHighlight(sentenceIndex))
        emit(MemorizeTestEvent.RecordingStateChange(true))

        val recordingDuration = calculateRecordingDuration(enSentence, category, scriptIndex, sentenceIndex)

        val recordingFile = audioRecorder.startRecording()
        val startTime = System.currentTimeMillis()
        try {
            delay(recordingDuration)
        } finally {
            audioRecorder.stopRecording()
        }
        val actualRecordingTime = System.currentTimeMillis() - startTime

        recordingTimeManager.saveRecordingTime(category, scriptIndex, sentenceIndex, actualRecordingTime)

        return audioFileManager.saveRecordingFile(
            recordingFile,
            "${ENGLISH_WRITING_FILE_PREFIX}_${category}_${scriptIndex}_${sentenceIndex}"
        )
    }

    private fun calculateRecordingDuration(
        enSentence: String,
        category: String,
        scriptIndex: Int,
        sentenceIndex: Int
    ): Long {
        val savedTtsTime = recordingTimeManager.getRecordingTime(category, scriptIndex, sentenceIndex)
        if (savedTtsTime != null && savedTtsTime > 0) return savedTtsTime
        return (enSentence.length * RECORDING_CHAR_DURATION_MS).coerceAtLeast(MIN_RECORDING_DURATION_MS)
    }

    private suspend fun mergeRecordingFiles(
        recordingFiles: List<File>,
        category: String,
        scriptIndex: Int
    ) {
        if (recordingFiles.isEmpty()) return

        val timestamp = DATE_FORMAT.format(Date())
        val mergedFileName = "${AudioFileManager.ENGLISH_WRITING_PREFIX}_${category}_${scriptIndex}_${timestamp}"

        try {
            val mergedFile = audioFileManager.mergeAudioFiles(recordingFiles, mergedFileName)
            if (mergedFile != null) {
                recordingFiles.forEach { file -> if (file.exists()) file.delete() }
                emit(MemorizeTestEvent.MergedFileCreated)
            } else {
                appLogger.e("EnglishWritingTestRepo", "병합 실패 — 개별 녹음 파일 유지")
            }
        } catch (e: Exception) {
            appLogger.e("EnglishWritingTestRepo", "병합 실패 — 개별 녹음 파일 유지", e)
        }
    }
}
