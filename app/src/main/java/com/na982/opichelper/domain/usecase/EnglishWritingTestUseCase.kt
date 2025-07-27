package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.repository.AudioFileManager
import com.na982.opichelper.domain.usecase.MemorizeTestProgressTracker
import com.na982.opichelper.domain.repository.RecordingTimeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 영작 테스트용 Service
 * 책임: 영작 테스트 실행, 진행 상황 관리, TTS 제어
 */
@Singleton
class EnglishWritingTestService @Inject constructor(
    private val ttsPlayer: TtsPlayer,
    private val audioRecorder: AudioRecorder,
    private val audioFileManager: AudioFileManager,
    private val progressTracker: MemorizeTestProgressTracker,
    private val recordingTimeManager: RecordingTimeManager
) {
    /**
     * 영작 테스트 실행 (부분암기 테스트)
     * - answerKo: 한글 답변 텍스트
     * - answerEn: 영문 답변 텍스트
     * - onKoreanHighlight: 한글 문장 하이라이트 콜백
     * - onRecordingHighlight: 녹음 하이라이트 콜백
     * - onCardFlip: 카드 뒤집기 콜백 (true: 한글, false: 영문)
     * - onRecordingStateChange: 녹음 상태 변경 콜백
     * - category: 카테고리
     * - scriptIndex: 스크립트 인덱스
     *
     * 플로우: 한글 문장 TTS → 녹음 → 다음 문장 반복 → 파일 합치기
     */
    suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int,
        onCardFlip: (Boolean) -> Unit,
        onKoreanHighlight: (Int?) -> Unit,
        onRecordingHighlight: (Int?) -> Unit,
        onRecordingStateChange: (Boolean) -> Unit,
        onMergedFileCreated: () -> Unit = {} // 병합 파일 생성 완료 콜백 추가
    ) {
        val koSentences = answerKo.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val count = minOf(koSentences.size, enSentences.size)
        
        // 복원된 앱 상태에서 시작 인덱스 가져오기 (암기레벨별)
        val currentProgress = progressTracker.getScriptProgress(category, scriptIndex, "영작 테스트")
        
        val startIndex = if (currentProgress?.isMemorizeTestRunning == true) {
            currentProgress.currentSentenceIndex
        } else {
            0
        }
        
        Log.d("EnglishWritingTestService", "영작 테스트 시작: 총 $count 문장, 시작 인덱스: $startIndex")
        
        val recordingFiles = mutableListOf<File>()
        
        for (idx in startIndex until count) {
            // 코루틴이 취소되었는지 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("EnglishWritingTestService", "코루틴이 취소됨 - Service 중단")
                break
            }
            
            Log.d("EnglishWritingTestService", "문장 ${idx + 1} 처리 시작 (인덱스: $idx)")
            
            // 진행 상황 업데이트 및 실시간 저장
            progressTracker.updateProgress(
                category = category,
                scriptIndex = scriptIndex,
                memorizeLevel = "영작 테스트",
                currentSentenceIndex = idx,
                totalSentences = count,
                isMemorizeTestRunning = true
            )
            // 실시간으로 진행상황 저장
            progressTracker.persistChangedProgress()
            Log.d("EnglishWritingTestService", "문장 $idx 진행상황 실시간 저장 완료")
            
            // 1. 한글 문장 TTS (카드를 한글로 뒤집고 하이라이트)
            onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            onKoreanHighlight(idx) // 한글 하이라이트
            
            // 한글 문장 TTS 재생
            ttsPlayer.speakAndGetDuration(koSentences[idx], isKorean = true, rate = 0.8f)
            // delay((koDuration * 0.5).toLong()) // 쉬는 시간 제거

            // 코루틴이 취소되었는지 다시 확인
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) {
                Log.d("EnglishWritingTestService", "코루틴이 취소됨 - Service 중단")
                break
            }

            // 2. 녹음 시작 (마이크 2차 하이라이트) - 한글 하이라이트는 유지
            onRecordingHighlight(idx) // 녹음 하이라이트 추가 (한글 하이라이트와 함께)
            onRecordingStateChange(true) // 녹음 상태 활성화
            
            // 영문 문장 길이에 비례한 녹음 시간 계산
            val recordingDuration = (enSentences[idx].length * 100L).coerceAtLeast(3000L) // 최소 3초
            
            // 녹음 시작
            val recordingFile = audioRecorder.startRecording()
            val startTime = System.currentTimeMillis()
            delay(recordingDuration)
            val endTime = System.currentTimeMillis()
            val actualRecordingTime = endTime - startTime
            audioRecorder.stopRecording()
            
            // 실제 녹음 시간 저장
            recordingTimeManager.saveRecordingTime(category, scriptIndex, idx, actualRecordingTime)
            Log.d("EnglishWritingTestService", "문장 $idx 실제 녹음 시간: ${actualRecordingTime}ms, 키: ${category}_${scriptIndex}")
            
            // 저장 확인
            val savedTime = recordingTimeManager.getRecordingTime(category, scriptIndex, idx)
            Log.d("EnglishWritingTestService", "문장 $idx 저장 확인: 저장된 시간=${savedTime}ms")
            
            // 녹음 파일 저장
            val savedFile = audioFileManager.saveRecordingFile(recordingFile, "english_writing_${category}_${scriptIndex}_${idx}")
            recordingFiles.add(savedFile)
            
            onRecordingStateChange(false) // 녹음 상태 비활성화
            onRecordingHighlight(null) // 녹음 하이라이트 제거
            onKoreanHighlight(null) // 한글 하이라이트 제거
        }
        
        // 마지막에 카드를 원래 상태(영문)로 복원
        onCardFlip(false)
        onKoreanHighlight(null)
        onRecordingHighlight(null)
        
        // 3. 모든 녹음 파일을 하나로 합치기
        if (recordingFiles.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val mergedFileName = "영작테스트_${category}_${scriptIndex}_${timestamp}"
            
            val mergedFile = audioFileManager.mergeAudioFiles(recordingFiles, mergedFileName)
            
            // 개별 녹음 파일들 삭제
            recordingFiles.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            
            Log.d("EnglishWritingTestService", "영작 테스트 완료 - 머지된 파일: ${mergedFile.absolutePath}")
            
            // 병합 파일 생성 완료 콜백 호출
            onMergedFileCreated()
        }
        
        // 테스트 완료 - 현재 스크립트 진행 상황 삭제 (암기레벨별)
        progressTracker.clearScriptProgress(category, scriptIndex, "영작 테스트")
        
        Log.d("EnglishWritingTestService", "영작 테스트 완료")
    }
} 