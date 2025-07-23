package com.na982.opichelper.presentation.ui.screen.MainScreenComponentsUI

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.entity.MainScreenState
import com.na982.opichelper.domain.entity.PlayType

@Composable
fun NavigationSection(
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    screenState: MainScreenState,
    onHighlightReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("NavigationSection", "Rendering navigation section")
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PreviousQuestionButton(
            onPreviousQuestion = {
                Log.d("NavigationSection", "Previous question button clicked")
                // 모든 상태 초기화
                screenState.resetAllPlayStates()
                onHighlightReset()
                // 이전 질문으로 이동
                onPreviousQuestion()
                Log.d("NavigationSection", "Moved to previous question, all states reset")
            },
            modifier = Modifier.weight(1f)
        )
        
        NextQuestionButton(
            onNextQuestion = {
                Log.d("NavigationSection", "Next question button clicked")
                // 모든 상태 초기화
                screenState.resetAllPlayStates()
                onHighlightReset()
                // 다음 질문으로 이동
                onNextQuestion()
                Log.d("NavigationSection", "Moved to next question, all states reset")
            },
            modifier = Modifier.weight(1f)
        )
    }
} 