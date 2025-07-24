package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log

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