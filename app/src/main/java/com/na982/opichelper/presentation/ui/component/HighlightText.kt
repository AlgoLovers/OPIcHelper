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
import android.util.Log

@Composable
fun HighlightText(
    text: String,
    highlightIndex: Int?,
    modifier: Modifier = Modifier,
    onHighlightChange: (Int?) -> Unit = {}
) {
    Log.d("HighlightText", "Rendering with highlightIndex=$highlightIndex, text=${text.take(50)}...")
    
    val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    
    Column(modifier = modifier) {
        sentences.forEachIndexed { index, sentence ->
            val isHighlighted = highlightIndex == index
            val backgroundColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
            val textColor = if (isHighlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val fontSize = if (isHighlighted) {
                18.sp
            } else {
                16.sp
            }
            val fontWeight = if (isHighlighted) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
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