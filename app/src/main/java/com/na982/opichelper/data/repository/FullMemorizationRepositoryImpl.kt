package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import com.na982.opichelper.domain.repository.QaDataRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullMemorizationRepositoryImpl @Inject constructor(
    private val ttsOrchestrator: TtsOrchestrator,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val audioFileManager: AudioFileManager,
    private val qaDataRepository: QaDataRepository
) : FullMemorizationRepository {

    private var currentRecordingPath: String? = null

    override suspend fun playQuestionWithHighlight() {
        try {
            val qaItem = qaDataRepository.getCurrentQaItem()
            if (qaItem != null) {
                // 영어 질문 TTS 재생 (표준화된 방식 사용)
                val questionDuration = ttsOrchestrator.speakAndWaitForCompletion(qaItem.questionEn, isKorean = false, rate = 1.0f)
                Log.d("FullMemorizationRepositoryImpl", "영어 질문 TTS 재생 완료: ${questionDuration}ms")
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationRepositoryImpl", "영어 질문 TTS 재생 실패", e)
        }
    }

    override suspend fun startRecording(category: String, scriptIndex: Int): String {
        try {
            // 녹음 파일 경로 생성
            val timestamp = System.currentTimeMillis()
            val recordingFileName = "통암기_${category}_${scriptIndex}_${timestamp}.m4a"
            currentRecordingPath = audioFileManager.getRecordingFilePath(recordingFileName)
            
            Log.d("FullMemorizationRepositoryImpl", "녹음 시작: $currentRecordingPath")
            
            audioRecorder.startRecording(currentRecordingPath!!)
            
            return currentRecordingPath!!
        } catch (e: Exception) {
            Log.e("FullMemorizationRepositoryImpl", "녹음 시작 실패", e)
            throw e
        }
    }
    
    override suspend fun stopRecording() {
        try {
            if (currentRecordingPath != null) {
                Log.d("FullMemorizationRepositoryImpl", "녹음 종료")
                
                audioRecorder.stopRecording()
                
                Log.d("FullMemorizationRepositoryImpl", "녹음 완료: $currentRecordingPath")
            }
        } catch (e: Exception) {
            Log.e("FullMemorizationRepositoryImpl", "녹음 종료 실패", e)
            throw e
        }
    }
    
    override suspend fun playRecording(
        onHighlight: (Int?) -> Unit
    ) {
        try {
            if (currentRecordingPath == null) {
                Log.e("FullMemorizationRepositoryImpl", "재생할 녹음 파일이 없음")
                return
            }
            
            Log.d("FullMemorizationRepositoryImpl", "녹음 재생 시작: $currentRecordingPath")
            
            // 녹음 재생 시작
            audioPlayer.playAudio(currentRecordingPath!!)
            
            // 녹음 재생 완료까지 대기
            val recordingDuration = audioPlayer.getDuration(currentRecordingPath!!)
            kotlinx.coroutines.delay(recordingDuration.toLong())
            
            // 재생 완료
            onHighlight(null)
            
            Log.d("FullMemorizationRepositoryImpl", "녹음 재생 완료")
            
        } catch (e: Exception) {
            Log.e("FullMemorizationRepositoryImpl", "녹음 재생 실패", e)
            throw e
        }
    }

    override fun hasRecording(): Boolean {
        // 현재 QA 아이템 정보로 실제 파일 존재 여부 확인
        val qaItem = qaDataRepository.getCurrentQaItem()
        if (qaItem == null) {
            Log.d("FullMemorizationRepositoryImpl", "hasRecording: QA 아이템이 null")
            return false
        }
        
        val category = qaItem.category
        val scriptIndex = qaDataRepository.getCurrentIndex()
        
        // recordings 디렉토리에서 해당 패턴의 파일 찾기
        val dummyPath = audioFileManager.getRecordingFilePath("dummy.m4a")
        val recordingsDir = File(dummyPath).parentFile
        val files = recordingsDir?.listFiles()
        
        if (files != null) {
            for (file in files) {
                if (file.name.startsWith("통암기_${category}_${scriptIndex}_") && file.name.endsWith(".m4a")) {
                    Log.d("FullMemorizationRepositoryImpl", "hasRecording: 파일 발견 - ${file.name}")
                    // 발견된 파일 경로를 currentRecordingPath에 설정
                    currentRecordingPath = file.absolutePath
                    return true
                }
            }
        }
        
        Log.d("FullMemorizationRepositoryImpl", "hasRecording: 파일 없음 - category=$category, scriptIndex=$scriptIndex")
        return false
    }

    override fun getRecordingPath(): String? {
        return currentRecordingPath
    }

    override fun clearRecording() {
        currentRecordingPath = null
    }
} 