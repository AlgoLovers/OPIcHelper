package com.na982.opichelper.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
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
    sentenceIndex: Int = 0,
    totalSentences: Int = 0,
    currentRepetition: Int = 0,
    totalRepetitions: Int = 0,
    isRepeatListeningMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (isRepeatListeningMode && totalSentences > 0) {
            RepeatListeningProgressBar(
                sentenceIndex = sentenceIndex,
                totalSentences = totalSentences,
                currentRepetition = currentRepetition,
                totalRepetitions = totalRepetitions,
                hasCompleted = hasCompleted,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!sentenceEn.isNullOrBlank()) {
            Text(
                text = sentenceEn,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (hasCompleted) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isRepeatListeningMode && totalSentences > 0) 2.dp else 0.dp)
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

        if (hasCompleted && !sentenceKo.isNullOrBlank()) {
            Text(
                text = sentenceKo,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RepeatListeningProgressBar(
    sentenceIndex: Int,
    totalSentences: Int,
    currentRepetition: Int,
    totalRepetitions: Int,
    hasCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSentences > 0) {
        ((sentenceIndex + 1).toFloat() / totalSentences.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${sentenceIndex + 1}/$totalSentences",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        )

        if (hasCompleted) {
            Text(
                text = "완료",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        } else if (totalRepetitions > 0) {
            Text(
                text = "$currentRepetition/${totalRepetitions}회",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
