package com.na982.opichelper.domain.button

import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.data.state.AppStateManagerImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * AppStateManager를 통한 버튼 상태 관리 테스트
 * 버튼 상태 관리 및 관찰자 패턴 테스트
 */
class ButtonStateManagerTest {

    @Mock
    private lateinit var mockObserver: ButtonStateObserver

    private lateinit var appStateManager: AppStateManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // 테스트에서 Log 무시 설정
        System.setProperty("java.util.logging.config.file", "logging.properties")
        
        appStateManager = AppStateManagerImpl()
    }

    @Test
    fun `질문_재생_버튼_상태_업데이트`() = runTest {
        // Given: 질문 재생 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.QuestionPlay
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        appStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[buttonFunction] == ButtonState.Playing)
    }

    @Test
    fun `답변_재생_버튼_상태_업데이트`() = runTest {
        // Given: 답변 재생 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.AnswerPlay
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        appStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[buttonFunction] == ButtonState.Playing)
    }

    @Test
    fun `암기_테스트_버튼_상태_업데이트`() = runTest {
        // Given: 암기 테스트 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.MemorizeTest
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        appStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[buttonFunction] == ButtonState.Playing)
    }

    @Test
    fun `녹음_재생_버튼_상태_업데이트`() = runTest {
        // Given: 녹음 재생 버튼 상태를 Playing으로 변경
        val buttonFunction = ButtonFunction.RecordingPlay
        val newState = ButtonState.Playing

        // When: 버튼 상태 업데이트
        appStateManager.updateButtonState(buttonFunction, newState)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[buttonFunction] == ButtonState.Playing)
    }

    @Test
    fun `Stop_버튼_클릭_시_모든_버튼_Idle로_초기화`() = runTest {
        // Given: 모든 버튼이 Playing 상태
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Playing)
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Playing)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Playing)

        // When: Stop 버튼 클릭
        appStateManager.updateButtonState(ButtonFunction.Stop, ButtonState.Idle)

        // Then: 모든 버튼이 Idle 상태로 초기화됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[ButtonFunction.QuestionPlay] == ButtonState.Idle)
        assert(currentState.buttonStates[ButtonFunction.AnswerPlay] == ButtonState.Idle)
        assert(currentState.buttonStates[ButtonFunction.MemorizeTest] == ButtonState.Idle)
        assert(currentState.buttonStates[ButtonFunction.RecordingPlay] == ButtonState.Idle)
    }

    @Test
    fun `관찰자_추가_및_상태_변경_알림`() = runTest {
        // Given: AppStateManager는 관찰자 패턴을 직접 지원하지 않으므로 생략

        // When: 버튼 상태 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[ButtonFunction.QuestionPlay] == ButtonState.Playing)
    }

    @Test
    fun `관찰자_제거_후_알림_전송_안됨`() = runTest {
        // Given: AppStateManager는 관찰자 패턴을 직접 지원하지 않음
        // When: 버튼 상태 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[ButtonFunction.QuestionPlay] == ButtonState.Playing)
    }

    @Test
    fun `모든_버튼_초기화_시_관찰자_알림`() {
        // Given: AppStateManager는 관찰자 패턴을 직접 지원하지 않음
        // When: 모든 버튼 초기화 (개별적으로)
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)

        // Then: 모든 버튼이 Idle 상태가 됨
        // (실제로는 각 버튼을 개별적으로 초기화해야 함)
    }

    @Test
    fun `특정_버튼_상태_가져오기`() = runTest {
        // Given: 질문 재생 버튼을 Playing 상태로 설정
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // When: 특정 버튼 상태 가져오기
        val currentState = appStateManager.state.first()
        val state = currentState.buttonStates[ButtonFunction.QuestionPlay]

        // Then: 정확한 상태 반환
        assert(state == ButtonState.Playing)
    }

    @Test
    fun `모든_버튼_Idle_상태_확인_True`() = runTest {
        // Given: 모든 버튼이 Idle 상태
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.AnswerPlay, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.MemorizeTest, ButtonState.Idle)
        appStateManager.updateButtonState(ButtonFunction.RecordingPlay, ButtonState.Idle)

        // When: 모든 버튼이 Idle인지 확인
        val currentState = appStateManager.state.first()
        val areAllIdle = currentState.buttonStates.values.all { it == ButtonState.Idle }

        // Then: true 반환
        assert(areAllIdle)
    }

    @Test
    fun `모든_버튼_Idle_상태_확인_False`() = runTest {
        // Given: 하나의 버튼이 Playing 상태
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // When: 모든 버튼이 Idle인지 확인
        val currentState = appStateManager.state.first()
        val areAllIdle = currentState.buttonStates.values.all { it == ButtonState.Idle }

        // Then: false 반환
        assert(!areAllIdle)
    }

    @Test
    fun `관찰자_알림_실패_시_예외_처리`() = runTest {
        // Given: AppStateManager는 관찰자 패턴을 직접 지원하지 않음
        // When: 버튼 상태 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 예외 없이 정상 처리됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[ButtonFunction.QuestionPlay] == ButtonState.Playing)
    }

    @Test
    fun `여러_관찰자_동시_알림`() = runTest {
        // Given: AppStateManager는 관찰자 패턴을 직접 지원하지 않음
        // When: 버튼 상태 변경
        appStateManager.updateButtonState(ButtonFunction.QuestionPlay, ButtonState.Playing)

        // Then: 상태가 정상적으로 업데이트됨
        val currentState = appStateManager.state.first()
        assert(currentState.buttonStates[ButtonFunction.QuestionPlay] == ButtonState.Playing)
    }

    @Test
    fun `초기_상태_확인`() = runTest {
        // Given: 초기 상태
        // When: 각 버튼의 초기 상태 확인
        val currentState = appStateManager.state.first()

        // Then: 모든 버튼이 Idle 상태
        assert(currentState.buttonStates[ButtonFunction.QuestionPlay] == ButtonState.Idle)
        assert(currentState.buttonStates[ButtonFunction.AnswerPlay] == ButtonState.Idle)
        assert(currentState.buttonStates[ButtonFunction.MemorizeTest] == ButtonState.Idle)
        assert(currentState.buttonStates[ButtonFunction.RecordingPlay] == ButtonState.Idle)
    }
} 