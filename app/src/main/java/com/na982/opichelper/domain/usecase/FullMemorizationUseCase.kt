package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.repository.AudioFileRepository
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.repository.ProgressRepository
import com.na982.opichelper.domain.usecase.MemorizeTestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import android.util.Log
import javax.inject.Inject

/**
 * 통암기 기능을 담당하는 UseCase
 * 책임: 전체 스크립트 녹음, 녹음 재생, 하이라이트 관리
 */
class FullMemorizationUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioFileRepository: AudioFileRepository,
    private val qaDataRepository: QaDataRepository,
    private val memorizeTestState: MemorizeTestState
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
            val qaItem = qaDataRepository.getCurrentQaItem()
            if (qaItem == null) {
                Log.e("FullMemorizationUseCase", "QA 아이템을 찾을 수 없음")
                return
            }
            
            // 녹음 파일 경로 생성
            val recordingFileName = "full_memorization_${category}_${scriptIndex}.m4a"
            currentRecordingPath = audioFileRepository.getRecordingFilePath(recordingFileName)
            
            // 녹음 시작
            _isRecording.value = true
            onRecordingStateChange(true)
            
            Log.d("FullMemorizationUseCase", "녹음 시작: $currentRecordingPath")
            
            // 녹음 시작
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
        return currentRecordingPath?.let { audioFileRepository.hasRecordingFileByPath(it) } ?: false
    }
    
    /**
     * 녹음 파일 삭제
     */
    suspend fun deleteRecordingFile() {
        currentRecordingPath?.let { path ->
            audioFileRepository.deleteRecordingFileByPath(path)
            currentRecordingPath = null
            Log.d("FullMemorizationUseCase", "녹음 파일 삭제: $path")
        }
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