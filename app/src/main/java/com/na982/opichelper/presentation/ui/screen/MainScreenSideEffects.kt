package com.na982.opichelper.presentation.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.presentation.viewmodel.FullMemorizationViewModel
import com.na982.opichelper.presentation.viewmodel.MemorizationController
import com.na982.opichelper.presentation.viewmodel.PlaybackViewModel
import com.na982.opichelper.presentation.viewmodel.RepeatListeningViewModel

@Composable
fun MainScreenSideEffects(
    selectedLevel: String,
    currentQaItem: QaItem?,
    isRepeatListeningPlaying: Boolean,
    fullMemorizationSentenceEn: String?,
    fullMemorizationSentenceKo: String?,
    memorizationController: MemorizationController,
    repeatListeningViewModel: RepeatListeningViewModel,
    playbackViewModel: PlaybackViewModel
) {
    LaunchedEffect(selectedLevel) {
        memorizationController.onLevelChangedAll()
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
