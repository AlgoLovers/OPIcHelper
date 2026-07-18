package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.component.SectionHeader

@Composable
fun MemorizeLevelSelector(
    levels: List<String>,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = levels.indexOf(selectedLevel).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "🎯 암기 레벨")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(levels) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val newIndex = if (dragAmount < -50) {
                                (currentIndex + 1).coerceAtMost(levels.size - 1)
                            } else if (dragAmount > 50) {
                                (currentIndex - 1).coerceAtLeast(0)
                            } else return@detectHorizontalDragGestures
                            if (newIndex != currentIndex) {
                                onLevelSelected(levels[newIndex])
                            }
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 고정 48dp IconButton(아이콘 중앙정렬) + weight(1f) 중앙 텍스트로 좌우 대칭을 보장한다.
                IconButton(
                    onClick = { onLevelSelected(levels[currentIndex - 1]) },
                    enabled = currentIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "이전 레벨",
                        tint = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = selectedLevel.ifEmpty { "레벨을 선택하세요" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onLevelSelected(levels[currentIndex + 1]) },
                    enabled = currentIndex < levels.size - 1
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "다음 레벨",
                        tint = if (currentIndex < levels.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Text(
                text = "좌우 스와이프 또는 화살표로 전환",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
