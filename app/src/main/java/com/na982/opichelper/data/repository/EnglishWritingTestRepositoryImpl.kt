package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.TestProgressData
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EnglishWritingTestRepositoryImpl(
    private val qaDataManager: QaDataManager,
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioRecorder: AudioRecorder,
    private val audioFileManager: AudioFileManager,
    private val recordingTimeManager: RecordingTimeManager,
    private val progressPersistenceService: ProgressPersistenceService
) : EnglishWritingTestRepository {

    private val _events = MutableSharedFlow<MemorizeTestEvent>(
        extraBufferCapacity = 1
    )
    override val events: SharedFlow<MemorizeTestEvent> = _events.asSharedFlow()

    private suspend fun emit(event: MemorizeTestEvent) {
        _events.emit(event)
    }

    override suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int
    ) {
        val koSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)

        val navState = progressPersistenceService.loadNavigationState()
        val startIndex = if (navState.category == category && navState.index in 0 until count) {
            navState.index
        } else {
            0
        }

        val recordingFiles = mutableListOf<File>()

        for (idx in startIndex until count) {
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

            progressPersistenceService.saveNavigationState(
                ProgressPersistenceService.NavigationState(category, idx)
            )

            // 1. 한글 문장 TTS
            emit(MemorizeTestEvent.CardFlip(true))
            delay(100)
            emit(MemorizeTestEvent.KoreanHighlight(idx))

            ttsOrchestrator.speakAndWaitForCompletion(koSentences[idx], isKorean = true, rate = 0.8f)

            if (!kotlinx.coroutines.currentCoroutineContext().isActive) break

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
            ProgressPersistenceService.NavigationState(category, 0)
        )
    }

    override suspend fun getCurrentProgress(category: String, scriptIndex: Int): TestProgressData? {
        val navState = progressPersistenceService.loadNavigationState()
        return if (navState.category == category) {
            TestProgressData(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = MemorizeLevel.ENGLISH_WRITING.displayName,
                currentSentenceIndex = navState.index,
                totalSentences = 0,
                isMemorizeTestRunning = false
            )
        } else null
    }

    override suspend fun updateProgress(progressData: TestProgressData) {
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(progressData.category, progressData.currentSentenceIndex)
        )
    }

    override suspend fun clearProgress(category: String, scriptIndex: Int) {
        progressPersistenceService.saveNavigationState(
            ProgressPersistenceService.NavigationState(category, 0)
        )
    }
}
