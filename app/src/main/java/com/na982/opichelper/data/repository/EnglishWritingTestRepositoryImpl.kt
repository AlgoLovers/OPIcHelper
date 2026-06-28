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

        val recordingFiles = mutableListOf<File>()

        try {
            for (idx in startIndex until count) {
                if (!currentCoroutineContext().isActive) break

                progressPersistenceService.saveNavigationState(
                    ProgressPersistenceService.NavigationState(category, scriptIndex, idx)
                )

                // 1. 한글 문장 TTS
                val koResult = playKoreanWithHighlight(ttsOrchestrator, koSentences[idx], idx)
                if (koResult is TtsSpeakResult.Unavailable) break

                if (!currentCoroutineContext().isActive) break

                // 2. 녹음
                emit(MemorizeTestEvent.RecordingHighlight(idx))
                emit(MemorizeTestEvent.RecordingStateChange(true))

                val savedTtsTime = recordingTimeManager.getRecordingTime(category, scriptIndex, idx)
                val recordingDuration = if (savedTtsTime != null && savedTtsTime > 0) {
                    savedTtsTime
                } else {
                    (enSentences[idx].length * RECORDING_CHAR_DURATION_MS).coerceAtLeast(MIN_RECORDING_DURATION_MS)
                }

                val recordingFile = audioRecorder.startRecording()
                val startTime = System.currentTimeMillis()
                try {
                    delay(recordingDuration)
                } finally {
                    audioRecorder.stopRecording()
                }
                val actualRecordingTime = System.currentTimeMillis() - startTime

                recordingTimeManager.saveRecordingTime(category, scriptIndex, idx, actualRecordingTime)

                val savedFile = audioFileManager.saveRecordingFile(recordingFile, "${ENGLISH_WRITING_FILE_PREFIX}_${category}_${scriptIndex}_${idx}")
                recordingFiles.add(savedFile)

                emit(MemorizeTestEvent.RecordingStateChange(false))
                emit(MemorizeTestEvent.RecordingHighlight(null))
                emit(MemorizeTestEvent.KoreanHighlight(null))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            recordingFiles.forEach { file ->
                if (file.exists()) file.delete()
            }
            throw e
        }

        emit(MemorizeTestEvent.CardFlip(false))
        emit(MemorizeTestEvent.KoreanHighlight(null))
        emit(MemorizeTestEvent.RecordingHighlight(null))

        // 3. 녹음 파일 병합
        if (recordingFiles.isNotEmpty()) {
            val timestamp = DATE_FORMAT.format(Date())
            val mergedFileName = "${AudioFileManager.ENGLISH_WRITING_PREFIX}_${category}_${scriptIndex}_${timestamp}"

            try {
                val mergedFile = audioFileManager.mergeAudioFiles(recordingFiles, mergedFileName)
                if (mergedFile != null) {
                    recordingFiles.forEach { file ->
                        if (file.exists()) file.delete()
                    }
                    emit(MemorizeTestEvent.MergedFileCreated)
                } else {
                    appLogger.e("EnglishWritingTestRepo", "병합 실패 — 개별 녹음 파일 유지")
                }
            } catch (e: Exception) {
                appLogger.e("EnglishWritingTestRepo", "병합 실패 — 개별 녹음 파일 유지", e)
            }
        }

        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, scriptIndex, 0)
        )

        emit(MemorizeTestEvent.Completed)
    }
}
