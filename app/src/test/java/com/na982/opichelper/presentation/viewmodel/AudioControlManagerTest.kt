package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.event.ButtonEvent
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.state.AppStateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * AudioControlManager 테스트
 * 오디오 제어 및 TTS 재생 기능 테스트
 */
class AudioControlManagerTest {

    @Mock
    private lateinit var mockTtsOrchestrator: TtsOrchestrator

    @Mock
    private lateinit var mockButtonEventHandler: ButtonEventHandler

    @Mock
    private lateinit var mockAppStateManager: AppStateManager

    private lateinit var audioControlManager: AudioControlManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        audioControlManager = AudioControlManager(
            ttsOrchestrator = mockTtsOrchestrator,
            buttonEventHandler = mockButtonEventHandler,
            appStateManager = mockAppStateManager
        )
    }

    @Test
    fun `질문_재생_성공`() = runTest {
        // Given: QA 아이템과 TTS 재생 성공
        val qaItem = createMockQaItem()
        whenever(mockTtsOrchestrator.speak(any(), any())).thenAnswer { }

        // When: 질문 재생
        audioControlManager.playQuestion(qaItem)

        // Then: TTS가 정상적으로 호출됨
        verify(mockTtsOrchestrator).speak(qaItem.questionEn, any())
        
        // And: 버튼 상태가 Loading에서 Playing으로 변경됨
        verify(mockAppStateManager).updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Loading)
        verify(mockAppStateManager).updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
    }

    @Test
    fun `질문_재생_실패`() = runTest {
        // Given: TTS 재생 시 예외 발생
        val qaItem = createMockQaItem()
        whenever(mockTtsOrchestrator.speak(any(), any())).thenThrow(RuntimeException("재생 실패"))

        // When: 질문 재생
        audioControlManager.playQuestion(qaItem)

        // Then: 에러 상태가 설정됨
        val error = audioControlManager.error.first()
        assert(error != null)
        assert(error!!.contains("재생 실패"))
        
        // And: 버튼 상태가 Idle로 변경됨
        verify(mockAppStateManager).updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
    }

    @Test
    fun `답변_재생_성공`() = runTest {
        // Given: QA 아이템과 TTS 재생 성공
        val qaItem = createMockQaItem()
        whenever(mockTtsOrchestrator.speak(any(), any())).thenAnswer { }

        // When: 답변 재생
        audioControlManager.playAnswer(qaItem)

        // Then: TTS가 정상적으로 호출됨
        verify(mockTtsOrchestrator).speak(qaItem.answers.values.first().answerEn, any())
        
        // And: 버튼 상태가 Loading에서 Playing으로 변경됨
        verify(mockAppStateManager).updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Loading)
        verify(mockAppStateManager).updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)
    }

    @Test
    fun `답변_재생_실패`() = runTest {
        // Given: TTS 재생 시 예외 발생
        val qaItem = createMockQaItem()
        whenever(mockTtsOrchestrator.speak(any(), any())).thenThrow(RuntimeException("재생 실패"))

        // When: 답변 재생
        audioControlManager.playAnswer(qaItem)

        // Then: 에러 상태가 설정됨
        val error = audioControlManager.error.first()
        assert(error != null)
        assert(error!!.contains("재생 실패"))
        
        // And: 버튼 상태가 Idle로 변경됨
        verify(mockAppStateManager).updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
    }

    @Test
    fun `모든_오디오_중지`() = runTest {
        // Given: TTS 중지 성공
        whenever(mockTtsOrchestrator.stop()).thenAnswer { }

        // When: 모든 오디오 중지
        audioControlManager.stopAllAudio()

        // Then: TTS가 중지됨
        verify(mockTtsOrchestrator).stop()
        
        // And: 모든 버튼에 대해 StopClick 이벤트가 발생함
        verify(mockButtonEventHandler).handleEvent(ButtonEvent.StopClick(ButtonFunction.QuestionPlay))
        verify(mockButtonEventHandler).handleEvent(ButtonEvent.StopClick(ButtonFunction.AnswerPlay))
        verify(mockButtonEventHandler).handleEvent(ButtonEvent.StopClick(ButtonFunction.MemorizeTest))
        verify(mockButtonEventHandler).handleEvent(ButtonEvent.StopClick(ButtonFunction.RecordingPlay))
    }

    @Test
    fun `모든_오디오_중지_실패`() = runTest {
        // Given: TTS 중지 시 예외 발생
        whenever(mockTtsOrchestrator.stop()).thenThrow(RuntimeException("중지 실패"))

        // When: 모든 오디오 중지
        audioControlManager.stopAllAudio()

        // Then: 에러 상태가 설정됨
        val error = audioControlManager.error.first()
        assert(error != null)
        assert(error!!.contains("중지 실패"))
    }

    @Test
    fun `특정_버튼_오디오_중지`() = runTest {
        // Given: 특정 버튼
        val buttonFunction = ButtonFunction.QuestionPlay

        // When: 특정 버튼 오디오 중지
        audioControlManager.stopButtonAudio(buttonFunction)

        // Then: 해당 버튼에 대한 StopClick 이벤트가 발생함
        verify(mockButtonEventHandler).handleEvent(ButtonEvent.StopClick(buttonFunction))
    }

    @Test
    fun `질문_재생_버튼_클릭_처리`() = runTest {
        // Given: QA 아이템과 TTS 재생 성공
        val qaItem = createMockQaItem()
        whenever(mockTtsOrchestrator.speak(any(), any())).thenAnswer { }

        // When: 질문 재생 버튼 클릭
        audioControlManager.handleButtonClick(ButtonFunction.QuestionPlay, qaItem)

        // Then: 질문 재생이 호출됨
        verify(mockTtsOrchestrator).speak(qaItem.questionEn, any())
    }

    @Test
    fun `답변_재생_버튼_클릭_처리`() = runTest {
        // Given: QA 아이템과 TTS 재생 성공
        val qaItem = createMockQaItem()
        whenever(mockTtsOrchestrator.speak(any(), any())).thenAnswer { }

        // When: 답변 재생 버튼 클릭
        audioControlManager.handleButtonClick(ButtonFunction.AnswerPlay, qaItem)

        // Then: 답변 재생이 호출됨
        verify(mockTtsOrchestrator).speak(qaItem.answers.values.first().answerEn, any())
    }

    @Test
    fun `Stop_버튼_클릭_처리`() = runTest {
        // Given: TTS 중지 성공
        whenever(mockTtsOrchestrator.stop()).thenAnswer { }

        // When: Stop 버튼 클릭
        audioControlManager.handleButtonClick(ButtonFunction.Stop)

        // Then: 모든 오디오가 중지됨
        verify(mockTtsOrchestrator).stop()
    }

    @Test
    fun `기타_버튼_클릭_처리`() = runTest {
        // Given: 기타 버튼
        val buttonFunction = ButtonFunction.MemorizeTest

        // When: 기타 버튼 클릭
        audioControlManager.handleButtonClick(buttonFunction)

        // Then: ButtonEventHandler에서 처리됨
        verify(mockButtonEventHandler).handleEvent(ButtonEvent.StopClick(buttonFunction))
    }

    @Test
    fun `버튼_클릭_이벤트_처리_실패`() = runTest {
        // Given: ButtonEventHandler에서 예외 발생
        whenever(mockButtonEventHandler.handleEvent(any())).thenThrow(RuntimeException("처리 실패"))

        // When: 기타 버튼 클릭
        audioControlManager.handleButtonClick(ButtonFunction.MemorizeTest)

        // Then: 에러 상태가 설정됨
        val error = audioControlManager.error.first()
        assert(error != null)
        assert(error!!.contains("처리 실패"))
    }

    @Test
    fun `에러_상태_초기화`() = runTest {
        // Given: 에러 상태 설정
        audioControlManager.playQuestion(createMockQaItem())

        // When: 에러 상태 초기화
        audioControlManager.clearError()

        // Then: 에러 상태가 null로 초기화됨
        val error = audioControlManager.error.first()
        assert(error == null)
    }

    @Test
    fun `재생_상태_업데이트`() = runTest {
        // Given: 재생 상태
        val isQuestionPlaying = true
        val isAnswerPlaying = false
        val isPlaying = true

        // When: 재생 상태 업데이트
        audioControlManager.updatePlayingState(isQuestionPlaying, isAnswerPlaying, isPlaying)

        // Then: 상태가 정상적으로 업데이트됨
        assert(audioControlManager.isQuestionPlaying.first() == isQuestionPlaying)
        assert(audioControlManager.isAnswerPlaying.first() == isAnswerPlaying)
        assert(audioControlManager.isPlaying.first() == isPlaying)
    }

    @Test
    fun `초기_상태_확인`() = runTest {
        // Given: 초기 상태
        // When: 초기 상태 확인
        val isQuestionPlaying = audioControlManager.isQuestionPlaying.first()
        val isAnswerPlaying = audioControlManager.isAnswerPlaying.first()
        val isPlaying = audioControlManager.isPlaying.first()
        val error = audioControlManager.error.first()

        // Then: 초기 상태가 올바르게 설정됨
        assert(!isQuestionPlaying)
        assert(!isAnswerPlaying)
        assert(!isPlaying)
        assert(error == null)
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