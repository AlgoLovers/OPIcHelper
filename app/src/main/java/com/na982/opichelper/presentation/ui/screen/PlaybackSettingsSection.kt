package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.na982.opichelper.presentation.viewmodel.SettingsUiState
import com.na982.opichelper.presentation.viewmodel.SettingsViewModel

@Composable
fun PlaybackSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(modifier = modifier) {
        SettingsSectionHeader(title = "🎧 학습 설정")

        SettingsSectionSpacer()

        Text(
            text = "반복듣기 횟수: ${uiState.repeatListeningCount}회",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            text = "한국어→영어 반복 횟수 (2~50회)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Slider(
            value = uiState.repeatListeningCount.toFloat(),
            onValueChange = { viewModel.setRepeatListeningCount(it.toInt()) },
            valueRange = 2f..50f,
            // 2~50 정수 값이라 눈금(steps)을 두면 47개가 촘촘히 겹쳐 지저분하다.
            // 연속 슬라이더로 두되, value가 정수 상태(repeatListeningCount)에 묶여 있어
            // 드래그 시 각 정수로 스냅된다.
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "답변 재생 횟수: ${uiState.answerPlayCount}회",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            text = "답변 TTS 연속 재생 횟수 (1~10회)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Slider(
            value = uiState.answerPlayCount.toFloat(),
            onValueChange = { viewModel.setAnswerPlayCount(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "영어 TTS 속도: ${"%.1f".format(uiState.englishTtsRate)}x",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Text(
            text = "영어 음성 재생 속도 (0.5x~1.5x)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Slider(
            value = uiState.englishTtsRate,
            onValueChange = { viewModel.setEnglishTtsRate(it) },
            valueRange = 0.5f..1.5f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
