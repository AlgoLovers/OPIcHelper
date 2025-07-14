package com.na982.opichelper.presentation.ui.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.material3.Text

@RunWith(AndroidJUnit4::class)
class FlipCardInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun flipCard_flips_on_click_and_shows_back_content() {
        composeTestRule.setContent {
            FlipCard(
                frontContent = { Text("Front") },
                backContent = { Text("Back") }
            )
        }
        // 앞면이 보임
        composeTestRule.onNodeWithText("Front").assertIsDisplayed()
        // 클릭하여 뒤집기
        composeTestRule.onNodeWithText("Front").performClick()
        // 뒷면이 보임
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }
} 