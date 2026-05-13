package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecordingAnimation(
    @Suppress("UNUSED_PARAMETER") isRecording: Boolean,
    @Suppress("UNUSED_PARAMETER") onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 녹음 중 애니메이션 (빨간 원) - 크기 3배 증가, 가운데 정렬
            Box(
                modifier = Modifier
                    .size(180.dp) // 60dp * 3 = 180dp
                    .background(
                        color = Color.Red,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "●",
                    color = Color.White,
                    fontSize = 72.sp // 24sp * 3 = 72sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 녹음 중 텍스트
            Text(
                text = "녹음 중...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            // 녹음 종료 버튼 삭제
        }
    }
} 