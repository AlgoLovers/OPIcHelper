package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.junit.Assert.*

class EnglishWritingTestUseCaseTest {

    @Mock
    private lateinit var mockAudioRecorder: AudioRecorder

    @Mock
    private lateinit var mockAudioPlayer: AudioPlayer

    @Mock
    private lateinit var mockTtsOrchestrator: TtsOrchestrator

    @Mock
    private lateinit var mockQaDataManager: QaDataManager

    @Mock
    private lateinit var mockProgressPersistenceService: ProgressPersistenceService

    @Mock
    private lateinit var mockAudioFileManager: AudioFileManager

    @Mock
    private lateinit var mockProgressTracker: MemorizeTestProgressTracker

    private lateinit var englishWritingTestUseCase: EnglishWritingTestUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        englishWritingTestUseCase = EnglishWritingTestUseCase(
            audioRecorder = mockAudioRecorder,
            audioPlayer = mockAudioPlayer,
            ttsOrchestrator = mockTtsOrchestrator,
            qaDataManager = mockQaDataManager,
            progressPersistenceService = mockProgressPersistenceService,
            audioFileManager = mockAudioFileManager,
            onKoreanHighlight = {},
            onEnglishHighlight = {},
            onRecordingHighlight = {},
            onCardFlip = {},
            progressTracker = mockProgressTracker,
            category = "test_category",
            scriptIndex = 0
        )
    }

    @Test
    fun `test EnglishWritingTestUseCase creation`() {
        assertNotNull(englishWritingTestUseCase)
    }
} 