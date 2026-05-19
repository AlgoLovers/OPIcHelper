package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import com.na982.opichelper.presentation.ui.component.FlipCard
import com.na982.opichelper.presentation.ui.component.HighlightText
import com.na982.opichelper.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

@Composable
fun AnswerCard(
    currentAnswer: String,
    currentAnswerKo: String,
    highlightIndex: Int?,
    answerKoHighlightIndex: Int? = null,
    recordingHighlightIndex: Int? = null,
    resumeHighlightIndex: Int? = null,
    isFlipped: Boolean = false,
    isRepeatListeningCardFlipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 플립 카드 (숨김 상태에 따라 크기 조절)
        FlipCard(
            isFlipped = isFlipped || isRepeatListeningCardFlipped,
            onCardClick = {
                // 카드 클릭 시 뒤집기만 동작, 숨기기 기능은 버튼 클릭 시에만 동작
            },
            frontContent = {
                ModernAnswerCard(
                    title = "Answer",
                    content = currentAnswer,
                    highlightIndex = highlightIndex,
                    recordingHighlightIndex = recordingHighlightIndex,
                    resumeHighlightIndex = resumeHighlightIndex,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    titleColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isVisible = isVisible,
                    onHideClick = {
                        isVisible = !isVisible
                    }
                )
            },
            backContent = {
                ModernAnswerCard(
                    title = "답변",
                    content = currentAnswerKo,
                    highlightIndex = answerKoHighlightIndex,
                    recordingHighlightIndex = recordingHighlightIndex,
                    resumeHighlightIndex = resumeHighlightIndex,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    isVisible = isVisible,
                    onHideClick = {
                        isVisible = !isVisible
                    }
                )
            }
        )
    }
}

@Composable
private fun ModernAnswerCard(
    title: String,
    content: String,
    highlightIndex: Int?,
    recordingHighlightIndex: Int?,
    resumeHighlightIndex: Int?,
    backgroundColor: Color,
    titleColor: Color,
    @Suppress("UNUSED_PARAMETER") contentColor: Color,
    isVisible: Boolean = true,
    onHideClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 제목 섹션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = titleColor.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = titleColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                // 숨기기/보이기 버튼 (탭하여 뒤집기 왼쪽)
                FilledTonalButton(
                    onClick = onHideClick,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isVisible) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        contentColor = if (isVisible) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isVisible) "숨기기" else "보이기",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 플립 힌트
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(
                        text = "👆 탭하여 뒤집기",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }

            // 내용 섹션 (숨김 상태에 따라 조건부 표시)
            if (isVisible) {
                Spacer(modifier = Modifier.height(16.dp))

                HighlightText(
                    text = content,
                    highlightIndex = highlightIndex,
                    recordingHighlightIndex = recordingHighlightIndex,
                    resumeHighlightIndex = resumeHighlightIndex,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
