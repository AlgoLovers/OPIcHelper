package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
        modifier = modifier.semantics { contentDescription = "이전 질문" }
    ) {
        Text(
            text = "이전 질문",
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}
