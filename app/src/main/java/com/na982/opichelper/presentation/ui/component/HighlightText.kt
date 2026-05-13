package com.na982.opichelper.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HighlightText(
    text: String,
    highlightIndex: Int?,
    recordingHighlightIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    
    Column(modifier = modifier) {
        sentences.forEachIndexed { index, sentence ->
            val isHighlighted = highlightIndex == index
            val isRecordingHighlighted = recordingHighlightIndex == index
            
            // 녹음 하이라이트가 우선순위가 높음
            val backgroundColor = when {
                isRecordingHighlighted -> MaterialTheme.colorScheme.errorContainer
                isHighlighted -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            val textColor = when {
                isRecordingHighlighted -> MaterialTheme.colorScheme.onErrorContainer
                isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            val fontSize = when {
                isRecordingHighlighted -> 20.sp
                isHighlighted -> 20.sp
                else -> 18.sp
            }
            val fontWeight = when {
                isRecordingHighlighted -> FontWeight.Bold
                isHighlighted -> FontWeight.Bold
                else -> FontWeight.Normal
            }
            
            Text(
                text = sentence,
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(vertical = 2.dp, horizontal = 4.dp)
            )
        }
    }
} 