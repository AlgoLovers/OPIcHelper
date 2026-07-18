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

@Composable
fun AnswerCard(
    currentAnswer: String,
    currentAnswerKo: String,
    highlightIndex: Int?,
    answerKoHighlightIndex: Int? = null,
    recordingHighlightIndex: Int? = null,
    resumeHighlightIndex: Int? = null,
    isFlipped: Boolean = false,
    isModified: Boolean = false,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (onEdit != null || isModified) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isModified) {
                    ModifiedBadge()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (onEdit != null) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "편집",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        // 플립 카드 (숨김 상태에 따라 크기 조절)
        FlipCard(
            isFlipped = isFlipped,
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

                FilledTonalButton(
                    onClick = onHideClick,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isVisible) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        contentColor = if (isVisible) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isVisible) "숨기기" else "보이기",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
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
