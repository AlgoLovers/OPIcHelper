package com.na982.opichelper.presentation.ui.component

import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.na982.opichelper.domain.entity.ButtonConfig


/**
 * 스마트 버튼 컴포넌트
 * 버튼의 상태와 기능을 동적으로 관리
 */
@Composable
fun SmartButton(
    buttonConfig: ButtonConfig,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("SmartButton", "Rendering SmartButton: ${buttonConfig.function}, state: ${buttonConfig.state}, text: ${buttonConfig.text}")
    
    // 버튼이 보이지 않으면 렌더링하지 않음
    if (!buttonConfig.isVisible) {
        Log.d("SmartButton", "Button is not visible, skipping render")
        return
    }
    
    Button(
        onClick = {
            Log.d("SmartButton", "SmartButton clicked: ${buttonConfig.function}")
            when (buttonConfig.state) {
                is com.na982.opichelper.domain.entity.ButtonState.Playing -> {
                    onStopClick()
                }
                else -> {
                    onPlayClick()
                }
            }
        },
        enabled = buttonConfig.isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (buttonConfig.state) {
                is com.na982.opichelper.domain.entity.ButtonState.Playing -> {
                    val errorColor = MaterialTheme.colorScheme.error
                    Log.d("SmartButton", "Playing 상태 - 버튼: ${buttonConfig.function}, 색상: $errorColor")
                    errorColor
                }
                is com.na982.opichelper.domain.entity.ButtonState.Loading -> MaterialTheme.colorScheme.secondary
                is com.na982.opichelper.domain.entity.ButtonState.Error -> MaterialTheme.colorScheme.error
                else -> {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Log.d("SmartButton", "Idle 상태 - 버튼: ${buttonConfig.function}, 색상: $primaryColor")
                    primaryColor
                }
            }
        ),
        modifier = modifier
    ) {
        Text(
            text = buttonConfig.text,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * 질문 재생 스마트 버튼
 */
@Composable
fun QuestionPlaySmartButton(
    buttonConfig: ButtonConfig,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmartButton(
        buttonConfig = buttonConfig,
        onPlayClick = onPlayClick,
        onStopClick = onStopClick,
        modifier = modifier
    )
}

/**
 * 답변 재생 스마트 버튼
 */
@Composable
fun AnswerPlaySmartButton(
    buttonConfig: ButtonConfig,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmartButton(
        buttonConfig = buttonConfig,
        onPlayClick = onPlayClick,
        onStopClick = onStopClick,
        modifier = modifier
    )
}

/**
 * 암기 테스트 스마트 버튼
 */
@Composable
fun MemorizeTestSmartButton(
    buttonConfig: ButtonConfig,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmartButton(
        buttonConfig = buttonConfig,
        onPlayClick = onPlayClick,
        onStopClick = onStopClick,
        modifier = modifier
    )
}

/**
 * 녹음 재생 스마트 버튼
 */
@Composable
fun RecordingPlaySmartButton(
    buttonConfig: ButtonConfig,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmartButton(
        buttonConfig = buttonConfig,
        onPlayClick = onPlayClick,
        onStopClick = onStopClick,
        modifier = modifier
    )
} 