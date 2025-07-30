package com.na982.opichelper.domain.audio

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.*
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.TtsOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS 재생 제어 전담 클래스 (Controller 패턴)
 * 책임: TTS 재생 제어, 하이라이트 관리, 재생 상태 관리, TTS 서비스 바인딩
 */
@Singleton
class TtsPlaybackController @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    private val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    // 재생 상태 관리
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isQuestionPlaying = MutableStateFlow(false)
    val isQuestionPlaying: StateFlow<Boolean> = _isQuestionPlaying.asStateFlow()

    private val _isAnswerPlaying = MutableStateFlow(false)
    val isAnswerPlaying: StateFlow<Boolean> = _isAnswerPlaying.asStateFlow()
    
    // 하이라이트 상태 관리
    private val _questionHighlightIndex = MutableStateFlow<Int?>(null)
    val questionHighlightIndex: StateFlow<Int?> = _questionHighlightIndex.asStateFlow()
    
    private val _answerHighlightIndex = MutableStateFlow<Int?>(null)
    val answerHighlightIndex: StateFlow<Int?> = _answerHighlightIndex.asStateFlow()
    
    private val _answerKoHighlightIndex = MutableStateFlow<Int?>(null)
    val answerKoHighlightIndex: StateFlow<Int?> = _answerKoHighlightIndex.asStateFlow()
    
    private val _recordingHighlightIndex = MutableStateFlow<Int?>(null)
    val recordingHighlightIndex: StateFlow<Int?> = _recordingHighlightIndex.asStateFlow()
    
    // TTS 오케스트레이터 설정
    private var ttsOrchestrator: TtsOrchestrator? = null
    
    // 다른 암기 모드 중단을 위한 콜백
    private var onStopOtherMemorizationMode: (() -> Unit)? = null
    
    // TTS 서비스 바인딩 관련
    private var serviceConnection: ServiceConnection? = null
    private var isServiceBound = false
    
    fun setTtsOrchestrator(orchestrator: TtsOrchestrator) {
        ttsOrchestrator = orchestrator
        Log.d("TtsPlaybackController", "TTS 오케스트레이터 설정 완료")
    }
    
    fun setOnStopOtherMemorizationMode(callback: () -> Unit) {
        onStopOtherMemorizationMode = callback
        Log.d("TtsPlaybackController", "다른 암기 모드 중단 콜백 설정")
    }
    
    /**
     * TTS 서비스 바인딩 시작
     */
    fun bindTtsService(context: Context, onKoreanTtsServiceUpdate: ((String) -> Unit)? = null) {
        Log.d("TtsPlaybackController", "TTS 서비스 바인딩 시작")
        
        // 이미 바인딩되어 있으면 해제
        if (isServiceBound) {
            Log.d("TtsPlaybackController", "이미 바인딩되어 있음 - 해제 후 재바인딩")
            unbindTtsService(context)
        }
        
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
                Log.d("TtsPlaybackController", "TTS 서비스 연결됨")
                isServiceBound = true
                
                // 서비스 바인딩은 제거했으므로 직접 TtsOrchestrator 사용
                
                // 한글 TTS 서비스 이름 업데이트
                ttsOrchestrator?.let { orchestrator ->
                    val serviceName = orchestrator.getCurrentKoreanTtsServiceName()
                    Log.d("TtsPlaybackController", "한글 TTS 서비스: $serviceName")
                    onKoreanTtsServiceUpdate?.invoke(serviceName)
                }
                
                Log.d("TtsPlaybackController", "TTS 플레이어 준비 완료")
            }
            
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                Log.d("TtsPlaybackController", "TTS 서비스 연결 해제됨")
                isServiceBound = false
            }
        }
        
        // TtsOrchestrator를 직접 사용하므로 서비스 바인딩 불필요
        Log.d("TtsPlaybackController", "TTS 오케스트레이터 직접 사용")
    }
    
    /**
     * TTS 서비스 바인딩 해제
     */
    fun unbindTtsService(context: Context) {
        Log.d("TtsPlaybackController", "TTS 서비스 바인딩 해제")
        
        // 바인딩되지 않은 경우 해제하지 않음
        if (!isServiceBound || serviceConnection == null) {
            Log.d("TtsPlaybackController", "서비스가 바인딩되지 않음 - 해제 건너뜀")
            return
        }
        
        try {
            serviceConnection?.let { connection ->
                context.unbindService(connection)
                Log.d("TtsPlaybackController", "TTS 서비스 바인딩 해제 완료")
            }
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 서비스 바인딩 해제 중 오류", e)
        } finally {
            serviceConnection = null
            isServiceBound = false
        }
    }
    
    /**
     * 질문 TTS 재생
     */
    fun playQuestion(question: String) {
        coroutineScope.launch {
            // 1단계: 이미 질문 재생 중이면 토글(멈춤)
            if (_isQuestionPlaying.value) {
                Log.d("TtsPlaybackController", "질문 재생 중 - 토글하여 중지")
                stopTts()
                return@launch
            }
            
            // 2단계: 다른 TTS가 재생 중이면 먼저 중지하고 완료 대기
            if (_isAnswerPlaying.value || (_isPlaying.value && !_isQuestionPlaying.value)) {
                Log.d("TtsPlaybackController", "다른 TTS 재생 중 - 먼저 중지")
                stopTts()
                Log.d("TtsPlaybackController", "다른 TTS 중지 완료")
            }
            
            // 3단계: 다른 암기 모드 중단 (MemorizationViewModel 이벤트 발생)
            // MainViewModel에서 처리하던 부분을 여기서 처리
            Log.d("TtsPlaybackController", "다른 암기 모드 중단 요청 - MainViewModel에서 처리 필요")
            
            // 4단계: 새 질문 재생 시작
            try {
                Log.d("TtsPlaybackController", "질문 TTS 재생 시작: $question")
                _isPlaying.value = true
                _isQuestionPlaying.value = true
                
                ttsOrchestrator?.speakWithHighlight(question) { index ->
                    _questionHighlightIndex.value = index
                }
                
                Log.d("TtsPlaybackController", "질문 TTS 재생 완료")
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "질문 TTS 재생 오류", e)
            } finally {
                _isPlaying.value = false
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null
            }
        }
    }
    
    /**
     * 답변 TTS 재생
     */
    fun playAnswer(answer: String) {
        coroutineScope.launch {
            // 1단계: 이미 답변 재생 중이면 토글(멈춤)
            if (_isAnswerPlaying.value) {
                Log.d("TtsPlaybackController", "답변 재생 중 - 토글하여 중지")
                stopTts()
                return@launch
            }
            
            // 2단계: 다른 TTS가 재생 중이면 먼저 중지하고 완료 대기
            if (_isQuestionPlaying.value || (_isPlaying.value && !_isAnswerPlaying.value)) {
                Log.d("TtsPlaybackController", "다른 TTS 재생 중 - 먼저 중지")
                stopTts()
                Log.d("TtsPlaybackController", "다른 TTS 중지 완료")
            }
            
            // 3단계: 다른 암기 모드 중단
            Log.d("TtsPlaybackController", "다른 암기 모드 중단 요청 - MainViewModel에서 처리 필요")
            
            // 4단계: 새 답변 재생 시작
            try {
                Log.d("TtsPlaybackController", "답변 TTS 재생 시작: $answer")
                _isPlaying.value = true
                _isAnswerPlaying.value = true
                
                ttsOrchestrator?.speakWithHighlight(answer) { index ->
                    _answerHighlightIndex.value = index
                }
                
                Log.d("TtsPlaybackController", "답변 TTS 재생 완료")
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "답변 TTS 재생 오류", e)
            } finally {
                _isPlaying.value = false
                _isAnswerPlaying.value = false
                _answerHighlightIndex.value = null
            }
        }
    }
    
    /**
     * 질문과 답변을 합쳐서 재생
     */
    fun playMergedAudio(question: String, answer: String) {
        coroutineScope.launch {
            // 1단계: 다른 TTS가 재생 중이면 먼저 중지하고 완료 대기
            if (_isQuestionPlaying.value || _isAnswerPlaying.value || _isPlaying.value) {
                Log.d("TtsPlaybackController", "다른 TTS 재생 중 - 먼저 중지")
                stopTts()
                Log.d("TtsPlaybackController", "다른 TTS 중지 완료")
            }
            
            // 2단계: 합쳐진 오디오 재생 시작
            try {
                Log.d("TtsPlaybackController", "합쳐진 오디오 재생 시작")
                _isPlaying.value = true
                
                // 질문 먼저 재생
                Log.d("TtsPlaybackController", "질문 부분 재생")
                _isQuestionPlaying.value = true
                ttsOrchestrator?.speakWithHighlight(question) { index ->
                    _questionHighlightIndex.value = index
                }
                _isQuestionPlaying.value = false
                _questionHighlightIndex.value = null
                
                // 잠시 대기
                kotlinx.coroutines.delay(500)
                
                // 답변 재생
                Log.d("TtsPlaybackController", "답변 부분 재생")
                _isAnswerPlaying.value = true
                ttsOrchestrator?.speakWithHighlight(answer) { index ->
                    _answerHighlightIndex.value = index
                }
                _isAnswerPlaying.value = false
                _answerHighlightIndex.value = null
                
                Log.d("TtsPlaybackController", "합쳐진 오디오 재생 완료")
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "합쳐진 오디오 재생 오류", e)
            } finally {
                _isPlaying.value = false
                _isQuestionPlaying.value = false
                _isAnswerPlaying.value = false
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
            }
        }
    }
    
    /**
     * TTS 재생 중지
     */
    fun stopTts() {
        coroutineScope.launch {
            try {
                Log.d("TtsPlaybackController", "TTS 재생 중지 시작")
                
                // 1. TTS 오케스트레이터 중지
                ttsOrchestrator?.stop()
                
                // 2. AudioPlayer 중지 제거 - 녹음 재생과 충돌 방지
                // audioPlayer.stop() // 이 라인 제거
                
                // 3. 상태 초기화
                _isPlaying.value = false
                _isQuestionPlaying.value = false
                _isAnswerPlaying.value = false
                _questionHighlightIndex.value = null
                _answerHighlightIndex.value = null
                _answerKoHighlightIndex.value = null
                _recordingHighlightIndex.value = null
                
                // 4. 하이라이트 초기화
                clearHighlight()
                
                Log.d("TtsPlaybackController", "TTS 재생 중지 완료")
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "TTS 재생 중지 실패", e)
            }
        }
    }
    
    /**
     * TTS 일시 중지 (설정화면 진입 시)
     */
    fun pauseTts() {
        Log.d("TtsPlaybackController", "TTS 일시 중지")
        try {
            // 현재 재생 중인 TTS만 일시 중지
            if (_isPlaying.value) {
                ttsOrchestrator?.pause()
                Log.d("TtsPlaybackController", "TTS 일시 중지 완료")
            }
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 일시 중지 실패", e)
        }
    }
    
    /**
     * TTS 재개 (설정화면에서 복귀 시)
     */
    fun resumeTts() {
        Log.d("TtsPlaybackController", "TTS 재개")
        try {
            // 이전에 재생 중이었던 TTS 재개
            if (_isPlaying.value) {
                ttsOrchestrator?.resume()
                Log.d("TtsPlaybackController", "TTS 재개 완료")
            }
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 재개 실패", e)
        }
    }
    
    /**
     * 질문 TTS만 중지
     */
    fun stopQuestion() {
        Log.d("TtsPlaybackController", "질문 TTS만 중지")
        if (_isQuestionPlaying.value) {
            stopTts()
            _isQuestionPlaying.value = false
            _questionHighlightIndex.value = null
        }
    }
    
    /**
     * 답변 TTS만 중지
     */
    fun stopAnswer() {
        Log.d("TtsPlaybackController", "답변 TTS만 중지")
        if (_isAnswerPlaying.value) {
            stopTts()
            _isAnswerPlaying.value = false
            _answerHighlightIndex.value = null
            _answerKoHighlightIndex.value = null
        }
    }
    
    /**
     * 모든 TTS 중지 (다른 TTS 시작 전)
     */
    fun stopAllTts() {
        Log.d("TtsPlaybackController", "모든 TTS 중지")
        stopTts()
        _isPlaying.value = false
        _isQuestionPlaying.value = false
        _isAnswerPlaying.value = false
        _questionHighlightIndex.value = null
        _answerHighlightIndex.value = null
        _answerKoHighlightIndex.value = null
        _recordingHighlightIndex.value = null
    }
    
    /**
     * TTS 완전 정리 (앱 종료 시)
     */
    fun cleanupTts() {
        Log.d("TtsPlaybackController", "TTS 완전 정리 시작")
        try {
            // 1. 모든 TTS 중지
            stopAllTts()
            
            // 2. TTS 오케스트레이터 완전 해제
            ttsOrchestrator?.releaseAllPlayers()
            
            // 3. 오디오 플레이어 정리
            audioPlayer.stop()
            
            Log.d("TtsPlaybackController", "TTS 완전 정리 완료")
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 완전 정리 실패", e)
        }
    }
    
    /**
     * 오디오 파일 재생
     */
    fun playAudioFile(file: File) {
        coroutineScope.launch {
            try {
                _isPlaying.value = true
                Log.d("TtsPlaybackController", "오디오 파일 재생: ${file.name}")
                audioPlayer.play(file) {
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "오디오 파일 재생 오류", e)
                _isPlaying.value = false
            }
        }
    }
    
    /**
     * 오디오 재생 중지
     */
    fun stopAudio() {
        coroutineScope.launch {
            try {
                Log.d("TtsPlaybackController", "오디오 재생 중지")
                audioPlayer.stop()
                _isPlaying.value = false
            } catch (e: Exception) {
                Log.e("TtsPlaybackController", "오디오 중지 오류", e)
            }
        }
    }
    
    /**
     * TTS 강제 중지 (동기적, 백키 종료 시 사용)
     */
    fun forceStopTts() {
        try {
            Log.d("TtsPlaybackController", "TTS 강제 중지 시작")
            
            // 1. TTS 오케스트레이터 강제 중지
            ttsOrchestrator?.stop()
            
            // 2. 오디오 플레이어 강제 중지
            audioPlayer.stop()
            
            // 3. 모든 상태 즉시 초기화
            _isPlaying.value = false
            _isQuestionPlaying.value = false
            _isAnswerPlaying.value = false
            _questionHighlightIndex.value = null
            _answerHighlightIndex.value = null
            _answerKoHighlightIndex.value = null
            _recordingHighlightIndex.value = null
            
            Log.d("TtsPlaybackController", "TTS 강제 중지 완료")
        } catch (e: Exception) {
            Log.e("TtsPlaybackController", "TTS 강제 중지 오류", e)
        }
    }
    
    /**
     * 현재 한글 TTS 서비스 이름 반환
     */
    fun getCurrentKoreanTtsServiceName(): String {
        return ttsOrchestrator?.getCurrentKoreanTtsServiceName() ?: "없음"
    }
    
    /**
     * 질문 하이라이트 인덱스 설정
     */
    fun setQuestionHighlightIndex(index: Int) {
        _questionHighlightIndex.value = index
        Log.d("TtsPlaybackController", "질문 하이라이트 설정: $index")
    }
    
    /**
     * 답변 하이라이트 인덱스 설정
     */
    fun setAnswerHighlightIndex(index: Int) {
        _answerHighlightIndex.value = index
        Log.d("TtsPlaybackController", "답변 하이라이트 설정: $index")
    }
    
    /**
     * 답변 한글 하이라이트 인덱스 설정
     */
    fun setAnswerKoHighlightIndex(index: Int) {
        _answerKoHighlightIndex.value = index
        Log.d("TtsPlaybackController", "답변 한글 하이라이트 설정: $index")
    }
    
    /**
     * 녹음 하이라이트 인덱스 설정
     */
    fun setRecordingHighlightIndex(index: Int) {
        _recordingHighlightIndex.value = index
        Log.d("TtsPlaybackController", "녹음 하이라이트 설정: $index")
    }
    
    /**
     * 하이라이트 상태 초기화
     */
    fun clearHighlight() {
        Log.d("TtsPlaybackController", "하이라이트 상태 초기화")
        _questionHighlightIndex.value = null
        _answerHighlightIndex.value = null
        _answerKoHighlightIndex.value = null
        _recordingHighlightIndex.value = null
    }
} 