package com.na982.opichelper.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SentenceRegex = Regex("(?<=[.!?])\\s+")

@Composable
fun HighlightText(
    text: String,
    highlightIndex: Int?,
    recordingHighlightIndex: Int? = null,
    resumeHighlightIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    val sentences = remember(text) {
        text.split(SentenceRegex).map { it.trim() }.filter { it.isNotEmpty() }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        sentences.forEachIndexed { index, sentence ->
            val isHighlighted = highlightIndex == index
            val isRecordingHighlighted = recordingHighlightIndex == index
            val isResumeHighlighted = resumeHighlightIndex == index

            val (backgroundColor, textColor, fontSize, fontWeight) = when {
                isRecordingHighlighted -> {
                    HighlightStyle(
                        bg = MaterialTheme.colorScheme.error,
                        text = MaterialTheme.colorScheme.onError,
                        size = 22.sp,
                        weight = FontWeight.ExtraBold
                    )
                }
                isHighlighted -> {
                    HighlightStyle(
                        bg = MaterialTheme.colorScheme.primary,
                        text = MaterialTheme.colorScheme.onPrimary,
                        size = 22.sp,
                        weight = FontWeight.ExtraBold
                    )
                }
                isResumeHighlighted -> {
                    HighlightStyle(
                        bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        text = MaterialTheme.colorScheme.primary,
                        size = 18.sp,
                        weight = FontWeight.SemiBold
                    )
                }
                else -> {
                    HighlightStyle(
                        bg = Color.Transparent,
                        text = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        size = 16.sp,
                        weight = FontWeight.Normal
                    )
                }
            }

            Text(
                text = sentence,
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor)
                    .padding(vertical = 6.dp, horizontal = 10.dp)
                    .then(
                        if (isHighlighted || isRecordingHighlighted) {
                            Modifier.semantics { stateDescription = "현재 재생 중" }
                        } else Modifier
                    )
            )
        }
    }
}

private data class HighlightStyle(
    val bg: Color,
    val text: Color,
    val size: TextUnit,
    val weight: FontWeight
)
