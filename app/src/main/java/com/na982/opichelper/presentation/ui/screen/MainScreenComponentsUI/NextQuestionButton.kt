package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NextQuestionButton(
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onNextQuestion,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = modifier
    ) {
        Text(
            text = "다음 질문",
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
} 