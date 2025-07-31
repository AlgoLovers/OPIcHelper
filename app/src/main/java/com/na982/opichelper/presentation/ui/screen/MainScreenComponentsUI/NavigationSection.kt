package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationSection(
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("NavigationSection", "Rendering navigation section")
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PreviousQuestionButton(
            onPreviousQuestion = {
                Log.d("NavigationSection", "Previous question button clicked")
                onPreviousQuestion()
                Log.d("NavigationSection", "Moved to previous question")
            },
            modifier = Modifier.weight(1f)
        )
        
        NextQuestionButton(
            onNextQuestion = {
                Log.d("NavigationSection", "Next question button clicked")
                onNextQuestion()
                Log.d("NavigationSection", "Moved to next question")
            },
            modifier = Modifier.weight(1f)
        )
    }
} 