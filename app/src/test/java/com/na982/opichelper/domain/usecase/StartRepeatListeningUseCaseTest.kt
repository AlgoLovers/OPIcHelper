package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.state.MemorizationProgressTracker
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.entity.RepeatListeningData
import com.na982.opichelper.domain.audio.RepeatListeningUiCallback
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * StartRepeatListeningUseCase 테스트
 * BASIC_OPERATIONS.md 2-1. 반복듣기 모드 시나리오 기반
 */
class StartRepeatListeningUseCaseTest {

    @Mock
    private lateinit var mockTtsController: TtsController

    @Mock
    private lateinit var mockProgressTracker: MemorizationProgressTracker

    @Mock
    private lateinit var mockRecordingTimeManager: RecordingTimeManager

    @Mock
    private lateinit var mockUiCallback: RepeatListeningUiCallback

    private lateinit var startRepeatListeningUseCase: StartRepeatListeningUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        startRepeatListeningUseCase = StartRepeatListeningUseCase(
            ttsController = mockTtsController,
            progressTracker = mockProgressTracker,
            recordingTimeManager = mockRecordingTimeManager
        )
    }

    @Test
    fun `반복듣기_시나리오_2_1_2_반복듣기_재생_시작`() = runTest {
        // Given: 반복듣기 데이터
        val repeatListeningData = RepeatListeningData(
            category = "bank",
            scriptIndex = 0,
            koreanAnswer = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다.",
            englishAnswer = "You can open a bank account by visiting a bank branch."
        )

        // When: 반복듣기 시작
        startRepeatListeningUseCase.execute(
            data = repeatListeningData,
            uiCallback = mockUiCallback
        )

        // Then: 첫 번째 한글 스크립트 문장이 재생됨
        verify(mockTtsController).playSentenceWithHighlight(any(), eq(true), any())
        
        // And: 한글 재생 시 한글 카드 표시
        verify(mockUiCallback).onCardFlip(isKorean = true)
        
        // And: 한글 텍스트 하이라이트
        verify(mockUiCallback).onKoreanHighlight(any())
    }

    @Test
    fun `반복듣기_시나리오_2_1_3_반복듣기_재생_중단`() = runTest {
        // Given: 반복듣기가 실행 중인 상태
        val repeatListeningData = RepeatListeningData(
            category = "bank",
            scriptIndex = 0,
            koreanAnswer = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다.",
            englishAnswer = "You can open a bank account by visiting a bank branch."
        )

        // 반복듣기 시작
        startRepeatListeningUseCase.execute(
            data = repeatListeningData,
            uiCallback = mockUiCallback
        )

        // When: 반복듣기 중단
        startRepeatListeningUseCase.stop()

        // Then: 현재 재생이 즉시 중단됨
        verify(mockTtsController).stopTts()
        
        // And: 반복듣기 모드가 비활성화됨
        // 실제로는 progressTracker에서 상태 업데이트
    }

    @Test
    fun `반복듣기_시나리오_2_1_5_다른_재생_기능과의_충돌_처리`() = runTest {
        // Given: 반복듣기가 재생 중인 상태
        val repeatListeningData = RepeatListeningData(
            category = "bank",
            scriptIndex = 0,
            koreanAnswer = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다.",
            englishAnswer = "You can open a bank account by visiting a bank branch."
        )

        startRepeatListeningUseCase.execute(
            data = repeatListeningData,
            uiCallback = mockUiCallback
        )

        // When: 다른 재생 기능 실행 (질문 재생)
        startRepeatListeningUseCase.stop()

        // Then: 반복듣기 재생이 즉시 중단됨
        verify(mockTtsController).stopTts()
        
        // And: 새로운 재생 기능이 정상적으로 실행됨
        // (실제로는 다른 UseCase에서 처리)
        
        // And: 반복듣기 모드가 비활성화됨
        // 실제로는 progressTracker에서 상태 업데이트
    }

    @Test
    fun `반복듣기_시나리오_2_1_6_답변_스크립트_완료`() = runTest {
        // Given: 모든 답변 스크립트가 반복 재생 완료될 때
        val repeatListeningData = RepeatListeningData(
            category = "bank",
            scriptIndex = 0,
            koreanAnswer = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다.",
            englishAnswer = "You can open a bank account by visiting a bank branch."
        )

        // When: 모든 문장 재생 완료
        startRepeatListeningUseCase.execute(
            data = repeatListeningData,
            uiCallback = mockUiCallback
        )

        // Then: 반복듣기 모드가 자동으로 종료됨
        verify(mockUiCallback).onComplete()
        
        // And: 반복듣기 버튼이 비활성화됨
        // 실제로는 progressTracker에서 상태 업데이트
    }

    @Test
    fun `반복듣기_시나리오_2_1_7_백키로_종료_시_TTS_즉시_종료`() = runTest {
        // Given: 반복듣기가 재생 중인 상태
        val repeatListeningData = RepeatListeningData(
            category = "bank",
            scriptIndex = 0,
            koreanAnswer = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다.",
            englishAnswer = "You can open a bank account by visiting a bank branch."
        )

        startRepeatListeningUseCase.execute(
            data = repeatListeningData,
            uiCallback = mockUiCallback
        )

        // When: 백키로 종료 시뮬레이션
        startRepeatListeningUseCase.stop()

        // Then: TTS가 즉시 종료됨
        verify(mockTtsController).stopTts()
        
        // And: 모든 하이라이트가 초기화됨
        verify(mockUiCallback).onKoreanHighlight(-1)
        verify(mockUiCallback).onHighlight(-1)
        
        // And: 앱이 정상적으로 종료됨 (실제로는 Activity에서 처리)
    }
} 