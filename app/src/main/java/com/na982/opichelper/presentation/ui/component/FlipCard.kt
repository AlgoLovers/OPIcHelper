package com.na982.opichelper.presentation.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    onCardClick: () -> Unit = {},
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    var flipped by remember { mutableStateOf(isFlipped) }

    LaunchedEffect(isFlipped) {
        flipped = isFlipped
    }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    val cameraDistancePx = with(LocalDensity.current) { 16.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "탭하여 영어/한국어 전환"
                stateDescription = if (flipped) "한국어 표시 중" else "영어 표시 중"
            }
            .clickable {
                flipped = !flipped
                onCardClick()
            }
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = cameraDistancePx * density
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
