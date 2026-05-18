package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsOrchestrator
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
    progressPersistenceService: ProgressPersistenceService
) : BaseMemorizeTestRepository(progressPersistenceService), EnglishWritingTestRepository {

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

        for (idx in startIndex until count) {
            if (!currentCoroutineContext().isActive) break

            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(category, idx, scriptIndex)
            )

            // 1. 한글 문장 TTS
            emit(MemorizeTestEvent.CardFlip(true))
            delay(100)
            emit(MemorizeTestEvent.KoreanHighlight(idx))

            ttsOrchestrator.speakAndWaitForCompletion(koSentences[idx], isKorean = true, rate = 0.8f)

            if (!currentCoroutineContext().isActive) break

            // 2. 녹음
            emit(MemorizeTestEvent.RecordingHighlight(idx))
            emit(MemorizeTestEvent.RecordingStateChange(true))

            val savedTtsTime = recordingTimeManager.getRecordingTime(category, scriptIndex, idx)
            val recordingDuration = if (savedTtsTime != null && savedTtsTime > 0) {
                savedTtsTime
            } else {
                (enSentences[idx].length * 100L).coerceAtLeast(3000L)
            }

            val recordingFile = audioRecorder.startRecording()
            val startTime = System.currentTimeMillis()
            delay(recordingDuration)
            val actualRecordingTime = System.currentTimeMillis() - startTime
            audioRecorder.stopRecording()

            recordingTimeManager.saveRecordingTime(category, scriptIndex, idx, actualRecordingTime)

            val savedFile = audioFileManager.saveRecordingFile(recordingFile, "english_writing_${category}_${scriptIndex}_${idx}")
            recordingFiles.add(savedFile)

            emit(MemorizeTestEvent.RecordingStateChange(false))
            emit(MemorizeTestEvent.RecordingHighlight(null))
            emit(MemorizeTestEvent.KoreanHighlight(null))
        }

        emit(MemorizeTestEvent.CardFlip(false))
        emit(MemorizeTestEvent.KoreanHighlight(null))
        emit(MemorizeTestEvent.RecordingHighlight(null))

        // 3. 녹음 파일 병합
        if (recordingFiles.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val mergedFileName = "영작테스트_${category}_${scriptIndex}_${timestamp}"

            audioFileManager.mergeAudioFiles(recordingFiles, mergedFileName)

            recordingFiles.forEach { file ->
                if (file.exists()) file.delete()
            }

            emit(MemorizeTestEvent.MergedFileCreated)
        }

        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, 0, scriptIndex)
        )
    }
}
