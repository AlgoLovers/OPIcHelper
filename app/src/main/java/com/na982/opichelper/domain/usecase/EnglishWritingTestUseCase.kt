package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.usecase.MemorizeTestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log
import com.na982.opichelper.domain.repository.AudioFileRepository

/**
 * 영작 테스트(암기 레벨) 테스트용 UseCase
 * - answerText: 정답 텍스트 (ViewModel에서 주입)
 * - ttsPlayer: TTS 재생기
 * - audioRecorder: 오디오 녹음기
 * - audioFileRepository: 오디오 파일 관리
 * - onAutoFlip: 자동 플립 콜백 (예: 답변 카드 자동 뒤집기)
 * - onMergedFileCreated: 병합된 파일 생성 콜백
 * - memorizeTestState: 암기 테스트 상태 관리
 * - category: 카테고리
 * - qaItemId: QA 아이템 ID
 *
 * execute()는 실제 영작 평가 로직 담당
 */
class EnglishWritingTestUseCase(
    private val answerEn: String,
    private val answerKo: String? = null,
    private val scriptId: String, // 스크립트 ID 추가
    private val ttsPlayer: TtsPlayer,
    private val audioRecorder: AudioRecorder,
    private val audioFileRepository: AudioFileRepository,
    private val memorizeTestState: MemorizeTestState,
    private val category: String,
    private val qaItemId: String,
    private val onAutoFlip: (() -> Unit)? = null,
    private val onKoreanHighlight: ((Int?) -> Unit)? = null, // 한글 하이라이트 콜백 추가
    private val onRecordingHighlight: ((Int?) -> Unit)? = null, // 녹음 하이라이트 콜백 추가
    private val onMergedFileCreated: ((File) -> Unit)? = null // 병합된 파일 콜백 추가
) : MemorizeTestUseCase {
    override suspend fun execute() {
        Log.d("EnglishWritingTestUseCase", "execute() 진입")
        // 1. 문장별로 분리 (영문 기준)
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val koSentences = answerKo?.split(Regex("(?<=[.!?])\\s+")).orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
        val recordedFiles = mutableListOf<File>()
        
        // 진행 상태에서 시작 인덱스 복원
        val currentProgress = memorizeTestState.getCurrentProgress()
        val startIndex = currentProgress?.currentSentenceIndex ?: 0
        
        Log.d("EnglishWritingTestUseCase", "영작 테스트 시작: 총 ${enSentences.size} 문장, 시작 인덱스: $startIndex")
        
        for ((idx, enSentence) in enSentences.withIndex()) {
            // 시작 인덱스 이전 문장은 건너뛰기
            if (idx < startIndex) {
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 건너뛰기 (이미 완료됨)")
                continue
            }
            
            Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 처리 시작: '${enSentence.take(50)}...'")
            
            // 진행 상태 업데이트
            memorizeTestState.updateProgress(idx)
            
            // 1. (옵션) 한글 문장 읽기 (answerKo가 있으면)
            if (koSentences.size > idx) {
                Log.d("EnglishWritingTestUseCase", "한글 문장 TTS 실행: '${koSentences[idx].take(30)}...'")
                // 자동 뒤집기 콜백 호출 (한글 페이지로)
                onAutoFlip?.invoke()
                // 한글 하이라이트 설정 (TTS 재생 중)
                onKoreanHighlight?.invoke(idx)
                // 한글 TTS 재생 (한글 TTS 엔진 사용)
                val koreanDuration = ttsPlayer.speakAndGetDuration(koSentences[idx], isKorean = true, rate = 0.8f)
                Log.d("EnglishWritingTestUseCase", "한글 TTS 재생 완료: ${koSentences[idx].take(30)}..., 시간=${koreanDuration}ms")
                // TTS 재생 완료 후 하이라이트 초기화
                onKoreanHighlight?.invoke(null)
            }
            
            // 2. 영문 문장 TTS 시간만 추정 (실제 재생 X) - 시간을 더 길게 설정
            val enDuration = (enSentence.length * 120L).coerceAtLeast(2000L) // 120ms per char, 최소 2000ms
            val recordDuration = (enDuration).toLong()
            Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 시간 계산: 길이=${enSentence.length}, 계산된 시간=${enDuration}ms, 최종 시간=${recordDuration}ms")
            
            // 3. 녹음 시작 (녹음 하이라이트 표시)
            val recordedFile = withContext(Dispatchers.IO) {
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 시작")
                // 녹음 하이라이트 설정 (녹음 중임을 표시)
                onRecordingHighlight?.invoke(idx)
                val startTime = System.currentTimeMillis()
                audioRecorder.startRecording("${scriptId}_${idx + 1}") // 스크립트 ID와 문장 번호 사용
                kotlinx.coroutines.delay(recordDuration)
                val endTime = System.currentTimeMillis()
                val actualDuration = endTime - startTime
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 완료: 예상 시간=${recordDuration}ms, 실제 시간=${actualDuration}ms")
                val file = audioRecorder.stopRecording()
                // 녹음 완료 후 하이라이트 초기화
                onRecordingHighlight?.invoke(null)
                file
            }
            recordedFile?.let { 
                recordedFiles.add(it)
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 파일 추가: ${it.absolutePath}, 크기: ${it.length()} bytes")
            }
            
            // 4. (옵션) 자동 플립 콜백 (문장별로 필요시)
            onAutoFlip?.invoke()
        }
        
        // 5. Repository를 통해 파일 병합 및 저장
        val mergedFile = audioFileRepository.mergeAndSaveAudioFiles(recordedFiles, scriptId)
        Log.d("EnglishWritingTestUseCase", "병합된 파일: ${mergedFile?.absolutePath}")
        
        // 6. 병합된 파일 콜백 호출
        mergedFile?.let { file ->
            onMergedFileCreated?.invoke(file)
        }
        
        // 7. 테스트 완료 - 진행 상태 삭제
        memorizeTestState.completeProgress()
        
        // 8. (옵션) 평가/피드백 로직 추가 가능
        Log.d("EnglishWritingTestUseCase", "execute() 완료")
    }
} 