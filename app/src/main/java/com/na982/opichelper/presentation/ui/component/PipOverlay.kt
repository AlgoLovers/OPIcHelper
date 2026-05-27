package com.na982.opichelper.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PipOverlay(
    sentenceEn: String?,
    sentenceKo: String?,
    hasCompleted: Boolean = false,
    hasNextItem: Boolean = false,
    onRepeat: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = if (hasCompleted) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        if (!sentenceEn.isNullOrBlank()) {
            Text(
                text = sentenceEn,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (hasCompleted) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "OPIc Helper",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (hasCompleted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRepeat,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("반복 재생", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                FilledTonalButton(
                    onClick = onNext,
                    enabled = hasNextItem,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("다음", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
