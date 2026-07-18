package com.na982.opichelper.presentation.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationController
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningViewModel
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun MainScreenSideEffects(
    levelChanged: SharedFlow<Unit>,
    currentQaItem: QaItem?,
    isRepeatListeningPlaying: Boolean,
    fullMemorizationSentenceEn: String?,
    fullMemorizationSentenceKo: String?,
    memorizationController: MemorizationController,
    repeatListeningViewModel: RepeatListeningViewModel,
    playbackViewModel: PlaybackViewModel
) {
    // 실제 레벨 변경 이벤트에만 반응한다. 값을 key로 관찰하던 이전 방식은 화면
    // 재진입(PiP 복귀, 설정/통계 왕복)마다 재실행되어 재생을 끊었다.
    LaunchedEffect(Unit) {
        levelChanged.collect {
            memorizationController.onLevelChangedAll()
        }
    }

    LaunchedEffect(currentQaItem) {
        if (!isRepeatListeningPlaying) {
            repeatListeningViewModel.refreshResumeIndex()
        }
    }

    LaunchedEffect(Unit) {
        repeatListeningViewModel.refreshResumeIndex()
    }

    LaunchedEffect(fullMemorizationSentenceEn, fullMemorizationSentenceKo) {
        playbackViewModel.pipStateAggregator.setFullMemorizationSentence(
            fullMemorizationSentenceEn,
            fullMemorizationSentenceKo
        )
    }
}
