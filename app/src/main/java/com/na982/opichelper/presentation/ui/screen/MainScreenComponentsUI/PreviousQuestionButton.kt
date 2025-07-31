package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PreviousQuestionButton(
    onPreviousQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onPreviousQuestion,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = modifier
    ) {
        Text(
            text = "이전 질문",
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
} 