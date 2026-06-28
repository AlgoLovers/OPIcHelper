package com.na982.opichelper.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.na982.opichelper.presentation.viewmodel.SettingsUiState

@Composable
fun AppInfoSection(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(modifier = modifier) {
        SettingsSectionHeader(title = "ℹ️ 앱 정보")

        SettingsSectionSpacer()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("버전", fontWeight = FontWeight.Medium)
            val context = LocalContext.current
            val versionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0)?.versionName ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
            }
            Text(
                versionName,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("한글 TTS", fontWeight = FontWeight.Medium)
            Text(
                uiState.currentKoreanTtsService.ifEmpty { "확인 중" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
