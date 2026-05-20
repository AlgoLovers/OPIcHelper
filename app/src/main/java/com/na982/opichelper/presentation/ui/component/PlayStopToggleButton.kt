package com.na982.opichelper.presentation.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun PlayStopToggleButton(
    isActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    activeLabel: String,
    inactiveLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = { if (isActive) onDeactivate() else onActivate() },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.semantics {
            contentDescription = if (isActive) activeLabel else inactiveLabel
        }
    ) {
        Text(
            text = if (isActive) activeLabel else inactiveLabel,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
