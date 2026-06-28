package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.MemorizeTestEvent
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.usecase.CoordinatorEvent
import com.na982.opichelper.domain.usecase.CurrentMode
import com.na982.opichelper.domain.usecase.MemorizationModeCoordinator
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.na982.opichelper.domain.manager.AppLogger
import javax.inject.Inject

data class EnglishWritingTestUiState(
    val isCardFlipped: Boolean = false
)

@HiltViewModel
class EnglishWritingTestViewModel @Inject constructor(
    private val englishWritingTestRepository: EnglishWritingTestRepository,
    private val ttsCtrl: TtsPlaybackController,
    private val qaDataManager: QaDataManager,
    private val progress: MemorizeTestProgressTracker,
    coordinator: MemorizationModeCoordinator,
    appLogger: AppLogger
) : BaseMemorizationViewModel<EnglishWritingTestUiState>(
    coordinator = coordinator,
    ttsPlaybackController = ttsCtrl,
    progressTracker = progress,
    appLogger = appLogger
) {

    override val _uiState = MutableStateFlow(EnglishWritingTestUiState())
    override fun resetUiState() = EnglishWritingTestUiState()
    override fun initialMode() = CurrentMode.ENGLISH_WRITING

    override fun onStop() {
        _uiState.update { it.copy(isCardFlipped = false) }
        viewModelScope.launch { coordinator.emitEvent(CoordinatorEvent.EnglishWritingStopped) }
    }

    override suspend fun startMode() {
        try {
            ttsCtrl.stopTts()
            ttsCtrl.clearHighlight()
            startEnglishWritingTest()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            appLogger.e("EnglishWritingTestVM", "영작 테스트 시작 실패", e)
            emitEvent("영작 테스트를 시작할 수 없습니다")
            stop()
        }
    }

    private suspend fun startEnglishWritingTest() {
        val currentItem = qaDataManager.currentQaItem.value
        if (currentItem == null) {
            appLogger.e("EnglishWritingTestVM", "현재 QA 아이템이 없음")
            emitEvent("질문 데이터를 불러올 수 없습니다")
            stop()
            return
        }

        val scriptIndex = qaDataManager.getCurrentIndex()

        val eventJob = viewModelScope.launch {
            englishWritingTestRepository.events.collect { event ->
                handleEvent(event)
            }
        }

        val useCaseJob = viewModelScope.launch {
            try {
                englishWritingTestRepository.executeEnglishWritingTest(
                    answerKo = qaDataManager.getCurrentAnswerKo(currentItem),
                    answerEn = qaDataManager.getCurrentAnswer(currentItem),
                    category = currentItem.category,
                    scriptIndex = scriptIndex
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                appLogger.e("EnglishWritingTestVM", "영작 테스트 실행 중 오류", e)
                emitEvent("영작 테스트 실행 중 오류가 발생했습니다")
                stop()
            }
        }
        coordinator.registerJob(useCaseJob)
        coordinator.registerEventJob(eventJob)
        useCaseJob.join()
    }

    private fun handleEvent(event: MemorizeTestEvent) {
        when (event) {
            is MemorizeTestEvent.CardFlip -> {
                _uiState.update { it.copy(isCardFlipped = event.isKorean) }
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
            is MemorizeTestEvent.RecordingHighlight -> {
                if (event.index != null) {
                    ttsCtrl.setRecordingHighlightIndex(event.index)
                } else {
                    ttsCtrl.clearHighlight()
                }
            }
            is MemorizeTestEvent.RecordingStateChange -> {
                if (!event.isRecording) {
                    viewModelScope.launch { coordinator.emitEvent(CoordinatorEvent.RecordingStateChanged) }
                }
            }
            is MemorizeTestEvent.MergedFileCreated -> {
                viewModelScope.launch { coordinator.emitEvent(CoordinatorEvent.EnglishWritingCompleted) }
                stop()
            }
            else -> {}
        }
    }

    private fun getSentenceFromAnswer(index: Int, isKorean: Boolean): String? {
        val currentItem = qaDataManager.currentQaItem.value ?: return null
        val text = if (isKorean) {
            qaDataManager.getCurrentAnswerKo(currentItem)
        } else {
            qaDataManager.getCurrentAnswer(currentItem)
        }
        return SentenceSplitter.split(text).getOrNull(index)
    }
}
