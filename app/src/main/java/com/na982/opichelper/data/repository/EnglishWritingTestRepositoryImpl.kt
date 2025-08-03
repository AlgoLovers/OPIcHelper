package com.na982.opichelper.data.repository

import android.util.Log
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.ProgressData
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager

import com.na982.opichelper.domain.state.MemorizationProgressTracker
import com.na982.opichelper.domain.util.CoroutineUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.na982.opichelper.domain.audio.EnglishWritingUiCallback

/**
 * 영작 테스트 Repository 구현체
 * 
 * 클린 아키텍처 원칙:
 * - Data Layer에서 Repository 인터페이스 구현
 * - Infrastructure Layer에 의존
 * - 실제 비즈니스 로직 처리
 */
@Singleton
class EnglishWritingTestRepositoryImpl @Inject constructor(
    private val qaDataRepository: QaDataRepository,
    private val ttsController: TtsController,
    private val audioRecorder: AudioRecorder,
    private val audioFileManager: AudioFileManager,
    private val recordingTimeManager: RecordingTimeManager,
    private val progressTracker: MemorizationProgressTracker
) : EnglishWritingTestRepository {
    
    companion object {
        private const val TAG = "EnglishWritingTest"
        private const val DEBUG_DETAILED = false // 상세 로그 비활성화
    }
    
    override suspend fun executeEnglishWritingTest(
        answerKo: String,
        answerEn: String,
        category: String,
        scriptIndex: Int,
        uiCallback: EnglishWritingUiCallback
    ) {
        Log.i(TAG, "영작 테스트 시작: $category/$scriptIndex")
        
        // 문장 분리
        val koSentences = answerKo.split(".").filter { it.trim().isNotEmpty() }
        val enSentences = answerEn.split(".").filter { it.trim().isNotEmpty() }
        
        val count = minOf(koSentences.size, enSentences.size)
        if (count == 0) {
            Log.w(TAG, "문장이 없음")
            return
        }
        
        // 현재 진행 상황 확인
        val currentProgress = progressTracker.getScriptProgress(category, scriptIndex, "영작 테스트")
        val startIndex = currentProgress?.currentSentenceIndex ?: 0
        
        Log.i(TAG, "영작 테스트 진행: 총 $count 문장, 시작 인덱스: $startIndex")
        
        val recordingFiles = mutableListOf<File>()
        
        for (idx in startIndex until count) {
            // 안전한 코루틴 취소 확인
            if (!CoroutineUtils.checkCancellation(TAG, "문장 처리")) {
                break
            }
            
            if (DEBUG_DETAILED) {
                Log.d(TAG, "문장 ${idx + 1} 처리 시작 (인덱스: $idx)")
            }
            
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
            if (DEBUG_DETAILED) {
                Log.d(TAG, "문장 $idx 진행상황 실시간 저장 완료")
            }
            
            // 1. 한글 문장 TTS (카드를 한글로 뒤집고 하이라이트)
            uiCallback.onCardFlip(true) // 카드를 한글로 뒤집기
            delay(100) // 카드 뒤집기 애니메이션 대기
            
            // 한글 문장 TTS 재생 (하이라이트 포함)
            ttsController.playSentenceWithHighlight(
                text = koSentences[idx],
                isKorean = true,
                onHighlight = { index ->
                    // TtsController에서 받은 index를 사용하여 하이라이트 설정
                    if (DEBUG_DETAILED) {
                        Log.d(TAG, "한글 문장 $idx 하이라이트 설정: index=$index")
                    }
                    uiCallback.onKoreanHighlight(idx)
                }
            )
            
            // 안전한 코루틴 취소 확인 (TTS 재생 후)
            if (!CoroutineUtils.checkCancellation(TAG, "TTS 재생 후")) {
                break
            }

            // 2. 녹음 시작 (마이크 2차 하이라이트) - 한글 하이라이트는 유지
            uiCallback.onRecordingHighlight(idx) // 녹음 하이라이트 추가 (한글 하이라이트와 함께)
            uiCallback.onRecordingStateChange(true) // 녹음 상태 활성화
            
            // 1순위: 저장된 TTS 시간 확인 (반복듣기에서 저장한 영문 TTS 시간)
            val savedTtsTime = recordingTimeManager.getRecordingTime(category, scriptIndex, idx)
            
            // 2순위: 폴백 - 문장 길이 기반 예측 계산
            val recordingDuration = if (savedTtsTime != null && savedTtsTime > 0) {
                if (DEBUG_DETAILED) {
                    Log.d(TAG, "문장 $idx 저장된 TTS 시간 사용: ${savedTtsTime}ms")
                }
                savedTtsTime
            } else {
                val enSentence = enSentences[idx]
                val enWordCount = enSentence.split("\\s+".toRegex()).size
                
                // 단어 수 기반 적응형 딜레이 계산
                val baseDelay = enWordCount * 500 // 기본 딜레이
                val lengthMultiplier = when {
                    enWordCount <= 5 -> 1.5f    // 짧은 문장: 1.5배
                    enWordCount <= 10 -> 1.2f   // 중간 문장: 1.2배
                    enWordCount <= 15 -> 1.0f   // 긴 문장: 1.0배
                    else -> 0.8f                // 매우 긴 문장: 0.8배
                }
                val calculatedDelay = (baseDelay * lengthMultiplier).toLong()
                
                if (DEBUG_DETAILED) {
                    Log.d(TAG, "문장 $idx 예측 계산 사용: 영문 단어 수=$enWordCount, 기본 딜레이=${baseDelay}ms, 최종 딜레이=${calculatedDelay}ms")
                }
                calculatedDelay
            }
            
            // 녹음 시작
            val recordingFile = audioRecorder.startRecording()
            val startTime = System.currentTimeMillis()
            delay(recordingDuration)
            val endTime = System.currentTimeMillis()
            val actualRecordingTime = endTime - startTime
            audioRecorder.stopRecording()
            
            // 실제 녹음 시간 저장
            recordingTimeManager.saveRecordingTime(category, scriptIndex, idx, actualRecordingTime)
            if (DEBUG_DETAILED) {
                Log.d(TAG, "문장 $idx 실제 녹음 시간: ${actualRecordingTime}ms")
            }
            
            // 저장 확인
            val savedTime = recordingTimeManager.getRecordingTime(category, scriptIndex, idx)
            if (DEBUG_DETAILED) {
                Log.d(TAG, "문장 $idx 저장 확인: 저장된 시간=${savedTime}ms")
            }
            
            // 녹음 파일 저장
            val savedFile = audioFileManager.saveRecordingFile(recordingFile, "english_writing_${category}_${scriptIndex}_${idx}")
            recordingFiles.add(savedFile)
            
            uiCallback.onRecordingStateChange(false) // 녹음 상태 비활성화
        }
        
        // 마지막에 카드를 원래 상태(영문)로 복원
        uiCallback.onCardFlip(false)
        
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
            
            Log.i(TAG, "영작 테스트 완료 - 머지된 파일: ${mergedFile.name}")
            
            // 병합 파일 생성 완료 콜백 호출
            uiCallback.onMergedFileCreated()
        }
        
        // 테스트 완료 - 현재 스크립트 진행 상황 삭제 (암기레벨별)
        progressTracker.clearScriptProgress(category, scriptIndex, "영작 테스트")
        
        // 영작테스트 완료 상태 업데이트
        progressTracker.updateProgress(
            category = category,
            scriptIndex = scriptIndex,
            memorizeLevel = "영작 테스트",
            currentSentenceIndex = count - 1, // 마지막 문장 인덱스
            totalSentences = count,
            isMemorizeTestRunning = false // 테스트 완료로 상태 변경
        )
        
        Log.i(TAG, "영작 테스트 완료")
    }
    
    override suspend fun getCurrentProgress(category: String, scriptIndex: Int): ProgressData? {
        val progress = progressTracker.getScriptProgress(category, scriptIndex, "영작 테스트")
        return progress?.let {
            ProgressData(
                category = it.category,
                scriptIndex = it.scriptIndex,
                memorizeLevel = it.memorizeLevel,
                currentSentenceIndex = it.currentSentenceIndex,
                totalSentences = it.totalSentences,
                isMemorizeTestRunning = it.isMemorizeTestRunning
            )
        }
    }
    
    override suspend fun updateProgress(progressData: ProgressData) {
        progressTracker.updateProgress(
            category = progressData.category,
            scriptIndex = progressData.scriptIndex,
            memorizeLevel = progressData.memorizeLevel,
            currentSentenceIndex = progressData.currentSentenceIndex,
            totalSentences = progressData.totalSentences,
            isMemorizeTestRunning = progressData.isMemorizeTestRunning
        )
    }
    
    override suspend fun clearProgress(category: String, scriptIndex: Int) {
        progressTracker.clearScriptProgress(category, scriptIndex, "영작 테스트")
        if (DEBUG_DETAILED) {
            Log.d(TAG, "영작 테스트 진행상황 삭제: $category/$scriptIndex")
        }
    }
} 