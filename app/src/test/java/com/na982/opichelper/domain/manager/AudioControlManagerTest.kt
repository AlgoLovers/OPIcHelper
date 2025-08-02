package com.na982.opichelper.domain.manager

import com.na982.opichelper.LogIgnoreRule
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.ButtonState
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.*
import org.mockito.MockitoAnnotations

class AudioControlManagerTest {

    @get:Rule
    val logIgnoreRule = LogIgnoreRule()

    @Mock
    private lateinit var mockTtsController: TtsController

    private lateinit var audioControlManager: AudioControlManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        audioControlManager = AudioControlManager(
            ttsController = mockTtsController
        )
    }

    @Test
    fun `질문_재생_성공`() = runTest {
        // Given: QA 아이템과 TTS 재생 성공
        val qaItem = createMockQaItem()
        var completionCalled = false
        
        whenever(mockTtsController.playQuestion(any())).thenAnswer { 
            completionCalled = true
            Unit
        }

        // When: 질문 재생
        audioControlManager.playQuestion(qaItem) {
            completionCalled = true
        }

        // Then: TTS가 정상적으로 호출됨
        verify(mockTtsController).playQuestion(qaItem.questionEn)
        
        // And: 콜백이 호출됨
        assert(completionCalled)
    }

    @Test
    fun `질문_재생_실패`() = runTest {
        // Given: TTS 재생 시 예외 발생
        val qaItem = createMockQaItem()
        var completionCalled = false
        
        whenever(mockTtsController.playQuestion(any())).thenThrow(RuntimeException("재생 실패"))

        // When: 질문 재생
        audioControlManager.playQuestion(qaItem) {
            completionCalled = true
        }

        // Then: 에러 상태가 설정됨
        val error = audioControlManager.error.first()
        assert(error != null)
        assert(error!!.contains("재생 실패"))
        
        // And: 에러 시에도 콜백이 호출됨
        assert(completionCalled)
    }

    @Test
    fun `답변_재생_성공`() = runTest {
        // Given: QA 아이템과 TTS 재생 성공
        val qaItem = createMockQaItem()
        var completionCalled = false
        
        whenever(mockTtsController.playAnswer(any())).thenAnswer { 
            completionCalled = true
            Unit
        }

        // When: 답변 재생
        audioControlManager.playAnswer(qaItem) {
            completionCalled = true
        }

        // Then: TTS가 정상적으로 호출됨
        verify(mockTtsController).playAnswer(qaItem.answers.values.first().answerEn)
        
        // And: 콜백이 호출됨
        assert(completionCalled)
    }

    @Test
    fun `답변_재생_실패`() = runTest {
        // Given: TTS 재생 시 예외 발생
        val qaItem = createMockQaItem()
        var completionCalled = false
        
        whenever(mockTtsController.playAnswer(any())).thenThrow(RuntimeException("재생 실패"))

        // When: 답변 재생
        audioControlManager.playAnswer(qaItem) {
            completionCalled = true
        }

        // Then: 에러 상태가 설정됨
        val error = audioControlManager.error.first()
        assert(error != null)
        assert(error!!.contains("재생 실패"))
        
        // And: 에러 시에도 콜백이 호출됨
        assert(completionCalled)
    }

    @Test
    fun `모든_오디오_중지`() = runTest {
        // Given: TTS 중지 성공
        whenever(mockTtsController.stopAllTts()).thenAnswer { }

        // When: 모든 오디오 중지
        audioControlManager.stopAllAudio()

        // Then: TTS가 중지됨
        verify(mockTtsController).stopAllTts()
    }

    @Test
    fun `특정_오디오_중지_질문`() = runTest {
        // Given: TTS 중지 성공
        whenever(mockTtsController.stopAllTts()).thenAnswer { }

        // When: 질문 오디오 중지
        audioControlManager.stopSpecificAudio(ButtonFunction.QuestionPlay.toString())

        // Then: TTS가 중지됨
        verify(mockTtsController).stopAllTts()
    }

    @Test
    fun `특정_오디오_중지_답변`() = runTest {
        // Given: TTS 중지 성공
        whenever(mockTtsController.stopAllTts()).thenAnswer { }

        // When: 답변 오디오 중지
        audioControlManager.stopSpecificAudio(ButtonFunction.AnswerPlay.toString())

        // Then: TTS가 중지됨
        verify(mockTtsController).stopAllTts()
    }

    @Test
    fun `알_수_없는_버튼_함수_중지`() = runTest {
        // When: 알 수 없는 버튼 함수로 중지
        audioControlManager.stopSpecificAudio("UNKNOWN")

        // Then: TTS가 호출되지 않음
        verify(mockTtsController, never()).stopAllTts()
    }

    @Test
    fun `에러_상태_초기화`() = runTest {
        // Given: 에러 상태가 설정됨
        audioControlManager.playQuestion(createMockQaItem()) {
            // 에러 발생
        }
        
        // When: 새로운 재생 시작
        audioControlManager.playQuestion(createMockQaItem()) {
            // 성공
        }

        // Then: 에러가 초기화됨
        val error = audioControlManager.error.first()
        assert(error == null)
    }

    private fun createMockQaItem(): QaItem {
        return QaItem(
            id = "test_001",
            category = "test",
            questionEn = "Tell me about how you usually clean your house.",
            questionKo = "당신이 보통 집을 어떻게 청소하는지 말해주세요.",
            answers = mapOf(
                UserLevel.IM to LeveledAnswer(
                    answerEn = "I usually clean my house on weekends.",
                    answerKo = "저는 보통 주말에 집을 청소합니다."
                )
            )
        )
    }
} 