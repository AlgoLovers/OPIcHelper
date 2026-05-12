package com.na982.opichelper.domain.audio

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPlaybackController @Inject constructor() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentPlayJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()

    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()

    private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
    val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex.asStateFlow()

    private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
    val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex.asStateFlow()

    private val _answerKoHighlightIndex = MutableStateFlow<Int?>(null)
    val answerKoHighlightIndex: StateFlow<Int?> = _answerKoHighlightIndex.asStateFlow()

    private val _recordingHighlightIndex = MutableStateFlow<Int?>(null)
    val recordingHighlightIndex: StateFlow<Int?> = _recordingHighlightIndex.asStateFlow()

    private var ttsOrchestrator: TtsOrchestrator? = null

    fun setTtsOrchestrator(orchestrator: TtsOrchestrator) {
        ttsOrchestrator = orchestrator
    }

    fun playQuestion(question: String) {
        forceStopTts()
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                kotlinx.coroutines.delay(150)
                _isPlaying.value = true
                _isQuestionPlaying.value = true

                ttsOrchestrator?.speakWithHighlight(question) { index ->
                    _questionHighlightIndex.value = index
                }
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "질문 TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isPlaying.value = false
                    _isQuestionPlaying.value = false
                    _questionHighlightIndex.value = null
                }
            }
        }
    }

    fun playAnswer(answer: String) {
        forceStopTts()
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                kotlinx.coroutines.delay(150)
                _isPlaying.value = true
                _isAnswerPlaying.value = true

                ttsOrchestrator?.speakWithHighlight(answer) { index ->
                    _answerHighlightIndex.value = index
                }
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "답변 TTS 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isPlaying.value = false
                    _isAnswerPlaying.value = false
                    _answerHighlightIndex.value = null
                }
            }
        }
    }

    fun playMergedAudio(question: String, answer: String) {
        forceStopTts()
        currentPlayJob = coroutineScope.launch {
            val myJob = this.coroutineContext[Job]
            try {
                kotlinx.coroutines.delay(150)
                _isPlaying.value = true

                _isQuestionPlaying.value = true
                ttsOrchestrator?.speakWithHighlight(question) { index ->
                    _questionHighlightIndex.value = index
                }
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null

                kotlinx.coroutines.delay(500)

                _isAnswerPlaying.value = true
                ttsOrchestrator?.speakWithHighlight(answer) { index ->
                    _answerHighlightIndex.value = index
                }
                _isAnswerPlaying.value = false
                _answerHighlightIndex.value = null
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "합쳐진 오디오 재생 오류", e)
            } finally {
                if (currentPlayJob == myJob) {
                    _isPlaying.value = false
                    _isQuestionPlaying.value = false
                    _isAnswerPlaying.value = false
                    _questionHighlightIndex.value = null
                    _answerHighlightIndex.value = null
                }
            }
        }
    }

    fun stopTts() {
        stopTtsSync()
    }

    private fun stopTtsSync() {
        try {
            ttsOrchestrator?.stop()
            currentPlayJob?.cancel()
            currentPlayJob = null
            resetPlayState()
            clearHighlight()
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 재생 중지 실패", e)
        }
    }

    private fun resetPlayState() {
        _isPlaying.value = false
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = false
        _questionHighlightIndex.value = null
        _answerHighlightIndex.value = null
        _answerKoHighlightIndex.value = null
        _recordingHighlightIndex.value = null
    }

    fun pauseTts() {
        try {
            if (_isPlaying.value) {
                ttsOrchestrator?.pause()
            }
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 일시 중지 실패", e)
        }
    }

    fun resumeTts() {
        try {
            if (_isPlaying.value) {
                ttsOrchestrator?.resume()
            }
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 재개 실패", e)
        }
    }

    fun stopAllTts() {
        stopTtsSync()
    }

    fun cleanupTts() {
        try {
            stopAllTts()
            ttsOrchestrator?.releaseAllPlayers()
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 완전 정리 실패", e)
        }
    }

    fun forceStopTts() {
        try {
            ttsOrchestrator?.stop()
            currentPlayJob?.cancel()
            currentPlayJob = null
            resetPlayState()
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 강제 중지 오류", e)
        }
    }

    fun getCurrentKoreanTtsServiceName(): String {
        return ttsOrchestrator?.getCurrentKoreanTtsServiceName() ?: "없음"
    }

    fun setQuestionHighlightIndex(index: Int) {
        _questionHighlightIndex.value = index
    }

    fun setAnswerHighlightIndex(index: Int) {
        _answerHighlightIndex.value = index
    }

    fun setAnswerKoHighlightIndex(index: Int) {
        _answerKoHighlightIndex.value = index
    }

    fun setRecordingHighlightIndex(index: Int) {
        _recordingHighlightIndex.value = index
    }

    fun clearHighlight() {
        _questionHighlightIndex.value = null
        _answerHighlightIndex.value = null
        _answerKoHighlightIndex.value = null
        _recordingHighlightIndex.value = null
    }
}
