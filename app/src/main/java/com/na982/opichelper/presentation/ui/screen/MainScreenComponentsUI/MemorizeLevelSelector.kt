package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "◀",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (currentIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier
                        .clickable(enabled = currentIndex > 0) {
                            onLevelSelected(levels[currentIndex - 1])
                        }
                        .size(48.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                )

                Text(
                    text = selectedLevel.ifEmpty { "레벨을 선택하세요" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "▶",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (currentIndex < levels.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier
                        .clickable(enabled = currentIndex < levels.size - 1) {
                            onLevelSelected(levels[currentIndex + 1])
                        }
                        .size(48.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
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
