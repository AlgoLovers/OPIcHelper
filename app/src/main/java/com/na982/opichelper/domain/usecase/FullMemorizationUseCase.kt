package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.entity.QaItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.na982.opichelper.domain.repository.RecordingTimeManager

/**
 * 통암기 기능을 담당하는 Service
 * 책임: 영어 질문 TTS, 전체 스크립트 녹음, 녹음 재생, 하이라이트 관리
 */
@Singleton
class FullMemorizationService @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioFileManager: AudioFileManager,
    private val qaDataManager: QaDataManager,
    private val progressPersistenceService: ProgressPersistenceService,
    private val recordingTimeManager: RecordingTimeManager
) {
    // 녹음 상태 관리
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    // 재생 상태 관리
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    // 하이라이트 인덱스
    private val _highlightIndex = MutableStateFlow<Int?>(null)
    val highlightIndex: StateFlow<Int?> = _highlightIndex.asStateFlow()
    
    // 현재 녹음 파일 경로
    private var currentRecordingPath: String? = null
    
    // 녹음 상태 변경 콜백
    private var onRecordingStateChange: ((Boolean) -> Unit)? = null
    
    /**
     * 통암기 테스트 시작
     */
    suspend fun startFullMemorization(
        category: String,
        scriptIndex: Int,
        onRecordingStateChange: (Boolean) -> Unit,
        onPlayingStateChange: (Boolean) -> Unit,
        onHighlight: (Int?) -> Unit
    ) {
        try {
            Log.d("FullMemorizationUseCase", "통암기 테스트 시작: $category, $scriptIndex")
            
            // 콜백 저장
            this.onRecordingStateChange = onRecordingStateChange
            
            // 현재 QA 아이템 가져오기
            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem == null) {
                Log.e("FullMemorizationUseCase", "QA 아이템을 찾을 수 없음")
                return
            }
            
            // 1. 영어 질문 TTS 재생 (기존 질문 재생 기능 사용)
            Log.d("FullMemorizationUseCase", "영어 질문 TTS 시작")
            Log.d("FullMemorizationUseCase", "질문 내용: ${qaItem.questionEn}")
            
            // 기존 질문 재생 기능 사용 (문장별 하이라이트 포함)
            ttsOrchestrator.speakWithHighlight(qaItem.questionEn) { index ->
                onHighlight(index)
                Log.d("FullMemorizationUseCase", "영어 질문 하이라이트: $index")
            }
            
            Log.d("FullMemorizationUseCase", "영어 질문 TTS 재생 완료")
            
            // 하이라이트 제거
            onHighlight(null)
            Log.d("FullMemorizationUseCase", "영어 질문 하이라이트 제거")
            
            // TTS 완료 후 약간의 지연 (GUI 업데이트 대기)
            delay(500L)
            Log.d("FullMemorizationUseCase", "TTS 완료 후 지연 완료")
            
            // 2. 녹음 파일 경로 생성
            val timestamp = System.currentTimeMillis()
            val recordingFileName = "통암기_${category}_${scriptIndex}_${timestamp}.m4a"
            currentRecordingPath = audioFileManager.getRecordingFilePath(recordingFileName)
            
            // 3. 녹음 시작
            _isRecording.value = true
            onRecordingStateChange(true)
            
            Log.d("FullMemorizationUseCase", "녹음 시작: $currentRecordingPath")
            
            audioRecorder.startRecording(currentRecordingPath!!)
            
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "통암기 테스트 시작 실패", e)
            _isRecording.value = false
            onRecordingStateChange(false)
        }
    }
    
    /**
     * 녹음 종료
     */
    suspend fun stopRecording() {
        try {
            if (_isRecording.value && currentRecordingPath != null) {
                Log.d("FullMemorizationUseCase", "녹음 종료")
                
                audioRecorder.stopRecording()
                _isRecording.value = false
                onRecordingStateChange?.invoke(false)  // 저장된 콜백 호출
                
                Log.d("FullMemorizationUseCase", "녹음 완료: $currentRecordingPath")
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 종료 실패", e)
            _isRecording.value = false
            onRecordingStateChange?.invoke(false)  // 에러 시에도 상태 변경
        }
    }
    
    /**
     * 녹음 재생 (단순 재생)
     */
    suspend fun playRecording(
        onPlayingStateChange: (Boolean) -> Unit,
        onHighlight: (Int?) -> Unit
    ) {
        try {
            if (currentRecordingPath == null) {
                Log.e("FullMemorizationUseCase", "재생할 녹음 파일이 없음")
                return
            }
            
            Log.d("FullMemorizationUseCase", "녹음 재생 시작: $currentRecordingPath")
            
            _isPlaying.value = true
            onPlayingStateChange(true)
            
            // 녹음 재생 시작
            audioPlayer.playAudio(currentRecordingPath!!)
            
            // 녹음 재생 완료까지 대기
            val recordingDuration = audioPlayer.getDuration(currentRecordingPath!!)
            kotlinx.coroutines.delay(recordingDuration.toLong())
            
            // 재생 완료
            _isPlaying.value = false
            onPlayingStateChange(false)
            _highlightIndex.value = null
            onHighlight(null)
            
            Log.d("FullMemorizationUseCase", "녹음 재생 완료")
            
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _isPlaying.value = false
            onPlayingStateChange(false)
            _highlightIndex.value = null
            onHighlight(null)
        }
    }
    
    /**
     * 녹음 재생 (커스텀 타이밍)
     */
    suspend fun playRecordingWithCustomTiming(
        onPlayingStateChange: (Boolean) -> Unit,
        onHighlight: (Int?) -> Unit
    ) {
        try {
            if (currentRecordingPath == null) {
                Log.e("FullMemorizationUseCase", "재생할 녹음 파일이 없음")
                return
            }
            
            Log.d("FullMemorizationUseCase", "녹음 재생 시작: $currentRecordingPath")
            
            _isPlaying.value = true
            onPlayingStateChange(true)
            
            // 실제 오디오 재생 시작
            audioPlayer.playAudio(currentRecordingPath!!)
            Log.d("FullMemorizationUseCase", "오디오 재생 호출 완료")
            
            // 하이라이트 시작 전 1200ms 지연 (오디오와 싱크 맞추기)
            delay(1200L)
            Log.d("FullMemorizationUseCase", "하이라이트 시작 지연 완료")
            
            // 현재 QA 아이템 가져오기
            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem != null) {
                val category = qaItem.category
                val scriptIndex = qaDataManager.getCurrentIndex()
                
                // 1순위: 반복듣기에서 저장한 TTS 시간 확인
                if (recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                    Log.d("FullMemorizationUseCase", "1순위: 반복듣기 TTS 시간 데이터 사용")
                    val recordingTimes = recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
                    
                    // 각 문장에 대해 저장된 실제 시간으로 하이라이트
                    for (i in recordingTimes.indices) {
                        if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                            break
                        }
                        
                        onHighlight(i)
                        Log.d("FullMemorizationUseCase", "통암기 녹음 하이라이트 (반복듣기 시간): $i (시간: ${recordingTimes[i]}ms)")
                        
                        delay(recordingTimes[i])
                    }
                } else {
                    // 2순위: 영작테스트에서 저장한 TTS 시간 확인 (영작테스트는 다른 키를 사용할 수 있음)
                    val englishWritingTestKey = "english_writing_${category}_${scriptIndex}"
                    if (recordingTimeManager.hasRecordingTimes(englishWritingTestKey, 0)) {
                        Log.d("FullMemorizationUseCase", "2순위: 영작테스트 TTS 시간 데이터 사용")
                        val recordingTimes = recordingTimeManager.getAllRecordingTimes(englishWritingTestKey, 0)
                        
                        for (i in recordingTimes.indices) {
                            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                                break
                            }
                            
                            onHighlight(i)
                            Log.d("FullMemorizationUseCase", "통암기 녹음 하이라이트 (영작테스트 시간): $i (시간: ${recordingTimes[i]}ms)")
                            
                            delay(recordingTimes[i])
                        }
                    } else {
                        // 3순위: 기본 하이라이트 (텍스트 길이 기반)
                        Log.d("FullMemorizationUseCase", "3순위: 문자열 길이 기반 시간 사용")
                        val answerSentences = qaItem.answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                        
                        for (i in answerSentences.indices) {
                            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                                break
                            }
                            
                            onHighlight(i)
                            Log.d("FullMemorizationUseCase", "통암기 녹음 하이라이트 (문자열 길이): $i")
                            
                            val sentenceLength = answerSentences[i].length
                            val waitTime = maxOf(sentenceLength * 50L, 1000L)
                            delay(waitTime)
                        }
                    }
                }
            }
            
            // 재생 완료
            _isPlaying.value = false
            onPlayingStateChange(false)
            onHighlight(null)
            
            Log.d("FullMemorizationUseCase", "녹음 재생 완료")
            
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            _isPlaying.value = false
            onPlayingStateChange(false)
            onHighlight(null)
        }
    }
    
    /**
     * 녹음 재생 후 재생 시간 반환
     */
    suspend fun getRecordingDuration(): Long {
        return currentRecordingPath?.let { audioPlayer.getDuration(it).toLong() } ?: 0L
    }
    
    /**
     * 재생 중지
     */
    suspend fun stopPlaying() {
        try {
            if (_isPlaying.value) {
                Log.d("FullMemorizationUseCase", "녹음 재생 중지")
                
                audioPlayer.stopAudio()
                _isPlaying.value = false
                _highlightIndex.value = null
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 중지 실패", e)
            _isPlaying.value = false
            _highlightIndex.value = null
        }
    }
    
    /**
     * 현재 녹음 파일 경로 가져오기
     */
    fun getCurrentRecordingPath(): String? = currentRecordingPath
    
    /**
     * 녹음 파일 존재 여부 확인
     */
    suspend fun hasRecordingFile(): Boolean {
        return currentRecordingPath?.let { audioFileManager.hasRecordingFileByPath(it) } ?: false
    }
    
    /**
     * 녹음 파일 삭제
     */
    suspend fun deleteRecordingFile() {
        currentRecordingPath?.let { path ->
            audioFileManager.deleteRecordingFileByPath(path)
            currentRecordingPath = null
            Log.d("FullMemorizationUseCase", "녹음 파일 삭제: $path")
        }
    }
    
    /**
     * 녹음 파일 가져오기
     */
    suspend fun getRecordingFile(category: String, scriptIndex: Int): java.io.File? {
        return audioFileManager.getFullMemorizationRecording(category, scriptIndex)
    }
    
    /**
     * 녹음 시간 데이터 존재 여부 확인
     */
    suspend fun hasRecordingTimes(category: String, scriptIndex: Int): Boolean {
        return recordingTimeManager.hasRecordingTimes(category, scriptIndex)
    }
    
    /**
     * 모든 녹음 시간 데이터 가져오기
     */
    suspend fun getAllRecordingTimes(category: String, scriptIndex: Int): List<Long> {
        return recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
    }
    
    /**
     * 상태 초기화
     */
    fun reset() {
        _isRecording.value = false
        _isPlaying.value = false
        _highlightIndex.value = null
        currentRecordingPath = null
    }
} 