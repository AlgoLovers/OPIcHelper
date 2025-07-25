package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.usecase.FullMemorizationService
import com.na982.opichelper.domain.usecase.RepeatListeningService
import com.na982.opichelper.domain.usecase.EnglishWritingTestService
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.audio.TtsPlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.delay

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val qaDataManager: QaDataManager,
    private val ttsPlaybackController: TtsPlaybackController,
    private val progressTracker: MemorizeTestProgressTracker,
    private val fullMemorizationService: FullMemorizationService,
    private val repeatListeningService: RepeatListeningService,
    private val englishWritingTestService: EnglishWritingTestService
) : ViewModel() {
    // 상태 StateFlow들
    private val _memorizeLevels = MutableStateFlow(listOf("반복 듣기", "영작 테스트", "통암기"))
    val memorizeLevels: StateFlow<List<String>> = _memorizeLevels.asStateFlow()

    // selectedLevel은 MainViewModel의 selectedMemorizeLevel을 사용하므로 제거

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isFullMemorizationMode = MutableStateFlow(false)
    val isFullMemorizationMode: StateFlow<Boolean> = _isFullMemorizationMode.asStateFlow()

    private val _isEnglishWritingTestMode = MutableStateFlow(false)
    val isEnglishWritingTestMode: StateFlow<Boolean> = _isEnglishWritingTestMode.asStateFlow()

    // 영작테스트 카드 뒤집기 상태
    private val _isEnglishWritingTestCardFlipped = MutableStateFlow(false)
    val isEnglishWritingTestCardFlipped: StateFlow<Boolean> = _isEnglishWritingTestCardFlipped.asStateFlow()

    private val _fullMemorizationHighlightIndex = MutableStateFlow<Int?>(null)
    val fullMemorizationHighlightIndex: StateFlow<Int?> = _fullMemorizationHighlightIndex.asStateFlow()

    private val _isFullMemorizationRecording = MutableStateFlow(false)
    val isFullMemorizationRecording: StateFlow<Boolean> = _isFullMemorizationRecording.asStateFlow()

    private val _isFullMemorizationPlaying = MutableStateFlow(false)
    val isFullMemorizationPlaying: StateFlow<Boolean> = _isFullMemorizationPlaying.asStateFlow()

    private val _hasFullMemorizationRecording = MutableStateFlow(false)
    val hasFullMemorizationRecording: StateFlow<Boolean> = _hasFullMemorizationRecording.asStateFlow()

    private val _englishWritingTestCompleted = MutableStateFlow(false)
    val englishWritingTestCompleted: StateFlow<Boolean> = _englishWritingTestCompleted.asStateFlow()

    private var currentUseCaseJob: Job? = null

    // setMemorizeLevel은 MainViewModel의 setSelectedMemorizeLevel을 사용하므로 제거

    fun onMemorizeTestButtonClick(selectedLevel: String) {
        viewModelScope.launch {
            try {
                if (_isRunning.value) {
                    Log.d("MemorizationViewModel", "암기 테스트 종료")
                    currentUseCaseJob?.cancel()
                    currentUseCaseJob = null
                    _isRunning.value = false
                    _isFullMemorizationMode.value = false
                    _fullMemorizationHighlightIndex.value = null
                    _isFullMemorizationRecording.value = false
                    _isFullMemorizationPlaying.value = false
                    _isEnglishWritingTestMode.value = false
                    _isEnglishWritingTestCardFlipped.value = false // 영작테스트 카드 상태 초기화
                    
                    // 반복듣기 종료 시 현재 진행상황 저장
                    if (selectedLevel == "반복 듣기") {
                        try {
                            val currentItem = qaDataManager.getCurrentQaItem()
                            if (currentItem != null) {
                                val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex())
                                if (currentProgress != null && currentProgress.memorizeLevel == "반복 듣기") {
                                    progressTracker.persistChangedProgress()
                                    Log.d("MemorizationViewModel", "반복듣기 종료 시 진행상황 저장: ${currentProgress.currentSentenceIndex}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MemorizationViewModel", "진행상황 저장 실패", e)
                        }
                    }
                    
                    ttsPlaybackController.stopTts()
                    ttsPlaybackController.clearHighlight()
                    return@launch
                }
                Log.d("MemorizationViewModel", "암기 테스트 시작: $selectedLevel")
                when (selectedLevel) {
                    "반복 듣기" -> {
                        // MainViewModel의 stopAllTts를 호출하여 TTS 중지 및 하이라이트 제거
                        // MainViewModel에 대한 참조가 필요하므로 이벤트 기반으로 처리
                        ttsPlaybackController.stopTts()
                        ttsPlaybackController.clearHighlight()
                        startRepeatListening()
                    }
                    "영작 테스트" -> {
                        _isEnglishWritingTestMode.value = true
                        _isEnglishWritingTestCardFlipped.value = false // 초기 상태는 영문
                        ttsPlaybackController.stopTts()
                        ttsPlaybackController.clearHighlight()
                        // 영작테스트 녹음 파일 재생 중단 이벤트 발생
                        _stopEnglishWritingTestMergedFilePlaying.value = true
                        startEnglishWritingTest()
                    }
                    "통암기" -> {
                        _isFullMemorizationMode.value = true
                        ttsPlaybackController.stopTts()
                        ttsPlaybackController.clearHighlight()
                        startFullMemorizationMode()
                    }
                    else -> Log.w("MemorizationViewModel", "알 수 없는 암기 레벨: $selectedLevel")
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기 테스트 시작 실패", e)
                _isRunning.value = false
            }
        }
    }

    fun startFullMemorizationMode() {
        viewModelScope.launch {
            try {
                currentUseCaseJob?.cancel()
                _isFullMemorizationMode.value = true
                _isRunning.value = true
                ttsPlaybackController.stopTts()
                currentUseCaseJob = viewModelScope.launch {
                    val category = qaDataManager.getCurrentCategory() ?: ""
                    val scriptIndex = qaDataManager.getCurrentIndex()
                    fullMemorizationService.startFullMemorization(
                        category = category,
                        scriptIndex = scriptIndex,
                        onRecordingStateChange = { isRecording ->
                            _isFullMemorizationRecording.value = isRecording
                            Log.d("MemorizationViewModel", "통암기 녹음 상태 변경: $isRecording")
                            if (!isRecording) {
                                viewModelScope.launch {
                                    updateFullMemorizationRecordingStatus()
                                }
                            }
                        },
                        onPlayingStateChange = { isPlaying ->
                            _isFullMemorizationPlaying.value = isPlaying
                            Log.d("MemorizationViewModel", "통암기 재생 상태 변경: $isPlaying")
                        },
                        onHighlight = { index ->
                            _fullMemorizationHighlightIndex.value = index
                        }
                    )
                }
                Log.d("MemorizationViewModel", "통암기 모드 시작")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 모드 시작 실패", e)
                _isFullMemorizationMode.value = false
                _isRunning.value = false
            }
        }
    }

    fun stopFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationService.stopRecording()
                _isFullMemorizationRecording.value = false
                _isRunning.value = false
                _fullMemorizationHighlightIndex.value = null
                Log.d("MemorizationViewModel", "통암기 녹음 종료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 종료 실패", e)
            }
        }
    }

    fun playFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationService.playRecording(
                    onPlayingStateChange = { isPlaying ->
                        _isFullMemorizationPlaying.value = isPlaying
                    },
                    onHighlight = { index ->
                        _fullMemorizationHighlightIndex.value = index
                    }
                )
                Log.d("MemorizationViewModel", "통암기 녹음 재생")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 재생 실패", e)
                _fullMemorizationHighlightIndex.value = null
            }
        }
    }

    fun stopFullMemorizationPlaying() {
        viewModelScope.launch {
            try {
                fullMemorizationService.stopPlaying()
                _fullMemorizationHighlightIndex.value = null
                Log.d("MemorizationViewModel", "통암기 재생 중지")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 재생 중지 실패", e)
            }
        }
    }

    suspend fun hasFullMemorizationRecording(): Boolean {
        return fullMemorizationService.hasRecordingFile()
    }

    private suspend fun updateFullMemorizationRecordingStatus() {
        val hasRecording = fullMemorizationService.hasRecordingFile()
        _hasFullMemorizationRecording.value = hasRecording
        Log.d("MemorizationViewModel", "통암기 녹음 파일 존재 여부 업데이트: $hasRecording")
    }

    fun deleteFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                fullMemorizationService.deleteRecordingFile()
                Log.d("MemorizationViewModel", "통암기 녹음 파일 삭제")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 파일 삭제 실패", e)
            }
        }
    }

    private fun startRepeatListening() {
        viewModelScope.launch {
            try {
                _isRunning.value = true
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    Log.d("MemorizationViewModel", "반복 듣기 Service 실행")
                    currentUseCaseJob = launch {
                        repeatListeningService.executeRepeatListeningTest(
                            answerKo = currentItem.answerKo,
                            answerEn = currentItem.answerEn,
                            onHighlight = { index ->
                                if (index != null) {
                                    ttsPlaybackController.setAnswerHighlightIndex(index)
                                    Log.d("MemorizationViewModel", "반복 듣기: 하이라이트 설정: $index")
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MemorizationViewModel", "반복 듣기: 하이라이트 제거")
                                }
                            },
                            onCardFlip = { isKorean ->
                                Log.d("MemorizationViewModel", "반복 듣기: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
                            },
                            category = currentItem.category,
                            scriptIndex = qaDataManager.getCurrentIndex()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "반복 듣기 시작 실패", e)
                _isRunning.value = false
            }
        }
    }

    private fun startEnglishWritingTest() {
        val currentItem = qaDataManager.currentQaItem.value
        if (currentItem != null) {
            val scriptIndex = qaDataManager.getCurrentIndex()
            
            currentUseCaseJob = viewModelScope.launch {
                try {
                    englishWritingTestService.executeEnglishWritingTest(
                        answerKo = currentItem.answerKo,
                        answerEn = currentItem.answerEn,
                        category = currentItem.category,
                        scriptIndex = scriptIndex,
                        onCardFlip = { isKorean ->
                            // 영작테스트 카드 뒤집기 상태 업데이트
                            _isEnglishWritingTestCardFlipped.value = isKorean
                            Log.d("MemorizationViewModel", "영작 테스트: 카드 뒤집기 - ${if (isKorean) "한글" else "영문"}")
                        },
                        onKoreanHighlight = { index ->
                            if (index != null) {
                                ttsPlaybackController.setAnswerKoHighlightIndex(index)
                            } else {
                                ttsPlaybackController.clearHighlight()
                            }
                        },
                        onRecordingHighlight = { index ->
                            if (index != null) {
                                ttsPlaybackController.setRecordingHighlightIndex(index)
                            } else {
                                ttsPlaybackController.clearHighlight()
                            }
                        },
                        onRecordingStateChange = { isRecording ->
                            _isFullMemorizationRecording.value = isRecording
                        },
                        onMergedFileCreated = {
                            viewModelScope.launch {
                                delay(500L) // 파일 시스템 동기화 대기
                                _englishWritingTestCompleted.value = true
                                Log.d("MemorizationViewModel", "영작테스트 병합 파일 생성 완료 - 이벤트 발생")
                                
                                // 녹음 시간 데이터 확인
                                // mainViewModel.checkRecordingTimesAfterEnglishWritingTest() // Removed MainViewModel dependency
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MemorizationViewModel", "영작 테스트 실행 중 오류", e)
                } finally {
                    _isRunning.value = false
                    _isEnglishWritingTestMode.value = false
                    _isEnglishWritingTestCardFlipped.value = false
                }
            }
        }
    }

    fun stopMemorization() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        _isRunning.value = false
        
        // 반복듣기 중단 시 현재 진행상황 저장
        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    // 현재 진행상황을 가져와서 저장
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex())
                    if (currentProgress != null && currentProgress.memorizeLevel == "반복 듣기") {
                        // 현재 진행상황을 그대로 유지 (이미 updateCurrentSentenceIndex에서 업데이트됨)
                        progressTracker.persistChangedProgress()
                        Log.d("MemorizationViewModel", "반복듣기 중단 시 진행상황 저장: ${currentProgress.currentSentenceIndex}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "진행상황 저장 실패", e)
            }
            
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }
    }

    fun stopRepeatListening() {
        currentUseCaseJob?.cancel()
        currentUseCaseJob = null
        _isRunning.value = false
        
        // 반복듣기 중단 시 현재 진행상황 저장
        viewModelScope.launch {
            try {
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    // 현재 진행상황을 가져와서 저장
                    val currentProgress = progressTracker.getScriptProgress(currentItem.category, qaDataManager.getCurrentIndex())
                    if (currentProgress != null && currentProgress.memorizeLevel == "반복 듣기") {
                        // 현재 진행상황을 그대로 유지 (이미 updateCurrentSentenceIndex에서 업데이트됨)
                        progressTracker.persistChangedProgress()
                        Log.d("MemorizationViewModel", "반복듣기 중단 시 진행상황 저장: ${currentProgress.currentSentenceIndex}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "진행상황 저장 실패", e)
            }
            
            ttsPlaybackController.stopTts()
            ttsPlaybackController.clearHighlight()
        }
    }

    fun resetEnglishWritingTestCompleted() {
        _englishWritingTestCompleted.value = false
    }

    private val _stopEnglishWritingTestMergedFilePlaying = MutableStateFlow(false)
    val stopEnglishWritingTestMergedFilePlaying: StateFlow<Boolean> = _stopEnglishWritingTestMergedFilePlaying.asStateFlow()

    fun resetStopEnglishWritingTestMergedFilePlaying() {
        _stopEnglishWritingTestMergedFilePlaying.value = false
    }
} 