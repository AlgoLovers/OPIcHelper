package com.na982.opichelper.domain.manager

import com.na982.opichelper.LogIgnoreRule
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.state.MemorizationProgressTracker
import com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
import com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
import com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
import com.na982.opichelper.domain.usecase.GetLeveledAnswerUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * MemorizationManager 테스트
 * 암기 테스트 모드 관리 기능 테스트
 */
class MemorizationManagerTest {

    @get:Rule
    val logIgnoreRule = LogIgnoreRule()

    @Mock
    private lateinit var mockExecuteRepeatListeningUseCase: StartRepeatListeningUseCase

    @Mock
    private lateinit var mockExecuteEnglishWritingTestUseCase: StartEnglishWritingTestUseCase

    @Mock
    private lateinit var mockExecuteFullMemorizationUseCase: StartFullMemorizationUseCase

    @Mock
    private lateinit var mockQaDataRepository: QaDataRepository

    @Mock
    private lateinit var mockTtsOrchestrator: TtsOrchestrator

    @Mock
    private lateinit var mockGetCurrentAnswerUseCase: GetLeveledAnswerUseCase

    @Mock
    private lateinit var mockProgressTracker: MemorizationProgressTracker

    private lateinit var memorizationManager: MemorizationManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        memorizationManager = MemorizationManager(
            executeRepeatListeningUseCase = mockExecuteRepeatListeningUseCase,
            executeEnglishWritingTestUseCase = mockExecuteEnglishWritingTestUseCase,
            executeFullMemorizationUseCase = mockExecuteFullMemorizationUseCase,
            getCurrentAnswerUseCase = mockGetCurrentAnswerUseCase,
            progressTracker = mockProgressTracker
        )
    }

    @Test
    fun `모드_시작`() = runTest {
        // Given: 모드
        val mode = CurrentMode.REPEAT_LISTENING

        // When: 모드 시작
        memorizationManager.startMode(mode)

        // Then: UI 상태가 업데이트됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == mode)
        assert(uiState.isRunning)
    }

    @Test
    fun `모드_중지`() = runTest {
        // Given: 모드가 실행 중인 상태
        memorizationManager.startMode(CurrentMode.REPEAT_LISTENING)

        // When: 모드 중지
        memorizationManager.stopMode()

        // Then: UI 상태가 초기화됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.NONE)
        assert(!uiState.isRunning)
    }

    @Test
    fun `반복듣기_시작_성공`() = runTest {
        // Given: QA 아이템과 답변 데이터
        val qaItem = createMockQaItem()
        val category = "bank"
        val scriptIndex = 0
        
        whenever(mockQaDataRepository.getCurrentQaItem()).thenReturn(qaItem)
        whenever(mockGetCurrentAnswerUseCase.getCurrentAnswerKo(qaItem)).thenReturn("은행 지점을 방문하여 은행 계좌를 열 수 있습니다.")
        whenever(mockGetCurrentAnswerUseCase.getCurrentAnswer(qaItem)).thenReturn("You can open a bank account by visiting a bank branch.")

        // When: 반복듣기 시작
        memorizationManager.startRepeatListening(category, scriptIndex)

        // Then: 모드가 REPEAT_LISTENING으로 설정됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.REPEAT_LISTENING)
        assert(uiState.isRunning)
    }

    @Test
    fun `반복듣기_시작_실패`() = runTest {
        // Given: QA 아이템이 null인 경우
        val category = "bank"
        val scriptIndex = 0
        
        whenever(mockQaDataRepository.getCurrentQaItem()).thenReturn(null)

        // When: 반복듣기 시작
        memorizationManager.startRepeatListening(category, scriptIndex)

        // Then: 모드가 중지됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.NONE)
        assert(!uiState.isRunning)
    }

    @Test
    fun `영작테스트_시작_성공`() = runTest {
        // Given: QA 아이템과 답변 데이터
        val qaItem = createMockQaItem()
        val category = "bank"
        val scriptIndex = 0
        
        whenever(mockQaDataRepository.getCurrentQaItem()).thenReturn(qaItem)
        whenever(mockGetCurrentAnswerUseCase.getCurrentAnswerKo(qaItem)).thenReturn("은행 지점을 방문하여 은행 계좌를 열 수 있습니다.")
        whenever(mockGetCurrentAnswerUseCase.getCurrentAnswer(qaItem)).thenReturn("You can open a bank account by visiting a bank branch.")

        // When: 영작테스트 시작
        memorizationManager.startEnglishWritingTest(category, scriptIndex)

        // Then: 모드가 ENGLISH_WRITING으로 설정됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.ENGLISH_WRITING)
        assert(uiState.isRunning)
    }

    @Test
    fun `영작테스트_시작_실패`() = runTest {
        // Given: QA 아이템이 null인 경우
        val category = "bank"
        val scriptIndex = 0
        
        whenever(mockQaDataRepository.getCurrentQaItem()).thenReturn(null)

        // When: 영작테스트 시작
        memorizationManager.startEnglishWritingTest(category, scriptIndex)

        // Then: 모드가 중지됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.NONE)
        assert(!uiState.isRunning)
    }

    @Test
    fun `통암기_시작_성공`() = runTest {
        // Given: 통암기 UseCase 성공
        val category = "bank"
        val scriptIndex = 0
        
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(false)

        // When: 통암기 시작
        memorizationManager.startFullMemorization(category, scriptIndex)

        // Then: 모드가 FULL_MEMORIZATION으로 설정됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.FULL_MEMORIZATION)
        assert(uiState.isRunning)
    }

    @Test
    fun `통암기_시작_실패`() = runTest {
        // Given: 통암기 UseCase에서 예외 발생
        val category = "bank"
        val scriptIndex = 0
        
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenThrow(RuntimeException("통암기 시작 실패"))

        // When: 통암기 시작
        memorizationManager.startFullMemorization(category, scriptIndex)

        // Then: 모드가 중지됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.NONE)
        assert(!uiState.isRunning)
    }

    @Test
    fun `현재_모드_중지_반복듣기`() = runTest {
        // Given: 반복듣기 모드가 실행 중
        memorizationManager.startMode(CurrentMode.REPEAT_LISTENING)

        // When: 현재 모드 중지
        memorizationManager.stopCurrentMode()

        // Then: 반복듣기 UseCase가 중지됨
        verify(mockExecuteRepeatListeningUseCase).stop()
    }

    @Test
    fun `통암기_녹음_중지`() = runTest {
        // Given: 통암기 모드가 실행 중
        memorizationManager.startMode(CurrentMode.FULL_MEMORIZATION_RECORDING)

        // When: 통암기 녹음 중지
        memorizationManager.stopFullMemorizationRecording()

        // Then: 통암기 UseCase의 녹음 중지가 호출됨
        verify(mockExecuteFullMemorizationUseCase).stopRecording(any())
    }

    @Test
    fun `통암기_녹음_재생_성공`() = runTest {
        // Given: 녹음 파일이 존재하는 상태
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(true)

        // When: 통암기 녹음 재생
        memorizationManager.playFullMemorizationRecording()

        // Then: 모드가 FULL_MEMORIZATION_PLAYING으로 설정됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.FULL_MEMORIZATION_PLAYING)
    }

    @Test
    fun `통암기_녹음_재생_실패_녹음파일_없음`() = runTest {
        // Given: 녹음 파일이 없는 상태
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(false)

        // When: 통암기 녹음 재생
        memorizationManager.playFullMemorizationRecording()

        // Then: 모드가 변경되지 않음
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.NONE)
    }

    @Test
    fun `통암기_녹음_파일_삭제`() = runTest {
        // Given: 녹음 파일 삭제 성공
        whenever(mockExecuteFullMemorizationUseCase.clearRecording()).thenAnswer { }

        // When: 통암기 녹음 파일 삭제
        memorizationManager.deleteFullMemorizationRecording()

        // Then: 통암기 UseCase의 녹음 파일 삭제가 호출됨
        verify(mockExecuteFullMemorizationUseCase).clearRecording()
    }

    @Test
    fun `통암기_녹음_파일_존재_확인_True`() = runTest {
        // Given: 녹음 파일이 존재하는 상태
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(true)

        // When: 통암기 녹음 파일 존재 확인
        val hasRecording = memorizationManager.hasFullMemorizationRecording()

        // Then: true 반환
        assert(hasRecording)
    }

    @Test
    fun `통암기_녹음_파일_존재_확인_False`() = runTest {
        // Given: 녹음 파일이 없는 상태
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(false)

        // When: 통암기 녹음 파일 존재 확인
        val hasRecording = memorizationManager.hasFullMemorizationRecording()

        // Then: false 반환
        assert(!hasRecording)
    }

    @Test
    fun `통암기_녹음_상태_업데이트_녹음파일_있음`() = runTest {
        // Given: 녹음 파일이 존재하는 상태
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(true)

        // When: 통암기 녹음 상태 업데이트
        memorizationManager.updateFullMemorizationRecordingStatus()

        // Then: 모드가 FULL_MEMORIZATION_WITH_FILE로 설정됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.FULL_MEMORIZATION_WITH_FILE)
        assert(uiState.hasFullMemorizationRecording)
    }

    @Test
    fun `통암기_녹음_상태_업데이트_녹음파일_없음`() = runTest {
        // Given: 녹음 파일이 없는 상태
        whenever(mockExecuteFullMemorizationUseCase.hasRecording()).thenReturn(false)

        // When: 통암기 녹음 상태 업데이트
        memorizationManager.updateFullMemorizationRecordingStatus()

        // Then: 모드가 FULL_MEMORIZATION으로 설정됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.FULL_MEMORIZATION)
        assert(!uiState.hasFullMemorizationRecording)
    }

    @Test
    fun `현재_상태_가져오기`() = runTest {
        // Given: 특정 상태 설정
        memorizationManager.startMode(CurrentMode.REPEAT_LISTENING)

        // When: 현재 상태 가져오기
        val currentState = memorizationManager.getCurrentState()

        // Then: 정확한 상태 반환
        assert(currentState.currentMode == CurrentMode.REPEAT_LISTENING)
        assert(currentState.isRunning)
    }

    @Test
    fun `상태_초기화`() = runTest {
        // Given: 특정 상태 설정
        memorizationManager.startMode(CurrentMode.REPEAT_LISTENING)

        // When: 상태 초기화
        memorizationManager.resetState()

        // Then: 상태가 초기화됨
        val uiState = memorizationManager.uiState.first()
        assert(uiState.currentMode == CurrentMode.NONE)
        assert(!uiState.isRunning)
    }

    @Test
    fun `초기_상태_확인`() = runTest {
        // Given: 초기 상태
        // When: 초기 상태 확인
        val uiState = memorizationManager.uiState.first()

        // Then: 초기 상태가 올바르게 설정됨
        assert(uiState.currentMode == CurrentMode.NONE)
        assert(!uiState.isRunning)
        assert(!uiState.isRepeatListeningMode)
        assert(!uiState.isEnglishWritingTestMode)
        assert(!uiState.isFullMemorizationMode)
    }

    private fun createMockQaItem(): QaItem {
        return QaItem(
            id = "bank_001",
            category = "bank",
            questionEn = "How can I open a bank account?",
            questionKo = "은행 계좌를 어떻게 열 수 있나요?",
            answers = mapOf(
                UserLevel.IM to LeveledAnswer(
                    answerEn = "You can open a bank account by visiting a bank branch.",
                    answerKo = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다."
                )
            )
        )
    }
} 