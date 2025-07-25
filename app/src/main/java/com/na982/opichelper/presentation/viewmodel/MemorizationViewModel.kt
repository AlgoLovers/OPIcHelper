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
import com.na982.opichelper.domain.repository.AudioFileManager

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val qaDataManager: QaDataManager,
    private val ttsPlaybackController: TtsPlaybackController,
    private val progressTracker: MemorizeTestProgressTracker,
    private val fullMemorizationService: FullMemorizationService,
    private val repeatListeningService: RepeatListeningService,
    private val englishWritingTestService: EnglishWritingTestService,
    private val audioFileManager: AudioFileManager
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
                Log.d("MemorizationViewModel", "onMemorizeTestButtonClick 호출됨 - selectedLevel: '$selectedLevel'")
                
                when (selectedLevel) {
                    "반복 듣기" -> {
                        Log.d("MemorizationViewModel", "반복 듣기 모드 선택됨")
                        _isRunning.value = true
                        ttsPlaybackController.stopTts()
                        ttsPlaybackController.clearHighlight()
                        startRepeatListening()
                    }
                    "영작 테스트" -> {
                        Log.d("MemorizationViewModel", "영작 테스트 모드 선택됨")
                        _isEnglishWritingTestMode.value = true
                        _isEnglishWritingTestCardFlipped.value = false // 초기 상태는 영문
                        ttsPlaybackController.stopTts()
                        ttsPlaybackController.clearHighlight()
                        // 영작테스트 녹음 파일 재생 중단 이벤트 발생
                        _stopEnglishWritingTestMergedFilePlaying.value = true
                        startEnglishWritingTest()
                    }
                    "통암기" -> {
                        Log.d("MemorizationViewModel", "통암기 모드 선택됨 - 진입 시작")
                        viewModelScope.launch {
                            Log.d("MemorizationViewModel", "통암기 모드 진입 시작")
                            updateFullMemorizationRecordingStatus()
                            Log.d("MemorizationViewModel", "통암기 모드 진입 전 녹음 파일 상태: ${_hasFullMemorizationRecording.value}")
                            
                            _isFullMemorizationMode.value = true
                            ttsPlaybackController.stopTts()
                            ttsPlaybackController.clearHighlight()
                            startFullMemorizationMode()
                            
                            Log.d("MemorizationViewModel", "통암기 모드 진입 완료")
                        }
                    }
                    else -> {
                        Log.w("MemorizationViewModel", "알 수 없는 암기 레벨: '$selectedLevel'")
                    }
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
                Log.d("MemorizationViewModel", "startFullMemorizationMode 호출됨")
                currentUseCaseJob?.cancel()
                _isFullMemorizationMode.value = true
                _isRunning.value = true
                ttsPlaybackController.stopTts()
                
                // 통암기 모드 진입 시 녹음 파일 상태 확인
                Log.d("MemorizationViewModel", "통암기 모드 진입 시 녹음 파일 상태 확인 시작")
                updateFullMemorizationRecordingStatus()
                Log.d("MemorizationViewModel", "통암기 모드 진입 시 녹음 파일 상태 확인 완료")
                
                currentUseCaseJob = viewModelScope.launch {
                    val category = qaDataManager.getCurrentCategory() ?: ""
                    val scriptIndex = qaDataManager.getCurrentIndex()
                    Log.d("MemorizationViewModel", "통암기 서비스 시작: category=$category, scriptIndex=$scriptIndex")
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
                            // 통암기 하이라이트를 질문 하이라이트로 연결
                            if (index != null) {
                                Log.d("MemorizationViewModel", "통암기: 질문 하이라이트 설정 요청: $index")
                                ttsPlaybackController.setQuestionHighlightIndex(index)
                                Log.d("MemorizationViewModel", "통암기: 질문 하이라이트 설정 완료: $index")
                            } else {
                                Log.d("MemorizationViewModel", "통암기: 질문 하이라이트 제거 요청")
                                ttsPlaybackController.clearHighlight()
                                Log.d("MemorizationViewModel", "통암기: 질문 하이라이트 제거 완료")
                            }
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
                
                // 녹음 종료 후 녹음 파일 상태 업데이트
                updateFullMemorizationRecordingStatus()
                
                Log.d("MemorizationViewModel", "통암기 녹음 종료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 종료 실패", e)
            }
        }
    }

    fun playFullMemorizationRecording() {
        viewModelScope.launch {
            try {
                // 다른 TTS 중단
                ttsPlaybackController.stopTts()
                
                val currentItem = qaDataManager.getCurrentQaItem()
                if (currentItem != null) {
                    val recordingFile = fullMemorizationService.getRecordingFile(
                        currentItem.category,
                        qaDataManager.getCurrentIndex()
                    )
                    
                    if (recordingFile != null) {
                        _isFullMemorizationPlaying.value = true
                        Log.d("MemorizationViewModel", "통암기 녹음 재생 시작: ${recordingFile.absolutePath}")
                        
                        // FullMemorizationService를 통해 재생
                        fullMemorizationService.playRecordingWithCustomTiming(
                            onPlayingStateChange = { isPlaying ->
                                _isFullMemorizationPlaying.value = isPlaying
                                Log.d("MemorizationViewModel", "통암기 재생 상태 변경: $isPlaying")
                            },
                            onHighlight = { index ->
                                _fullMemorizationHighlightIndex.value = index
                                Log.d("MemorizationViewModel", "통암기 하이라이트 변경: $index")
                            }
                        )
                        
                        Log.d("MemorizationViewModel", "통암기 녹음 재생 완료")
                    } else {
                        Log.d("MemorizationViewModel", "통암기 녹음 파일이 존재하지 않음")
                    }
                }
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 녹음 재생 실패", e)
                _isFullMemorizationPlaying.value = false
                _fullMemorizationHighlightIndex.value = null
            }
        }
    }

    fun stopFullMemorizationPlaying() {
        viewModelScope.launch {
            try {
                fullMemorizationService.stopPlaying()
                _isFullMemorizationPlaying.value = false
                _fullMemorizationHighlightIndex.value = null
                Log.d("MemorizationViewModel", "통암기 재생 중지")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "통암기 재생 중지 실패", e)
                _isFullMemorizationPlaying.value = false
                _fullMemorizationHighlightIndex.value = null
            }
        }
    }

    suspend fun hasFullMemorizationRecording(): Boolean {
        return fullMemorizationService.hasRecordingFile()
    }

    private suspend fun updateFullMemorizationRecordingStatus() {
        Log.d("MemorizationViewModel", "updateFullMemorizationRecordingStatus 시작")
        
        val currentItem = qaDataManager.getCurrentQaItem()
        if (currentItem == null) {
            Log.d("MemorizationViewModel", "현재 QA 아이템이 null - 녹음 파일 상태를 false로 설정")
            _hasFullMemorizationRecording.value = false
            return
        }
        
        val category = currentItem.category
        val scriptIndex = qaDataManager.getCurrentIndex()
        Log.d("MemorizationViewModel", "녹음 파일 확인: category='$category', scriptIndex=$scriptIndex")
        
        // AudioFileManager를 직접 사용하여 파일 존재 여부 확인
        val hasRecording = audioFileManager.hasFullMemorizationRecording(category, scriptIndex)
        Log.d("MemorizationViewModel", "audioFileManager.hasFullMemorizationRecording() 결과: $hasRecording")
        
        // 추가 확인: 실제 파일 존재 여부
        val recordingFile = audioFileManager.getFullMemorizationRecording(category, scriptIndex)
        val fileExists = recordingFile?.exists() == true
        Log.d("MemorizationViewModel", "실제 파일 존재 여부: $fileExists, 파일 경로: ${recordingFile?.absolutePath}")
        
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
                                    Log.d("MemorizationViewModel", "반복 듣기: 영문 하이라이트 설정: $index")
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MemorizationViewModel", "반복 듣기: 영문 하이라이트 제거")
                                }
                            },
                            onKoreanHighlight = { index ->
                                if (index != null) {
                                    ttsPlaybackController.setAnswerKoHighlightIndex(index)
                                    Log.d("MemorizationViewModel", "반복 듣기: 한글 하이라이트 설정: $index")
                                } else {
                                    ttsPlaybackController.clearHighlight()
                                    Log.d("MemorizationViewModel", "반복 듣기: 한글 하이라이트 제거")
                                }
                            },
                            onCardFlip = { isKorean ->
                                _isRepeatListeningCardFlipped.value = isKorean
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

    /**
     * 암기레벨 변경 시 상태 초기화
     */
    fun onMemorizeLevelChanged() {
        viewModelScope.launch {
            try {
                Log.d("MemorizationViewModel", "암기레벨 변경 감지 - 상태 초기화")
                
                // 현재 실행 중인 작업 중단
                currentUseCaseJob?.cancel()
                currentUseCaseJob = null
                
                // 기본 상태 초기화 (통암기 모드는 유지)
                _isRunning.value = false
                // _isFullMemorizationMode.value = false  // ← 통암기 모드는 유지
                _isEnglishWritingTestMode.value = false
                _isEnglishWritingTestCardFlipped.value = false
                _isRepeatListeningCardFlipped.value = false
                _fullMemorizationHighlightIndex.value = null
                _isFullMemorizationRecording.value = false
                _isFullMemorizationPlaying.value = false
                
                // hasFullMemorizationRecording은 초기화하지 않음 (파일 존재 여부는 유지)
                // _hasFullMemorizationRecording.value = false  // ← 이 줄 제거
                
                _englishWritingTestCompleted.value = false
                _stopEnglishWritingTestMergedFilePlaying.value = false
                
                // TTS 중단
                ttsPlaybackController.stopTts()
                ttsPlaybackController.clearHighlight()
                
                Log.d("MemorizationViewModel", "암기레벨 변경으로 인한 상태 초기화 완료")
            } catch (e: Exception) {
                Log.e("MemorizationViewModel", "암기레벨 변경 처리 실패", e)
            }
        }
    }

    // 반복듣기 카드 상태
    private val _isRepeatListeningCardFlipped = MutableStateFlow(false)
    val isRepeatListeningCardFlipped: StateFlow<Boolean> = _isRepeatListeningCardFlipped.asStateFlow()
    
    // 통암기 모드 진입 시 녹음 파일 상태 확인
    init {
        viewModelScope.launch {
            _isFullMemorizationMode.collect { isFullMemorizationMode ->
                if (isFullMemorizationMode) {
                    Log.d("MemorizationViewModel", "통암기 모드 진입 감지 - 녹음 파일 상태 확인")
                    updateFullMemorizationRecordingStatus()
                }
            }
        }
    }
    
    // 스크립트 변경 시 통암기 녹음 파일 존재 여부 확인
    init {
        viewModelScope.launch {
            qaDataManager.currentQaItem.collect { currentItem ->
                if (currentItem != null) {
                    Log.d("MemorizationViewModel", "스크립트 변경 감지 - 통암기 녹음 파일 상태 확인")
                    updateFullMemorizationRecordingStatus()
                }
            }
        }
    }
} 