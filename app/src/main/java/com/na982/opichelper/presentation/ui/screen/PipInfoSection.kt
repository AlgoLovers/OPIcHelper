package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PipInfoSection(
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(modifier = modifier) {
        SettingsSectionHeader(title = "📱 화면 겹쳐보기 (PiP)")

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "TTS 재생 중 홈키를 누르면 현재 문장이 작은 창으로 표시됩니다.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PiP가 동작하지 않는 경우:",
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "1. 시스템 설정 → 애플리케이션 → OPIc Helper → 화면 겹쳐보기 허용\n2. Android 12+ 필수 (API 26 이상에서 지원)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 16.sp
        )
    }
}
