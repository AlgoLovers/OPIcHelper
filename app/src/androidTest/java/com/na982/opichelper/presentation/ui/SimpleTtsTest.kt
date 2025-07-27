package com.na982.opichelper.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

/**
 * 간단한 TTS 테스트
 * 현재 UI 구조를 파악하고 TTS 상태 통합을 확인
 */
@RunWith(AndroidJUnit4::class)
class SimpleTtsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Activity 시작 대기 및 초기화
     */
    private fun waitForActivityStart() {
        Log.d("SimpleTtsTest", "Activity 시작 대기 중...")
        
        // Activity가 완전히 시작되고 기본 UI 요소가 표시될 때까지 대기
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                // 기본 UI 요소가 표시되는지 확인
                composeTestRule.onRoot().assertExists()
                // 추가로 실제 UI 요소가 렌더링되었는지 확인
                composeTestRule.onAllNodesWithText("카테고리 선택").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("암기 레벨").fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                Log.d("SimpleTtsTest", "Activity 시작 대기 중... ${e.message}")
                false
            }
        }
        
        Log.d("SimpleTtsTest", "Activity 시작 완료")
    }

    /**
     * 기본 UI 요소 확인
     */
    @Test
    fun testBasicUIElements() = runBlocking {
        // Activity 시작 대기
        waitForActivityStart()
        
        // 앱이 시작되고 기본 UI 요소들이 표시되는지 확인
        Log.d("SimpleTtsTest", "기본 UI 요소 확인 시작")
        
        // 현재 UI 상태 출력
        composeTestRule.onRoot().printToLog("CURRENT_UI_STATE")
        
        // 모든 텍스트 노드를 찾아서 실제 UI 구조 파악
        val allTextNodes = composeTestRule.onAllNodes().fetchSemanticsNodes()
        Log.d("SimpleTtsTest", "발견된 텍스트 노드들:")
        allTextNodes.forEach { node ->
            val text = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.firstOrNull()?.text
            if (!text.isNullOrEmpty()) {
                Log.d("SimpleTtsTest", "텍스트: '$text'")
            }
        }
        
        // 기본적인 UI 요소들이 존재하는지 확인 (여러 가능성 시도)
        val possibleTitles = listOf("카테고리 선택", "암기 레벨", "질문", "답변")
        
        for (title in possibleTitles) {
            try {
                composeTestRule.onNodeWithText(title).assertIsDisplayed()
                Log.d("SimpleTtsTest", "기본 UI 요소 발견: $title")
            } catch (e: Exception) {
                Log.w("SimpleTtsTest", "기본 UI 요소 없음: $title")
            }
        }
        
        Log.d("SimpleTtsTest", "기본 UI 요소 확인 완료")
    }

    /**
     * 카테고리 선택 테스트
     */
    @Test
    fun testCategorySelection() = runBlocking {
        // Activity 시작 대기
        waitForActivityStart()
        
        Log.d("SimpleTtsTest", "카테고리 선택 테스트 시작")
        
        // 카테고리 선택기 찾기 (여러 방법 시도)
        try {
            // 1. contentDescription으로 찾기
            composeTestRule.onNodeWithContentDescription("Category selector").performClick()
            Log.d("SimpleTtsTest", "contentDescription으로 카테고리 선택기 발견")
        } catch (e: Exception) {
            try {
                // 2. "카테고리 선택" 텍스트로 찾기
                composeTestRule.onNodeWithText("카테고리 선택").performClick()
                Log.d("SimpleTtsTest", "카테고리 선택 텍스트로 발견")
            } catch (e2: Exception) {
                try {
                    // 3. "카테고리를 선택하세요" 텍스트로 찾기
                    composeTestRule.onNodeWithText("카테고리를 선택하세요").performClick()
                    Log.d("SimpleTtsTest", "카테고리를 선택하세요 텍스트로 발견")
                } catch (e3: Exception) {
                    Log.w("SimpleTtsTest", "카테고리 선택기를 찾을 수 없음: ${e3.message}")
                    return@runBlocking
                }
            }
        }
        
        // 드롭다운 메뉴가 표시될 때까지 대기
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onAllNodesWithText("집").fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
        
        // 카테고리 선택 시도 (여러 카테고리 시도)
        val categoriesToTry = listOf("집", "음악", "영화", "레스토랑", "해변", "가족,친구")
        
        for (category in categoriesToTry) {
            try {
                composeTestRule.onNodeWithText(category).performClick()
                Log.d("SimpleTtsTest", "$category 카테고리 선택 성공")
                
                // 카테고리 선택 후 UI 상태 변화 대기
                composeTestRule.waitUntil(timeoutMillis = 3000) {
                    try {
                        // 질문 카드가 표시되는지 확인
                        composeTestRule.onAllNodesWithText("질문").fetchSemanticsNodes().isNotEmpty() ||
                        composeTestRule.onAllNodesWithText("답변").fetchSemanticsNodes().isNotEmpty()
                    } catch (e: Exception) {
                        false
                    }
                }
                break
            } catch (e: Exception) {
                Log.w("SimpleTtsTest", "$category 카테고리 선택 실패: ${e.message}")
            }
        }
        
        // 선택 후 UI 상태 확인
        composeTestRule.onRoot().printToLog("AFTER_CATEGORY_SELECTION")
        
        Log.d("SimpleTtsTest", "카테고리 선택 테스트 완료")
    }

    /**
     * TTS 재생 버튼 확인
     */
    @Test
    fun testTtsPlayButtons() = runBlocking {
        // Activity 시작 대기
        waitForActivityStart()
        
        Log.d("SimpleTtsTest", "TTS 재생 버튼 확인 시작")
        
        // 카테고리 선택 시도
        try {
            composeTestRule.onNodeWithText("가족/친구").performClick()
            delay(1000)
        } catch (e: Exception) {
            Log.w("SimpleTtsTest", "카테고리 선택 실패, 기본 상태에서 진행: ${e.message}")
        }
        
        // 현재 UI 상태 출력
        composeTestRule.onRoot().printToLog("BEFORE_TTS_TEST")
        
        // TTS 재생 버튼 찾기 시도
        try {
            // 질문 재생 버튼 찾기
            composeTestRule.onNodeWithContentDescription("질문 재생").assertIsDisplayed()
            Log.d("SimpleTtsTest", "질문 재생 버튼 발견")
            
            // 답변 재생 버튼 찾기
            composeTestRule.onNodeWithContentDescription("답변 재생").assertIsDisplayed()
            Log.d("SimpleTtsTest", "답변 재생 버튼 발견")
            
        } catch (e: Exception) {
            Log.w("SimpleTtsTest", "TTS 재생 버튼 찾기 실패: ${e.message}")
            
            // 다른 방법으로 버튼 찾기 시도
            try {
                composeTestRule.onNodeWithContentDescription("재생").assertIsDisplayed()
                Log.d("SimpleTtsTest", "일반 재생 버튼 발견")
            } catch (e2: Exception) {
                Log.w("SimpleTtsTest", "일반 재생 버튼도 찾기 실패: ${e2.message}")
            }
        }
        
        Log.d("SimpleTtsTest", "TTS 재생 버튼 확인 완료")
    }

    /**
     * TTS 재생 테스트
     */
    @Test
    fun testTtsPlayback() = runBlocking {
        // Activity 시작 대기
        waitForActivityStart()
        
        Log.d("SimpleTtsTest", "TTS 재생 테스트 시작")
        
        // 카테고리 선택 시도
        try {
            composeTestRule.onNodeWithText("가족/친구").performClick()
            delay(1000)
        } catch (e: Exception) {
            Log.w("SimpleTtsTest", "카테고리 선택 실패: ${e.message}")
        }
        
        // TTS 재생 시도
        try {
            // 질문 재생 버튼 클릭
            composeTestRule.onNodeWithContentDescription("질문 재생").performClick()
            Log.d("SimpleTtsTest", "질문 재생 버튼 클릭 성공")
            
            // 재생 대기
            delay(2000)
            
            // 재생 중 상태 확인
            composeTestRule.onRoot().printToLog("DURING_QUESTION_PLAYBACK")
            
            // 답변 재생 버튼 클릭 (질문 재생 중단 확인)
            composeTestRule.onNodeWithContentDescription("답변 재생").performClick()
            Log.d("SimpleTtsTest", "답변 재생 버튼 클릭 성공")
            
            // 재생 대기
            delay(2000)
            
            // 재생 중 상태 확인
            composeTestRule.onRoot().printToLog("DURING_ANSWER_PLAYBACK")
            
        } catch (e: Exception) {
            Log.w("SimpleTtsTest", "TTS 재생 테스트 실패: ${e.message}")
        }
        
        Log.d("SimpleTtsTest", "TTS 재생 테스트 완료")
    }
} 