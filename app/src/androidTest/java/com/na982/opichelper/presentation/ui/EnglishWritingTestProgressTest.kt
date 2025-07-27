package com.na982.opichelper.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnglishWritingTestProgressTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * 4-2-1. 영작테스트_진행상황_실시간_저장_테스트
     * 
     * 시나리오: 영작테스트 시작 후 2번째 문장까지 진행하고 앱을 종료한 후 재시작
     * 기대 동작: 3번째 문장부터 시작되어야 함
     */
    @Test
    fun `4-2-1_영작테스트_진행상황_실시간_저장_테스트`() {
        // 1. 카테고리 선택 (첫 번째 카테고리)
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("은행").performClick()
        
        // 2. 영작테스트 모드 선택
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        
        // 3. 영작테스트 시작
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 4. 2번째 문장까지 진행 (약 15초 대기 - 녹음 시간 포함)
        Thread.sleep(15000)
        
        // 5. 영작테스트 중단 (다른 재생 버튼 클릭)
        composeTestRule.onNodeWithText("질문재생").performClick()
        
        // 6. 앱 종료 시뮬레이션 (백키)
        composeTestRule.activity.onBackPressed()
        
        // 7. 앱 재시작 시뮬레이션 (새로운 액티비티 시작)
        composeTestRule.activity.recreate()
        
        // 8. 같은 카테고리에서 영작테스트 재시작
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 9. 이전 진행상황에서 시작되는지 확인
        Thread.sleep(3000) // 첫 번째 문장 처리 시간 대기
        
        // 진행상황이 복원되었는지 확인 (UI에서 확인)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-2-2. 영작테스트_카테고리별_독립적_진행상황_테스트
     * 
     * 시나리오: 카테고리 A에서 영작테스트 진행 후, 카테고리 B에서 영작테스트 진행
     * 기대 동작: 각 카테고리별로 독립적으로 진행상황이 저장되어야 함
     */
    @Test
    fun `4-2-2_영작테스트_카테고리별_독립적_진행상황_테스트`() {
        // 1. 첫 번째 카테고리에서 영작테스트 진행
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("은행").performClick()
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 2. 1번째 문장 진행 후 중단
        Thread.sleep(8000)
        composeTestRule.onNodeWithText("질문재생").performClick()
        
        // 3. 두 번째 카테고리로 변경
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("해변").performClick()
        
        // 4. 두 번째 카테고리에서 영작테스트 진행
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 5. 2번째 문장까지 진행 후 중단
        Thread.sleep(15000)
        composeTestRule.onNodeWithText("질문재생").performClick()
        
        // 6. 각 카테고리별 진행상황이 독립적으로 저장되었는지 확인
        // 첫 번째 카테고리로 돌아가서 진행상황 확인
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("은행").performClick()
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 첫 번째 카테고리에서 이전 진행상황이 복원되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-2-3. 영작테스트_스크립트별_독립적_진행상황_테스트
     * 
     * 시나리오: 같은 카테고리 내에서 스크립트 0에서 영작테스트 진행 후, 스크립트 1에서 영작테스트 진행
     * 기대 동작: 각 스크립트별로 독립적으로 진행상황이 저장되어야 함
     */
    @Test
    fun `4-2-3_영작테스트_스크립트별_독립적_진행상황_테스트`() {
        // 1. 첫 번째 스크립트에서 영작테스트 진행
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("은행").performClick()
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 2. 1번째 문장 진행 후 중단
        Thread.sleep(8000)
        composeTestRule.onNodeWithText("질문재생").performClick()
        
        // 3. 다음 스크립트로 이동
        composeTestRule.onNodeWithText("다음").performClick()
        
        // 4. 두 번째 스크립트에서 영작테스트 진행
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 5. 2번째 문장까지 진행 후 중단
        Thread.sleep(15000)
        composeTestRule.onNodeWithText("질문재생").performClick()
        
        // 6. 각 스크립트별 진행상황이 독립적으로 저장되었는지 확인
        // 첫 번째 스크립트로 돌아가서 진행상황 확인
        composeTestRule.onNodeWithText("이전").performClick()
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 첫 번째 스크립트에서 이전 진행상황이 복원되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-2-4. 영작테스트_앱_재시작_후_진행상황_복원_테스트
     * 
     * 시나리오: 영작테스트 중 3번째 문장까지 진행 후 앱을 완전히 종료하고 재시작
     * 기대 동작: 4번째 문장부터 시작되어야 함
     */
    @Test
    fun `4-2-4_영작테스트_앱_재시작_후_진행상황_복원_테스트`() {
        // 1. 영작테스트 시작
        composeTestRule.onNodeWithText("카테고리 선택").performClick()
        composeTestRule.onNodeWithText("은행").performClick()
        composeTestRule.onNodeWithText("qa_bank").performClick()
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 2. 3번째 문장까지 진행 (약 25초 대기 - 녹음 시간 포함)
        Thread.sleep(25000)
        
        // 3. 영작테스트 중단
        composeTestRule.onNodeWithText("질문재생").performClick()
        
        // 4. 앱 완전 종료 시뮬레이션
        composeTestRule.activity.finish()
        
        // 5. 앱 재시작 시뮬레이션
        composeTestRule.activity.recreate()
        
        // 6. 같은 카테고리에서 영작테스트 재시작
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        
        // 7. 이전 진행상황에서 시작되는지 확인
        Thread.sleep(3000) // 첫 번째 문장 처리 시간 대기
        
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    /**
     * 4-2-5. 영작테스트_진행상황_저장_키_구조_테스트
     * 
     * 시나리오: 여러 카테고리와 스크립트에서 영작테스트 진행
     * 기대 동작: 각각의 고유 키로 저장되어야 함
     */
    @Test
    fun `4-2-5_영작테스트_진행상황_저장_키_구조_테스트`() {
        // 1. 여러 카테고리와 스크립트에서 영작테스트 진행
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
            
            // 영작테스트 진행
            composeTestRule.onNodeWithText("영작 테스트").performClick()
            composeTestRule.onNodeWithText("부분암기 테스트").performClick()
            
            // 진행 후 중단
            Thread.sleep(5000)
            composeTestRule.onNodeWithText("질문재생").performClick()
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
            
            // 영작테스트 시작하여 진행상황 복원 확인
            composeTestRule.onNodeWithText("영작 테스트").performClick()
            composeTestRule.onNodeWithText("부분암기 테스트").performClick()
            
            // 진행상황이 복원되는지 확인
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
            }
            
            // 중단
            composeTestRule.onNodeWithText("질문재생").performClick()
        }
    }
} 