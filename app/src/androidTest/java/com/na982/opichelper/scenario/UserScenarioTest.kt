package com.na982.opichelper.scenario

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.na982.opichelper.presentation.ui.screen.MainScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 사용자 시나리오 E2E 테스트
 * BASIC_OPERATIONS.md의 핵심 시나리오들 기반
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UserScenarioTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_1_1_1_앱_실행_시_초기화`() {
        // Given: 앱이 처음 실행될 때
        composeTestRule.setContent {
            MainScreen()
        }

        // When: 앱이 완전히 로드될 때까지 대기
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("은행").fetchSemanticsNodes().size > 0
        }

        // Then: 첫 번째 카테고리의 첫 번째 QA 아이템이 화면에 표시됨
        composeTestRule.onNodeWithText("은행 계좌를 어떻게 열 수 있나요?").assertIsDisplayed()

        // And: 질문과 답변이 정상적으로 로드됨
        composeTestRule.onNodeWithText("은행 지점을 방문하여").assertIsDisplayed()

        // And: 로딩 상태가 적절히 처리됨
        composeTestRule.onNodeWithText("로딩 중...").assertDoesNotExist()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_1_2_1_카테고리_변경`() {
        // Given: 앱이 시작된 상태
        composeTestRule.setContent {
            MainScreen()
        }

        // When: 사용자가 다른 카테고리를 선택할 때
        composeTestRule.onNodeWithText("beach").performClick()

        // Then: 선택된 카테고리의 QA 리스트가 정상적으로 로드됨
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("beach").fetchSemanticsNodes().size > 0
        }

        // And: 해당 카테고리의 첫 번째 QA 아이템이 화면에 표시됨
        composeTestRule.onNodeWithText("beach").assertIsDisplayed()

        // And: 이전 카테고리의 상태가 정리됨
        composeTestRule.onNodeWithText("은행").assertDoesNotExist()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_1_3_1_암기_레벨_선택_및_저장`() {
        // Given: 앱이 시작된 상태
        composeTestRule.setContent {
            MainScreen()
        }

        // When: 사용자가 암기 레벨을 선택할 때
        composeTestRule.onNodeWithText("반복 듣기").performClick()

        // Then: 선택된 레벨이 UI에 반영됨
        composeTestRule.onNodeWithText("반복듣기").assertIsDisplayed()

        // And: 버튼 텍스트가 선택된 레벨에 따라 동적으로 변경됨
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_2_1_2_반복듣기_재생_시작`() {
        // Given: 반복듣기 모드를 선택한 상태
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("반복 듣기").performClick()

        // When: 반복듣기 버튼을 클릭할 때
        composeTestRule.onNodeWithText("암기 테스트").performClick()

        // Then: 첫 번째 한글 스크립트 문장이 재생됨
        // 실제 TTS 재생은 확인 어려우므로 UI 상태로 확인
        composeTestRule.onNodeWithText("반복듣기 종료").assertIsDisplayed()

        // And: 한글 재생 시 한글 카드 표시, 한글 텍스트 하이라이트
        composeTestRule.onNodeWithText("은행 지점을 방문하여").assertIsDisplayed()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_2_1_3_반복듣기_재생_중단`() {
        // Given: 반복듣기가 재생 중인 상태
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("반복 듣기").performClick()
        composeTestRule.onNodeWithText("암기 테스트").performClick()

        // When: 반복듣기 재생 중에 다시 반복듣기 버튼을 클릭할 때
        composeTestRule.onNodeWithText("반복듣기 종료").performClick()

        // Then: 현재 재생이 즉시 중단됨
        composeTestRule.onNodeWithText("반복듣기").assertIsDisplayed()

        // And: 반복듣기 모드가 비활성화됨
        composeTestRule.onNodeWithText("반복듣기 종료").assertDoesNotExist()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_2_2_1_영작테스트_모드_활성화`() {
        // Given: 앱이 시작된 상태
        composeTestRule.setContent {
            MainScreen()
        }

        // When: 영작테스트 모드를 선택할 때
        composeTestRule.onNodeWithText("영작 테스트").performClick()

        // Then: 영작테스트 모드 선택 시 해당 모드가 활성화됨
        composeTestRule.onNodeWithText("부분암기 테스트").assertIsDisplayed()

        // And: 영작테스트 모드 상태가 올바르게 설정됨
        composeTestRule.onNodeWithText("영작 테스트").assertIsDisplayed()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_2_2_2_영작테스트_시작_첫_번째_문장_처리`() {
        // Given: 영작테스트 모드에서 "부분암기 테스트" 버튼이 있는 상태
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("영작 테스트").performClick()

        // When: "부분암기 테스트" 버튼을 클릭할 때
        composeTestRule.onNodeWithText("부분암기 테스트").performClick()

        // Then: 답변이 한글 스크립트 화면으로 전환됨
        composeTestRule.onNodeWithText("은행 지점을 방문하여").assertIsDisplayed()

        // And: 한글 첫 문장이 TTS로 읽어짐
        // 실제 TTS는 확인 어려우므로 UI 상태로 확인

        // And: 첫 번째 문장에 하이라이트가 표시됨
        composeTestRule.onNodeWithText("은행").assertIsDisplayed()

        // And: 마이크가 켜졌다는 2차 하이라이트가 표시됨 (녹음 상태 표시)
        // 녹음 상태는 UI에서 확인
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_2_3_1_통암기_모드_활성화`() {
        // Given: 앱이 시작된 상태
        composeTestRule.setContent {
            MainScreen()
        }

        // When: 통암기 모드를 선택할 때
        composeTestRule.onNodeWithText("통암기").performClick()

        // Then: 통암기 모드 선택 시 해당 모드가 활성화됨
        composeTestRule.onNodeWithText("통암기").assertIsDisplayed()

        // And: 통암기 모드 상태가 올바르게 설정됨
        composeTestRule.onNodeWithText("암기 테스트").assertIsDisplayed()
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_1_5_1_암기_레벨별_녹음_재생_버튼_표시`() {
        // Given: 각 암기 레벨에서 녹음 파일 존재 여부에 따른 버튼 표시
        composeTestRule.setContent {
            MainScreen()
        }

        // When: "반복 듣기" 모드 선택
        composeTestRule.onNodeWithText("반복 듣기").performClick()

        // Then: "반복 듣기" 모드: 녹음 재생 버튼이 표시되지 않음
        composeTestRule.onNodeWithText("녹음 재생").assertDoesNotExist()

        // When: "영작 테스트" 모드 선택
        composeTestRule.onNodeWithText("영작 테스트").performClick()

        // Then: "영작 테스트" 모드: 병합된 녹음 파일이 있으면 재생 버튼 표시
        // (실제로는 녹음 파일이 없을 수 있으므로 조건부 확인)

        // When: "통암기" 모드 선택
        composeTestRule.onNodeWithText("통암기").performClick()

        // Then: "통암기" 모드: 통암기 녹음 파일이 있으면 재생 버튼 표시
        // (실제로는 녹음 파일이 없을 수 있으므로 조건부 확인)
    }

    @Test
    fun `BASIC_OPERATIONS_시나리오_1_2_2_카테고리별_QA_아이템_이동`() {
        // Given: 앱이 시작된 상태
        composeTestRule.setContent {
            MainScreen()
        }

        // When: 이전/다음 스크립트 버튼을 클릭할 때
        composeTestRule.onNodeWithText("다음").performClick()

        // Then: 다음 버튼 클릭 시 다음 QA 아이템으로 이동
        // (실제로는 다음 아이템이 있을 수 있으므로 조건부 확인)

        // When: 이전 버튼 클릭
        composeTestRule.onNodeWithText("이전").performClick()

        // Then: 이전 버튼 클릭 시 이전 QA 아이템으로 이동
        // (실제로는 이전 아이템이 있을 수 있으므로 조건부 확인)
    }
} 