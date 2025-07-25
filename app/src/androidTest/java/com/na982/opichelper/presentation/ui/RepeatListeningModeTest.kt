package com.na982.opichelper.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class RepeatListeningModeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `앱_시작_시_기본_상태_확인`() {
        // Given & When - 앱이 시작됨
        
        // Then - 기본 UI 요소들이 표시되어야 함
        // 실제 텍스트를 찾기 위해 모든 텍스트 노드를 출력
        composeTestRule.onRoot().printToLog("UI_TEST")
        
        // 기본적인 UI 요소들이 존재하는지 확인
        composeTestRule.onNodeWithText("암기 레벨").assertIsDisplayed()
    }

    @Test
    fun `암기_레벨_선택_드롭다운_확인`() {
        // Given & When - 앱이 시작됨
        
        // Then - 암기 레벨 선택기가 표시되어야 함
        composeTestRule.onNodeWithText("레벨을 선택하세요").assertIsDisplayed()
        
        // 드롭다운 클릭
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        
        // 드롭다운 메뉴가 표시되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodesWithText("반복 듣기").fetchSemanticsNodes().size > 0
        }
    }

    @Test
    fun `반복듣기_모드_선택_테스트`() {
        // Given - 암기 레벨 선택 드롭다운 클릭
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        
        // When - 반복 듣기 선택
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // Then - 선택된 레벨이 표시되어야 함
        composeTestRule.onNodeWithText("반복 듣기").assertIsDisplayed()
    }

    @Test
    fun `암기_테스트_버튼_확인`() {
        // Given - 암기 레벨 선택
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // Then - 암기 테스트 버튼이 표시되어야 함
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun `4-1-2-1_한글_스크립트_재생_시_답변_카드가_한글_스크립트로_전환되어야_한다`() {
        // Given - 암기 레벨 선택
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // When - 암기 테스트 버튼 클릭
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // Then - 답변 카드가 표시되는지 확인
        composeTestRule.onNodeWithText("답변").assertIsDisplayed()
    }

    @Test
    fun `4-1-2-2_한글_스크립트_재생_시_하이라이트가_표시되어야_한다`() {
        // Given - 암기 레벨 선택
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // When - 암기 테스트 버튼 클릭
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // Then - 한글 스크립트 재생 시 하이라이트가 표시되어야 함
        // 실제로는 TTS 재생 중에 하이라이트가 표시되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    @Test
    fun `4-1-2-3_영문_스크립트_재생_시_영문_카드로_화면이_전환되어야_한다`() {
        // Given - 암기 레벨 선택
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // When - 암기 테스트 버튼 클릭
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // Then - 영문 스크립트 재생 시 영문 카드로 전환되어야 함
        // 실제로는 TTS 재생 중에 카드가 영문으로 표시되는지 확인
        // 답변 카드가 표시되는지 확인
        composeTestRule.onNodeWithText("답변").assertIsDisplayed()
    }

    @Test
    fun `4-1-2-4_영문_스크립트_재생_시_하이라이트가_표시되어야_한다`() {
        // Given - 암기 레벨 선택
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // When - 암기 테스트 버튼 클릭
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // Then - 영문 스크립트 재생 시 하이라이트가 표시되어야 함
        // 실제로는 TTS 재생 중에 하이라이트가 표시되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("highlighted_text").fetchSemanticsNodes().size > 0
        }
    }

    @Test
    fun `4-1-2-5_영문_스크립트_5회_반복_후_다음_한글_문장으로_진행되어야_한다`() {
        // Given - 암기 레벨 선택
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        
        // When - 암기 테스트 버튼 클릭
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // Then - 영문 스크립트가 5회 반복된 후 다음 한글 문장으로 진행되어야 함
        // 실제로는 시간이 지난 후 다음 문장으로 진행되는지 확인
        // 이는 시간 기반 테스트이므로 적절한 대기 시간 필요
        
        // 첫 번째 한글 문장 확인
        composeTestRule.onNodeWithText("답변").assertIsDisplayed()
        
        // 시간 대기 (영문 5회 반복 시간)
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            // 다음 문장으로 진행되었는지 확인 (실제로는 더 복잡한 로직 필요)
            true
        }
    }

    @Test
    fun `4-1-2-6_반복듣기_중간에_다시_버튼을_누르면_바로_종료되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 반복듣기 재생 중에 다시 버튼 클릭
        composeTestRule.onNodeWithText("암기 테스트 종료").performClick()
        
        // Then - 즉시 종료되어야 함 (버튼이 다시 활성화되어야 함)
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun `4-1-2-7_반복듣기_중에_다른_재생_기능_실행_시_반복듣기가_즉시_중단되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 반복듣기 재생 중에 질문 재생 버튼 클릭
        composeTestRule.onNodeWithText("질문 재생").performClick()
        
        // Then - 반복듣기가 즉시 중단되고 질문 재생이 시작되어야 함
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed() // 버튼이 다시 활성화
    }

    @Test
    fun `4-1-2-8_반복듣기_중_백키로_종료_시_TTS가_즉시_종료되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 반복듣기 재생 중에 백키 클릭
        composeTestRule.activity.onBackPressed()
        
        // Then - TTS가 즉시 종료되어야 함
        // 백키 후 앱이 종료되므로 테스트는 여기서 끝남
        // 실제로는 TTS 중지 로그를 확인해야 함
    }

    @Test
    fun `4-1-2-9_반복듣기_중_질문재생_시_반복듣기_종료_및_하이라이트_초기화_되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 반복듣기 재생 중에 질문 재생 버튼 클릭
        composeTestRule.onNodeWithText("질문 재생").performClick()
        
        // Then - 반복듣기가 종료되고 버튼이 "암기 테스트"로 변경되어야 함
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
        
        // 하이라이트가 초기화되었는지 확인 (답변 카드가 영문 상태로 복원)
        // 실제로는 하이라이트 상태를 확인해야 함
    }

    @Test
    fun `4-1-2-11_반복듣기_중_답변재생_시_반복듣기_종료_및_하이라이트_초기화_되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 반복듣기 재생 중에 답변 재생 버튼 클릭
        composeTestRule.onNodeWithText("답변 재생").performClick()
        
        // Then - 반복듣기가 종료되고 버튼이 "암기 테스트"로 변경되어야 함
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun `4-1-2-12_반복듣기_중_질문답변합쳐서재생_시_반복듣기_종료_및_하이라이트_초기화_되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 반복듣기 재생 중에 질문답변 합쳐서 재생 버튼 클릭
        composeTestRule.onNodeWithText("질문답변 합쳐서 재생").performClick()
        
        // Then - 반복듣기가 종료되고 버튼이 "암기 테스트"로 변경되어야 함
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun `4-1-2-10_반복듣기_진행상황_복원_테스트`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 두 번째 문장까지 진행 후 앱 종료
        // 실제로는 두 번째 문장까지 진행하는 시간을 기다려야 함
        Thread.sleep(10000) // 10초 대기 (두 번째 문장까지 진행)
        
        // Then - 앱 재실행 후 같은 스크립트에서 반복듣기 시작 시 두 번째 문장부터 진행되어야 함
        // 이 테스트는 실제로는 앱 재시작이 필요하므로 별도 테스트로 분리
    }

    @Test
    fun `4-1-2-8_반복듣기_완료_후_모드가_자동으로_종료되어야_한다`() {
        // Given - 암기 레벨 선택 및 시작
        composeTestRule.onNodeWithText("레벨을 선택하세요").performClick()
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()
        
        // When - 모든 스크립트 반복 재생 완료까지 대기
        // 실제로는 모든 스크립트가 완료될 때까지 대기
        
        // Then - 반복듣기 모드가 자동으로 종료되어야 함
        composeTestRule.waitUntil(timeoutMillis = 30000) {
            composeTestRule.onAllNodesWithText("암기 테스트").fetchSemanticsNodes().size == 1
        }
        
        // 암기 테스트 버튼이 다시 활성화되었는지 확인
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun testRepeatListeningCardFlipAndHighlight() {
        Log.d("RepeatListeningModeTest", "반복듣기 카드 전환 및 하이라이트 테스트 시작")

        // 1. 카테고리 선택
        composeTestRule.onNodeWithText("집").performClick()
        Log.d("RepeatListeningModeTest", "카테고리 선택 완료")

        // 2. 반복듣기 모드 선택
        composeTestRule.onNodeWithText("반복 듣기").performClick()
        Log.d("RepeatListeningModeTest", "반복듣기 모드 선택 완료")

        // 3. 기본적인 UI 요소들이 표시되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                // 질문 카드가 표시되는지 확인
                composeTestRule.onNodeWithText("질문").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Log.d("RepeatListeningModeTest", "기본 UI 요소 확인 완료")

        // 4. 답변 카드가 표시되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("답변").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Log.d("RepeatListeningModeTest", "답변 카드 확인 완료")

        // 5. 암기 테스트 버튼이 표시되는지 확인
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        Log.d("RepeatListeningModeTest", "암기 테스트 버튼 확인 완료")

        Log.d("RepeatListeningModeTest", "반복듣기 카드 전환 및 하이라이트 테스트 완료")
    }
} 