package com.na982.opichelper.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingDialog(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "OPIc Helper에 오신 것을 환영합니다!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingLevelItem(
                    emoji = "\uD83D\uDD0A",
                    title = "반복듣기",
                    description = "한국어→영어 순서로 반복하여 듣기"
                )
                OnboardingLevelItem(
                    emoji = "\u270D\uFE0F",
                    title = "부분암기",
                    description = "한국어 듣고 영어로 말하기 연습"
                )
                OnboardingLevelItem(
                    emoji = "\uD83C\uDFA4",
                    title = "통암기",
                    description = "전체 문장을 녹음하고 비교하기"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "시작하기",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun OnboardingLevelItem(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
