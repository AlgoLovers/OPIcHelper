package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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