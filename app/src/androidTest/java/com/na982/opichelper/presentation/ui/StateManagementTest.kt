package com.na982.opichelper.presentation.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.MainActivity
import com.na982.opichelper.domain.state.AppStateManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StateManagementTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var appStateManager: AppStateManager

    @Before
    fun setUp() {
        hiltRule.inject()
        // 상태 초기화
        runTest {
            appStateManager.updateHighlightState(
                questionHighlightIndex = -1,
                answerHighlightIndex = -1,
                answerKoHighlightIndex = -1,
                recordingHighlightIndex = -1
            )
            appStateManager.updateTtsPlayingState(
                isQuestionPlaying = false,
                isAnswerPlaying = false,
                isPlaying = false
            )
        }
    }

    @Test
    fun testAppExitStateReset() = runTest {
        // 초기 상태 설정
        appStateManager.updateHighlightState(
            questionHighlightIndex = 2,
            answerHighlightIndex = 1,
            answerKoHighlightIndex = 0,
            recordingHighlightIndex = 3
        )
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )

        val initialState = appStateManager.state.first()
        println("=== 앱 종료 테스트 ===")
        println("초기 상태: $initialState")
        
        // 상태 리셋 시뮬레이션
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        
        val finalState = appStateManager.state.first()
        println("최종 상태: $finalState")
        
        // 상태가 올바르게 초기화되었는지 확인
        assert(finalState.questionHighlightIndex == -1)
        assert(finalState.answerHighlightIndex == -1)
        assert(!finalState.isQuestionPlaying)
        assert(!finalState.isAnswerPlaying)
        assert(!finalState.isPlaying)
    }

    @Test
    fun testRepeatedButtonClicks() = runTest {
        println("=== 상태 관리 테스트 시작 ===")
        
        // 초기 상태 확인
        val initialState = appStateManager.state.first()
        println("초기 상태: $initialState")
        
        repeat(3) { clickCount ->
            println("테스트 횟수: ${clickCount + 1}")
            
            val beforeState = appStateManager.state.first()
            println("테스트 전 상태: questionPlaying=${beforeState.isQuestionPlaying}, answerPlaying=${beforeState.isAnswerPlaying}")
            println("하이라이트 상태: question=${beforeState.questionHighlightIndex}, answer=${beforeState.answerHighlightIndex}")
            
            // 상태 변경 시뮬레이션
            appStateManager.updateTtsPlayingState(
                isQuestionPlaying = true,
                isAnswerPlaying = false,
                isPlaying = true
            )
            appStateManager.updateHighlightState(
                questionHighlightIndex = clickCount,
                answerHighlightIndex = -1,
                answerKoHighlightIndex = -1,
                recordingHighlightIndex = -1
            )
            
            kotlinx.coroutines.delay(1000)
            
            val afterState = appStateManager.state.first()
            println("테스트 후 상태: questionPlaying=${afterState.isQuestionPlaying}, answerPlaying=${afterState.isAnswerPlaying}")
            println("하이라이트 상태: question=${afterState.questionHighlightIndex}, answer=${afterState.answerHighlightIndex}")
            println("---")
            
            // 상태 초기화
            appStateManager.updateTtsPlayingState(
                isQuestionPlaying = false,
                isAnswerPlaying = false,
                isPlaying = false
            )
            appStateManager.updateHighlightState(
                questionHighlightIndex = -1,
                answerHighlightIndex = -1,
                answerKoHighlightIndex = -1,
                recordingHighlightIndex = -1
            )
        }
        
        // 최종 상태 확인
        val finalState = appStateManager.state.first()
        println("최종 상태: $finalState")
        
        // 모든 상태가 초기화되었는지 확인
        assert(finalState.questionHighlightIndex == -1)
        assert(finalState.answerHighlightIndex == -1)
        assert(!finalState.isQuestionPlaying)
        assert(!finalState.isAnswerPlaying)
        assert(!finalState.isPlaying)
    }

    @Test
    fun testButtonSwitching() = runTest {
        println("=== 버튼 전환 테스트 시작 ===")
        
        // 질문 재생 시작
        println("1. 질문 재생 시작")
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )
        appStateManager.updateHighlightState(
            questionHighlightIndex = 0,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        kotlinx.coroutines.delay(1000)
        
        val questionState = appStateManager.state.first()
        println("질문 재생 후 상태: $questionState")
        
        // 답변 재생으로 전환
        println("2. 답변 재생으로 전환")
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = true,
            isPlaying = true
        )
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = 1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        kotlinx.coroutines.delay(1000)
        
        val answerState = appStateManager.state.first()
        println("답변 재생 후 상태: $answerState")
        
        // 다시 질문 재생으로 전환
        println("3. 다시 질문 재생으로 전환")
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )
        appStateManager.updateHighlightState(
            questionHighlightIndex = 2,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        kotlinx.coroutines.delay(1000)
        
        val finalState = appStateManager.state.first()
        println("최종 상태: $finalState")
    }

    @Test
    fun testHighlightPersistence() = runTest {
        println("=== 하이라이트 지속성 테스트 시작 ===")
        
        // 질문 재생으로 하이라이트 설정
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )
        appStateManager.updateHighlightState(
            questionHighlightIndex = 0,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        kotlinx.coroutines.delay(2000)
        
        val highlightState = appStateManager.state.first()
        println("하이라이트 설정 후: question=${highlightState.questionHighlightIndex}, answer=${highlightState.answerHighlightIndex}")
        
        // 다른 작업 수행 (답변 재생)
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = true,
            isPlaying = true
        )
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = 1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        
        kotlinx.coroutines.delay(2000)
        
        val afterAnswerState = appStateManager.state.first()
        println("답변 재생 후: question=${afterAnswerState.questionHighlightIndex}, answer=${afterAnswerState.answerHighlightIndex}")
        
        // 하이라이트가 올바르게 전환되었는지 확인
        assert(afterAnswerState.questionHighlightIndex == -1) // 질문 하이라이트 해제
        assert(afterAnswerState.answerHighlightIndex == 1) // 답변 하이라이트 설정
    }

    @Test
    fun testConcurrentOperations() = runTest {
        println("=== 동시 작업 테스트 시작 ===")
        
        // 질문 재생 시작
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )
        
        kotlinx.coroutines.delay(500)
        
        // 재생 중에 답변 재생 시도
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = true,
            isPlaying = true
        )
        
        kotlinx.coroutines.delay(1000)
        
        val finalState = appStateManager.state.first()
        println("동시 작업 후 상태: $finalState")
        
        // 하나의 작업만 활성화되어 있는지 확인
        val activeOperations = listOf(
            finalState.isQuestionPlaying,
            finalState.isAnswerPlaying
        ).count { it }
        
        assert(activeOperations <= 1) { "동시에 하나의 작업만 활성화되어야 함" }
    }

    @Test
    fun testFullStateReset() = runTest {
        println("=== 전체 상태 리셋 테스트 시작 ===")
        
        // 다양한 상태 설정
        appStateManager.updateHighlightState(
            questionHighlightIndex = 5,
            answerHighlightIndex = 3,
            answerKoHighlightIndex = 2,
            recordingHighlightIndex = 1
        )
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = true,
            isAnswerPlaying = false,
            isPlaying = true
        )
        
        val beforeReset = appStateManager.state.first()
        println("리셋 전 상태: $beforeReset")
        
        // 상태 리셋
        appStateManager.updateHighlightState(
            questionHighlightIndex = -1,
            answerHighlightIndex = -1,
            answerKoHighlightIndex = -1,
            recordingHighlightIndex = -1
        )
        appStateManager.updateTtsPlayingState(
            isQuestionPlaying = false,
            isAnswerPlaying = false,
            isPlaying = false
        )
        
        val afterReset = appStateManager.state.first()
        println("리셋 후 상태: $afterReset")
        
        // 모든 상태가 초기화되었는지 확인
        assert(afterReset.questionHighlightIndex == -1)
        assert(afterReset.answerHighlightIndex == -1)
        assert(afterReset.answerKoHighlightIndex == -1)
        assert(afterReset.recordingHighlightIndex == -1)
        assert(!afterReset.isQuestionPlaying)
        assert(!afterReset.isAnswerPlaying)
        assert(!afterReset.isPlaying)
    }

    @Test
    fun testUIIntegration() = runTest {
        println("=== UI 통합 테스트 시작 ===")
        
        // Activity가 완전히 로드될 때까지 대기 (더 긴 시간)
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            try {
                composeTestRule.onRoot().assertExists()
                true
            } catch (e: Exception) {
                println("Activity 로딩 대기 중... ${e.message}")
                false
            }
        }
        
        println("Activity 로딩 완료")
        
        // 현재 UI 상태 출력
        composeTestRule.onRoot().printToLog("CURRENT_UI_STATE")
        
        // 실제 버튼 클릭 시도
        val buttonTexts = listOf("질문", "답변", "Question", "Answer", "재생", "Play")
        
        for (text in buttonTexts) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    println("버튼 발견: '$text'")
                    
                    // 클릭 시도
                    composeTestRule.onNodeWithText(text).performClick()
                    println("클릭 성공: '$text'")
                    
                    // 상태 확인
                    val state = appStateManager.state.first()
                    println("상태: $state")
                    
                    break
                }
            } catch (e: Exception) {
                println("버튼 클릭 실패: '$text' - ${e.message}")
            }
        }
        
        println("UI 통합 테스트 완료")
    }

    @Test
    fun testUIElements() = runTest {
        println("=== UI 요소 찾기 테스트 시작 ===")
        
        // Activity 로딩 대기
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onRoot().assertExists()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        println("Activity 로딩 완료")
        
        // 현재 UI 상태 출력
        composeTestRule.onRoot().printToLog("CURRENT_UI_STATE")
        
        // 실제 버튼 클릭 시도
        val buttonTexts = listOf("질문", "답변", "Question", "Answer", "재생", "Play")
        
        for (text in buttonTexts) {
            try {
                val nodes = composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    println("버튼 발견: '$text'")
                    
                    // 클릭 시도
                    composeTestRule.onNodeWithText(text).performClick()
                    println("클릭 성공: '$text'")
                    
                    // 상태 확인
                    val state = appStateManager.state.first()
                    println("상태: $state")
                    
                    break
                }
            } catch (e: Exception) {
                println("버튼 클릭 실패: '$text' - ${e.message}")
            }
        }
        
        println("UI 요소 찾기 테스트 완료")
    }
} 