package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class FullMemorizationUseCase @Inject constructor(
    private val recordingFileRepository: RecordingFileRepository,
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioRecorder: AudioRecorder,
    private val qaDataManager: QaDataManager,
    private val recordingTimeManager: RecordingTimeManager
) {
    private val _highlightIndex = MutableStateFlow<Int?>(null)
    val highlightIndex: StateFlow<Int?> = _highlightIndex.asStateFlow()
    private var isRecording = false
    private var isPlaying = false
    private var currentRecordingPath: String? = null

    /**
     * 통암기 테스트 시작 (질문 재생 후 녹음)
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
            
            // 1. 질문 재생
            Log.d("FullMemorizationUseCase", "질문 재생 시작")
            onPlayingStateChange(true)
            
            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem != null) {
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
                
                // 2. 녹음 파일 생성
                currentRecordingPath = recordingFileRepository.createRecordingFile(category, scriptIndex)
                
                // 3. 녹음 시작
                isRecording = true
                onRecordingStateChange(true)
                
                Log.d("FullMemorizationUseCase", "녹음 시작: $currentRecordingPath")
                
                audioRecorder.startRecording(currentRecordingPath!!)
                
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "통암기 테스트 시작 실패", e)
            isRecording = false
            onRecordingStateChange(false)
        }
    }
    
    /**
     * 녹음 종료
     */
    suspend fun stopRecording() {
        try {
            if (isRecording && currentRecordingPath != null) {
                Log.d("FullMemorizationUseCase", "녹음 종료")
                
                audioRecorder.stopRecording()
                isRecording = false
                
                Log.d("FullMemorizationUseCase", "녹음 완료: $currentRecordingPath")
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 종료 실패", e)
            isRecording = false
        }
    }
    
    /**
     * 녹음 파일 재생 (하이라이트 포함)
     */
    suspend fun playRecordingWithHighlight(
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem == null) {
                Log.e("FullMemorizationUseCase", "재생할 QA 아이템이 없음")
                return
            }
            val category = qaItem.category
            val scriptIndex = qaDataManager.getCurrentIndex()
            if (recordingFileRepository.hasRecordingFile(category, scriptIndex)) {
                Log.d("FullMemorizationUseCase", "녹음 재생 시작 (하이라이트 포함)")
                isPlaying = true
                onPlayingStateChange(true)
                
                // 녹음 재생과 하이라이트를 동시에 시작
                val filePath = recordingFileRepository.getRecordingFilePath(category, scriptIndex)
                if (filePath != null) {
                    // 녹음 재생 시작 (비동기)
                    CoroutineScope(Dispatchers.IO).launch {
                        recordingFileRepository.playRecordingFileSimple(category, scriptIndex) { playing ->
                            if (!playing) isPlaying = false
                        }
                    }
                    
                    // 하이라이트 시작 전 약간의 지연 (오디오와 싱크 맞추기)
                    delay(1200L)
                    Log.d("FullMemorizationUseCase", "하이라이트 루프 시작")
                    
                    val times = if (recordingTimeManager.hasRecordingTimes(category, scriptIndex)) {
                        recordingTimeManager.getAllRecordingTimes(category, scriptIndex)
                    } else {
                        listOf(2000L, 2000L, 2000L, 2000L, 2000L)
                    }
                    
                    for (i in times.indices) {
                        if (!currentCoroutineContext().isActive) break
                        _highlightIndex.value = i
                        Log.d("FullMemorizationUseCase", "녹음 재생 하이라이트: $i (시간: ${times[i]}ms)")
                        delay(times[i])
                    }
                    _highlightIndex.value = null
                    Log.d("FullMemorizationUseCase", "하이라이트 루프 완료")
                }
                onPlayingStateChange(false)
                Log.d("FullMemorizationUseCase", "녹음 재생 완료 (하이라이트 포함)")
            } else {
                Log.d("FullMemorizationUseCase", "재생할 녹음 파일이 없음")
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            isPlaying = false
            onPlayingStateChange(false)
            _highlightIndex.value = null
        }
    }
    
    /**
     * 녹음 파일 재생 (하이라이트 없음)
     */
    suspend fun playRecordingSimple(
        onPlayingStateChange: (Boolean) -> Unit
    ) {
        try {
            val qaItem = qaDataManager.getCurrentQaItem()
            if (qaItem == null) {
                Log.e("FullMemorizationUseCase", "재생할 QA 아이템이 없음")
                return
            }
            
            val category = qaItem.category
            val scriptIndex = qaDataManager.getCurrentIndex()
            
            if (recordingFileRepository.hasRecordingFile(category, scriptIndex)) {
                Log.d("FullMemorizationUseCase", "녹음 재생 시작 (하이라이트 없음)")
                isPlaying = true
                
                recordingFileRepository.playRecordingFileSimple(
                    category = category,
                    scriptIndex = scriptIndex,
                    onPlayingStateChange = { isPlaying ->
                        Log.d("FullMemorizationUseCase", "녹음 재생 상태 변경: isPlaying=$isPlaying")
                        onPlayingStateChange(isPlaying)
                        if (!isPlaying) {
                            this.isPlaying = false
                        }
                    }
                )
                
                Log.d("FullMemorizationUseCase", "녹음 재생 완료")
            } else {
                Log.d("FullMemorizationUseCase", "재생할 녹음 파일이 없음")
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationUseCase", "녹음 재생 실패", e)
            isPlaying = false
            onPlayingStateChange(false)
        }
    }
    
    /**
     * 녹음 파일 존재 여부 확인
     */
    suspend fun hasRecording(): Boolean {
        val qaItem = qaDataManager.getCurrentQaItem()
        return if (qaItem != null) {
            recordingFileRepository.hasRecordingFile(qaItem.category, qaDataManager.getCurrentIndex())
        } else {
            false
        }
    }
    
    /**
     * 녹음 파일 삭제
     */
    suspend fun clearRecording() {
        val qaItem = qaDataManager.getCurrentQaItem()
        if (qaItem != null) {
            recordingFileRepository.deleteRecordingFile(qaItem.category, qaDataManager.getCurrentIndex())
        }
    }
    
    /**
     * 현재 녹음 상태
     */
    fun isRecording(): Boolean = isRecording
    
    /**
     * 현재 재생 상태
     */
    fun isPlaying(): Boolean = isPlaying
} 