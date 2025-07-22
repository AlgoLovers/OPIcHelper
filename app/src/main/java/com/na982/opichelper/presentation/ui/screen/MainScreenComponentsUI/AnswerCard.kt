package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.ui.component.FlipCard
import com.na982.opichelper.presentation.ui.component.HighlightText
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

@Composable
fun AnswerCard(
    currentAnswer: String,
    currentAnswerKo: String,
    highlightIndex: Int?,
    modifier: Modifier = Modifier
) {
    Log.d("AnswerCard", "Rendering with highlightIndex=$highlightIndex")
    
    var isVisible by remember { mutableStateOf(true) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 숨기기 버튼과 제목
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "답변",
                    style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
                    )
            
            TextButton(
                onClick = {
                    isVisible = !isVisible
                    Log.d("AnswerCard", "Visibility toggled to: $isVisible")
                }
                ) {
                    Text(
                    text = if (isVisible) "숨기기" else "보이기",
                    color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
        // 플립 카드로 답변 표시 (숨김 상태에 따라 크기 조절)
        AnimatedVisibility(
            visible = isVisible,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            FlipCard(
                frontContent = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "영문",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightText(
                                text = currentAnswer,
                                highlightIndex = highlightIndex,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                backContent = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "한글",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                                text = currentAnswerKo,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
                    }
                }
            )
        }
    }
} 