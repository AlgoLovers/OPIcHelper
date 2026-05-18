package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationSection(
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PreviousQuestionButton(
            onPreviousQuestion = {
                onPreviousQuestion()
            },
            modifier = Modifier.weight(1f)
        )

        NextQuestionButton(
            onNextQuestion = {
                onNextQuestion()
            },
            modifier = Modifier.weight(1f)
        )
    }
} 