package com.na982.opichelper.presentation.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import android.util.Log

/**
 * FlipCard: 재사용 가능한 플립 카드 컴포저블 (질문/답변 모두 사용 가능)
 * frontContent: 앞면(영문), backContent: 뒷면(한글)
 */
@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 800)
    )
    
    Log.d("FlipCard", "Rendering with flipped=$flipped, rotation=$rotation")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                flipped = !flipped
                Log.d("FlipCard", "Card flipped to: $flipped")
            }
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = 16.dp.value * density
            }
    ) {
        if (rotation <= 90f) {
            Box(Modifier.alpha(1f - (rotation / 90f))) {
                frontContent()
            }
        } else {
            Box(Modifier.graphicsLayer { rotationX = 180f }.alpha((rotation - 90f) / 90f)) {
                backContent()
            }
        }
    }
} 