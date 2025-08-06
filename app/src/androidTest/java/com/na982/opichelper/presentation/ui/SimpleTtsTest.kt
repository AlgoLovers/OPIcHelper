package com.na982.opichelper.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.na982.opichelper.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SimpleTtsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testBasicTtsFunctionality() {
        // 기본 TTS 기능 테스트
        composeTestRule.onNodeWithText("질문").performClick()
        
        // 잠시 대기
        Thread.sleep(2000)
        
        // 답변 재생 테스트
        composeTestRule.onNodeWithText("답변").performClick()
        
        // 잠시 대기
        Thread.sleep(2000)
    }
} 