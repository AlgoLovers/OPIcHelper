package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.na982.opichelper.ui.theme.*

@Composable
fun AppTitle(
    currentLevel: String = "",
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            // 설정 버튼 (우측 상단) - 미니멀하고 현대적인 디자인
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(52.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 미니멀한 설정 아이콘 (점 3개)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 첫 번째 점
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            // 두 번째 점
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            // 세 번째 점
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                        
                        // 우측 상단 작은 원형 배지 (선택적)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(6.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OPic Helper",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "영어 말하기 실력 향상을 위한 최고의 도구",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
                
                // 현재 목표 레벨 표시
                if (currentLevel.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "목표: $currentLevel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "🎯 목표 달성",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "📚 체계적 학습",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "🎧 실전 연습",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
} 