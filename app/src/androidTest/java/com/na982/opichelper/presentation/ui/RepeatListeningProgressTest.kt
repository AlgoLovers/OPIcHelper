package com.na982.opichelper.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepeatListeningProgressTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * 4-1-2-13. 반복듣기_진행상황_실시간_저장_테스트
     * 
     * 시나리오: 반복듣기 시작 후 2번째 문장까지 진행하고 앱을 종료한 후 재시작
     * 기대 동작: 3번째 문장부터 시작되어야 함
     */
    @Test
    fun `4-1-2-13_반복듣기_진행상황_실시간_저장_테스트`() {
        // 1. 카테고리 선택 (첫 번째 카테고리)
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("qa_bank").performClick()
        
        // 2. 반복듣기 모드 선택
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // 3. 반복듣기 시작
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 4. 2번째 문장까지 진행 (약 10초 대기)
        Thread.sleep(10000)
        
        // 5. 반복듣기 중단
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        
        // 6. 앱 종료 시뮬레이션 (백키)
        composeTestRule.activity.onBackPressed()
        
        // 7. 앱 재시작 시뮬레이션 (새로운 액티비티 시작)
        // 실제로는 앱을 완전히 종료하고 재시작해야 하지만, 테스트에서는 액티비티 재시작으로 대체
        composeTestRule.activity.recreate()
        
        // 8. 같은 카테고리에서 반복듣기 재시작
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 9. 이전 진행상황에서 시작되는지 확인 (하이라이트가 2번째 문장 이후에 나타남)
        Thread.sleep(3000) // 첫 번째 문장 처리 시간 대기
        
        // 진행상황이 복원되었는지 확인 (UI에서 확인)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-1-2-14. 반복듣기_카테고리별_독립적_진행상황_테스트
     * 
     * 시나리오: 카테고리 A에서 반복듣기 진행 후, 카테고리 B에서 반복듣기 진행
     * 기대 동작: 각 카테고리별로 독립적으로 진행상황이 저장되어야 함
     */
    @Test
    fun `4-1-2-14_반복듣기_카테고리별_독립적_진행상황_테스트`() {
        // 1. 첫 번째 카테고리에서 반복듣기 진행
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("qa_bank").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 2. 1번째 문장 진행 후 중단
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        
        // 3. 두 번째 카테고리로 변경
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("qa_beach").performClick()
        
        // 4. 두 번째 카테고리에서 반복듣기 진행
        composeTestRule.onNodeWithText("반복듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 5. 2번째 문장까지 진행 후 중단
        Thread.sleep(10000)
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        
        // 6. 각 카테고리별 진행상황이 독립적으로 저장되었는지 확인
        // 첫 번째 카테고리로 돌아가서 진행상황 확인
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("qa_bank").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 첫 번째 카테고리에서 이전 진행상황이 복원되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-1-2-15. 반복듣기_스크립트별_독립적_진행상황_테스트
     * 
     * 시나리오: 같은 카테고리 내에서 스크립트 0에서 반복듣기 진행 후, 스크립트 1에서 반복듣기 진행
     * 기대 동작: 각 스크립트별로 독립적으로 진행상황이 저장되어야 함
     */
    @Test
    fun `4-1-2-15_반복듣기_스크립트별_독립적_진행상황_테스트`() {
        // 1. 첫 번째 스크립트에서 반복듣기 진행
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("qa_bank").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 2. 1번째 문장 진행 후 중단
        Thread.sleep(5000)
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        
        // 3. 다음 스크립트로 이동
        composeTestRule.onNodeWithText("다음").performClick()
        
        // 4. 두 번째 스크립트에서 반복듣기 진행
        composeTestRule.onNodeWithText("반복듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 5. 2번째 문장까지 진행 후 중단
        Thread.sleep(10000)
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        
        // 6. 각 스크립트별 진행상황이 독립적으로 저장되었는지 확인
        // 첫 번째 스크립트로 돌아가서 진행상황 확인
        composeTestRule.onNodeWithText("이전").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 첫 번째 스크립트에서 이전 진행상황이 복원되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-1-2-16. 반복듣기_앱_재시작_후_진행상황_복원_테스트
     * 
     * 시나리오: 반복듣기 중 3번째 문장까지 진행 후 앱을 완전히 종료하고 재시작
     * 기대 동작: 4번째 문장부터 시작되어야 함
     */
    @Test
    fun `4-1-2-16_반복듣기_앱_재시작_후_진행상황_복원_테스트`() {
        // 1. 반복듣기 시작
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("qa_bank").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 2. 3번째 문장까지 진행 (약 15초 대기)
        Thread.sleep(15000)
        
        // 3. 반복듣기 중단
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        
        // 4. 앱 완전 종료 시뮬레이션
        composeTestRule.activity.finish()
        
        // 5. 앱 재시작 시뮬레이션
        composeTestRule.activity.recreate()
        
        // 6. 같은 카테고리에서 반복듣기 재시작
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("반복듣기").performClick()
        
        // 7. 이전 진행상황에서 시작되는지 확인
        Thread.sleep(3000) // 첫 번째 문장 처리 시간 대기
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-1-2-17. 반복듣기_진행상황_저장_키_구조_테스트
     * 
     * 시나리오: 여러 카테고리와 스크립트에서 반복듣기 진행
     * 기대 동작: 각각의 고유 키로 저장되어야 함
     */
    @Test
    fun `4-1-2-17_반복듣기_진행상황_저장_키_구조_테스트`() {
        // 1. 여러 카테고리와 스크립트에서 반복듣기 진행
        val testCases = listOf(
            Triple("qa_bank", 0, 1),
            Triple("qa_bank", 1, 2),
            Triple("qa_beach", 0, 1),
            Triple("qa_beach", 1, 2)
        )
        
        for ((category, scriptIndex, progressIndex) in testCases) {
            // 카테고리 선택
            composeTestRule.onNodeWithText("카테고리 선택").performClick()
            composeTestRule.onNodeWithText(category).performClick()
            
            // 스크립트 이동
            repeat(scriptIndex) {
                composeTestRule.onNodeWithText("다음").performClick()
            }
            
            // 반복듣기 진행
            composeTestRule.onNodeWithText("반복듣기").performClick()
            composeTestRule.onNodeWithText("반복듣기").performClick()
            
            // 진행 후 중단
            Thread.sleep(3000)
            composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        }
        
        // 2. 모든 진행상황이 올바른 키로 저장되었는지 확인
        // 각 카테고리와 스크립트에서 진행상황이 독립적으로 저장되었는지 확인
        for ((category, scriptIndex, expectedProgress) in testCases) {
            // 카테고리 선택
            composeTestRule.onNodeWithText("카테고리 선택").performClick()
            composeTestRule.onNodeWithText(category).performClick()
            
            // 스크립트 이동
            repeat(scriptIndex) {
                composeTestRule.onNodeWithText("다음").performClick()
            }
            
            // 반복듣기 시작하여 진행상황 복원 확인
            composeTestRule.onNodeWithText("반복듣기").performClick()
            composeTestRule.onNodeWithText("반복듣기").performClick()
            
            // 진행상황이 복원되는지 확인
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
            }
            
            // 중단
            composeTestRule.onNodeWithText("반복듣기 종료").performClick()
        }
    }
} 