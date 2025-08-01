package com.na982.opichelper.domain.button

import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * ButtonStateManager 테스트
 * 버튼 상태 관리 및 관찰자 패턴 테스트
 */
class ButtonStateManagerTest {

    @Mock
    private lateinit var mockObserver: ButtonStateObserver

    private lateinit var buttonStateManager: ButtonStateManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        buttonStateManager = ButtonStateManager()
    }

    @Test
    fun `질문_재생_버튼_상태_업데이트`() = runTest {
        // Given: 질문 재생 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.QuestionPlay
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        buttonStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = buttonStateManager.questionPlayState.first()
        assert(currentState == ButtonState.Playing)
    }

    @Test
    fun `답변_재생_버튼_상태_업데이트`() = runTest {
        // Given: 답변 재생 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.AnswerPlay
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        buttonStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = buttonStateManager.answerPlayState.first()
        assert(currentState == ButtonState.Playing)
    }

    @Test
    fun `암기_테스트_버튼_상태_업데이트`() = runTest {
        // Given: 암기 테스트 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.MemorizeTest
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        buttonStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = buttonStateManager.memorizeTestState.first()
        assert(currentState == ButtonState.Playing)
    }

    @Test
    fun `녹음_재생_버튼_상태_업데이트`() = runTest {
        // Given: 녹음 재생 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.RecordingPlay
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        buttonStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = buttonStateManager.recordingPlayState.first()
        assert(currentState == ButtonState.Playing)
    }

    @Test
    fun `Stop_버튼_클릭_시_모든_버튼_Idle로_초기화`() = runTest {
        // Given: 모든 버튼이 Playing 상태
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
        buttonStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)
        buttonStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
        buttonStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Playing)

        // When: Stop 버튼 클릭
        buttonStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)

        // Then: 모든 버튼이 Idle 상태로 초기화됨
        assert(buttonStateManager.questionPlayState.first() == ButtonState.Idle)
        assert(buttonStateManager.answerPlayState.first() == ButtonState.Idle)
        assert(buttonStateManager.memorizeTestState.first() == ButtonState.Idle)
        assert(buttonStateManager.recordingPlayState.first() == ButtonState.Idle)
    }

    @Test
    fun `관찰자_추가_및_상태_변경_알림`() = runTest {
        // Given: 관찰자 추가
        buttonStateManager.addObserver(mockObserver)

        // When: 버튼 상태 변경
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 관찰자에게 알림이 전송됨
        verify(mockObserver).onButtonStateChanged(ButtonFunction.QuestionPlay, ButtonState.Playing)
    }

    @Test
    fun `관찰자_제거_후_알림_전송_안됨`() = runTest {
        // Given: 관찰자 추가 후 제거
        buttonStateManager.addObserver(mockObserver)
        buttonStateManager.removeObserver(mockObserver)

        // When: 버튼 상태 변경
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 관찰자에게 알림이 전송되지 않음
        verify(mockObserver, never()).onButtonStateChanged(any(), any())
    }

    @Test
    fun `모든_버튼_초기화_시_관찰자_알림`() {
        // Given: 관찰자 추가
        buttonStateManager.addObserver(mockObserver)

        // When: 모든 버튼 초기화
        buttonStateManager.resetAllButtonStates()

        // Then: 관찰자에게 초기화 알림이 전송됨
        verify(mockObserver).onAllButtonsReset()
    }

    @Test
    fun `특정_버튼_상태_가져오기`() = runTest {
        // Given: 질문 재생 버튼을 Playing 상태로 설정
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // When: 특정 버튼 상태 가져오기
        val state = buttonStateManager.getButtonState(ButtonFunction.QuestionPlay)

        // Then: 정확한 상태 반환
        assert(state == ButtonState.Playing)
    }

    @Test
    fun `모든_버튼_Idle_상태_확인_True`() = runTest {
        // Given: 모든 버튼이 Idle 상태
        buttonStateManager.resetAllButtonStates()

        // When: 모든 버튼이 Idle인지 확인
        val areAllIdle = buttonStateManager.areAllButtonsIdle()

        // Then: true 반환
        assert(areAllIdle)
    }

    @Test
    fun `모든_버튼_Idle_상태_확인_False`() = runTest {
        // Given: 하나의 버튼이 Playing 상태
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // When: 모든 버튼이 Idle인지 확인
        val areAllIdle = buttonStateManager.areAllButtonsIdle()

        // Then: false 반환
        assert(!areAllIdle)
    }

    @Test
    fun `관찰자_알림_실패_시_예외_처리`() = runTest {
        // Given: 알림 실패하는 관찰자
        val failingObserver = mock<ButtonStateObserver>()
        whenever(failingObserver.onButtonStateChanged(any(), any())).thenThrow(RuntimeException("알림 실패"))
        
        buttonStateManager.addObserver(failingObserver)

        // When: 버튼 상태 변경
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 예외가 발생해도 앱이 크래시되지 않음
        // (로그만 출력되고 정상적으로 처리됨)
    }

    @Test
    fun `여러_관찰자_동시_알림`() = runTest {
        // Given: 여러 관찰자 추가
        val observer1 = mock<ButtonStateObserver>()
        val observer2 = mock<ButtonStateObserver>()
        
        buttonStateManager.addObserver(observer1)
        buttonStateManager.addObserver(observer2)

        // When: 버튼 상태 변경
        buttonStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 모든 관찰자에게 알림이 전송됨
        verify(observer1).onButtonStateChanged(ButtonFunction.QuestionPlay, ButtonState.Playing)
        verify(observer2).onButtonStateChanged(ButtonFunction.QuestionPlay, ButtonState.Playing)
    }

    @Test
    fun `초기_상태_확인`() = runTest {
        // Given: 초기 상태
        // When: 각 버튼의 초기 상태 확인
        val questionState = buttonStateManager.questionPlayState.first()
        val answerState = buttonStateManager.answerPlayState.first()
        val memorizeState = buttonStateManager.memorizeTestState.first()
        val recordingState = buttonStateManager.recordingPlayState.first()

        // Then: 모든 버튼이 Idle 상태
        assert(questionState == ButtonState.Idle)
        assert(answerState == ButtonState.Idle)
        assert(memorizeState == ButtonState.Idle)
        assert(recordingState == ButtonState.Idle)
    }
} 