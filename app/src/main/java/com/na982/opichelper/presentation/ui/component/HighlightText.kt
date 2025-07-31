package com.na982.opichelper.presentation.ui.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HighlightText(
    text: String,
    highlightIndex: Int,
    recordingHighlightIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    Log.d("HighlightText", "Rendering with highlightIndex=$highlightIndex, recordingHighlightIndex=$recordingHighlightIndex, text=${text.take(50)}...")
    
    val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    
    Column(modifier = modifier) {
        sentences.forEachIndexed { index, sentence ->
            val isHighlighted = highlightIndex == index
            val isRecordingHighlighted = recordingHighlightIndex == index
            
            Log.d("HighlightText", "문장 $index: isHighlighted=$isHighlighted, isRecordingHighlighted=$isRecordingHighlighted")
            
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