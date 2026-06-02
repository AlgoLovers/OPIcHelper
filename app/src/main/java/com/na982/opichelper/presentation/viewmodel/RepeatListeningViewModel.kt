package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.PlaybackPreferences
import com.na982.opichelper.domain.usecase.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.entity.RepeatListeningData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.na982.opichelper.domain.manager.AppLogger
import javax.inject.Inject

data class RepeatListeningUiState(
    val isCardFlipped: Boolean = false,
    val isPlaying: Boolean = false,
    val resumeSentenceIndex: Int? = null
)

@HiltViewModel
class RepeatListeningViewModel @Inject constructor(
    private val repeatListeningRepository: RepeatListeningRepository,
    private val ttsCtrl: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val progress: MemorizeTestProgressTracker,
    private val playbackPreferences: PlaybackPreferences,
    coordinator: MemorizationModeCoordinator,
    appLogger: AppLogger
) : BaseMemorizationViewModel<RepeatListeningUiState>(
    coordinator = coordinator,
    ttsPlaybackController = ttsCtrl,
    progressTracker = progress,
    appLogger = appLogger
) {

    val modeCoordinator: MemorizationModeCoordinator get() = coordinator

    override val _uiState = MutableStateFlow(RepeatListeningUiState())
    override fun resetUiState() = RepeatListeningUiState()
    override fun initialMode() = CurrentMode.REPEAT_LISTENING

    override fun onStop() {
        _uiState.update { it.copy(isCardFlipped = false, isPlaying = false) }
        refreshResumeIndex()
    }

    override suspend fun startMode() {
        try {
            _uiState.update { it.copy(isPlaying = true, resumeSentenceIndex = null) }
            ttsCtrl.stopTts()
            ttsCtrl.clearHighlight()
            startRepeatListening()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            appLogger.e("RepeatListeningVM", "반복 듣기 시작 실패", e)
            emitEvent("반복듣기를 시작할 수 없습니다")
            stop()
        }
    }

    fun refreshResumeIndex() {
        viewModelScope.launch {
            val currentItem = qaDataManager.getCurrentQaItem()
            if (currentItem != null && !_uiState.value.isPlaying) {
                val answerText = qaDataManager.getCurrentAnswer(currentItem)
                val totalCount = SentenceSplitter.split(answerText).size
                if (totalCount > 0) {
                    val resumeIndex = repeatListeningRepository.getResumeIndex(
                        currentItem.category, qaDataManager.getCurrentIndex(), totalCount
                    )
                    _uiState.update { it.copy(resumeSentenceIndex = resumeIndex) }
                } else {
                    _uiState.update { it.copy(resumeSentenceIndex = null) }
                }
            } else {
                _uiState.update { it.copy(resumeSentenceIndex = null) }
            }
        }
    }

    private suspend fun startRepeatListening() {
        val currentItem = qaDataManager.getCurrentQaItem()
        if (currentItem != null) {
            val eventJob = viewModelScope.launch {
                repeatListeningRepository.events.collect { event ->
                    handleEvent(event)
                }
            }
            val useCaseJob = viewModelScope.launch {
                val repeatListeningData = RepeatListeningData(
                    category = currentItem.category,
                    scriptIndex = qaDataManager.getCurrentIndex(),
                    koreanAnswer = qaDataManager.getCurrentAnswerKo(currentItem),
                    englishAnswer = qaDataManager.getCurrentAnswer(currentItem)
                )
                repeatListeningRepository.executeRepeatListening(
                    data = repeatListeningData,
                    repeatCount = playbackPreferences.getRepeatListeningCount()
                )
            }
            coordinator.registerJob(useCaseJob)
            coordinator.registerEventJob(eventJob)
            useCaseJob.join()
        }
    }

    private fun handleEvent(event: MemorizeTestEvent) {
        when (event) {
            is MemorizeTestEvent.CardFlip -> {
                _uiState.update { it.copy(isCardFlipped = event.isKorean) }
            }
            is MemorizeTestEvent.Highlight -> {
                if (event.index != null) {
                    val sentence = getSentenceFromAnswer(event.index, isKorean = false)
                    ttsCtrl.setAnswerHighlightIndex(event.index, sentence)
                } else {
                    ttsCtrl.clearHighlight()
                }
            }
            is MemorizeTestEvent.KoreanHighlight -> {
                if (event.index != null) {
                    val koSentence = getSentenceFromAnswer(event.index, isKorean = true)
                    val enSentence = getSentenceFromAnswer(event.index, isKorean = false)
                    ttsCtrl.setAnswerKoHighlightIndex(event.index, koSentence)
                    ttsCtrl.setAnswerHighlightIndex(event.index, enSentence)
                } else {
                    ttsCtrl.clearHighlight()
                }
            }
            is MemorizeTestEvent.Completed -> {
                if (playbackPreferences.isAutoAdvance()) {
                    handleAutoAdvance()
                } else {
                    stop()
                }
            }
            else -> {}
        }
    }

    private fun handleAutoAdvance() {
        viewModelScope.launch {
            coordinator.cancelEventJob()
            val hasMore = qaDataManager.hasNextQaItem()
            if (hasMore) {
                qaDataManager.nextQaItem()
                _uiState.update { it.copy(isCardFlipped = false) }
                coordinator.updateMode(CurrentMode.REPEAT_LISTENING)
                ttsCtrl.stopTts()
                ttsCtrl.clearHighlight()
                startRepeatListening()
            } else {
                stop()
            }
        }
    }

    private fun getSentenceFromAnswer(index: Int, isKorean: Boolean): String? {
        val currentItem = qaDataManager.getCurrentQaItem() ?: return null
        val text = if (isKorean) {
            qaDataManager.getCurrentAnswerKo(currentItem)
        } else {
            qaDataManager.getCurrentAnswer(currentItem)
        }
        return SentenceSplitter.split(text).getOrNull(index)
    }
}
