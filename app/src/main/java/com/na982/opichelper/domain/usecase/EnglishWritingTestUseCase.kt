package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.TtsPlayer
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
 *
 * execute()는 실제 영작 평가 로직 담당
 */
class EnglishWritingTestUseCase(
    private val answerEn: String,
    private val answerKo: String? = null,
    private val ttsPlayer: TtsPlayer,
    private val audioRecorder: AudioRecorder,
    private val audioFileRepository: AudioFileRepository,
    private val onAutoFlip: (() -> Unit)? = null,
    private val onMergedFileCreated: ((File) -> Unit)? = null // 병합된 파일 콜백 추가
) : MemorizeTestUseCase {
    override suspend fun execute() {
        Log.d("EnglishWritingTestUseCase", "execute() 진입")
        // 1. 문장별로 분리 (영문 기준)
        val enSentences = answerEn.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val koSentences = answerKo?.split(Regex("(?<=[.!?])\\s+")).orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
        val recordedFiles = mutableListOf<File>()
        
        for ((idx, enSentence) in enSentences.withIndex()) {
            Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 처리 시작: '${enSentence.take(50)}...'")
            
            // 1. (옵션) 한글 문장 읽기 (answerKo가 있으면)
            if (koSentences.size > idx) {
                Log.d("EnglishWritingTestUseCase", "한글 문장 TTS 실행: '${koSentences[idx].take(30)}...'")
                ttsPlayer.speakAndGetDuration(koSentences[idx], isKorean = true)
            }
            
            // 2. 영문 문장 TTS 시간만 추정 (실제 재생 X) - 시간을 더 길게 설정
            val enDuration = (enSentence.length * 120L).coerceAtLeast(2000L) // 120ms per char, 최소 2000ms
            val recordDuration = (enDuration).toLong()
            Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 시간 계산: 길이=${enSentence.length}, 계산된 시간=${enDuration}ms, 최종 시간=${recordDuration}ms")
            
            // 3. 녹음 시작
            val recordedFile = withContext(Dispatchers.IO) {
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 시작")
                val startTime = System.currentTimeMillis()
                audioRecorder.startRecording()
                kotlinx.coroutines.delay(recordDuration)
                val endTime = System.currentTimeMillis()
                val actualDuration = endTime - startTime
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 완료: 예상 시간=${recordDuration}ms, 실제 시간=${actualDuration}ms")
                audioRecorder.stopRecording()
            }
            recordedFile?.let { 
                recordedFiles.add(it)
                Log.d("EnglishWritingTestUseCase", "문장 ${idx + 1} 녹음 파일 추가: ${it.absolutePath}, 크기: ${it.length()} bytes")
            }
            
            // 4. (옵션) 자동 플립 콜백 (문장별로 필요시)
            onAutoFlip?.invoke()
        }
        
        // 5. Repository를 통해 파일 병합 및 저장
        val mergedFile = audioFileRepository.mergeAndSaveAudioFiles(recordedFiles)
        Log.d("EnglishWritingTestUseCase", "병합된 파일: ${mergedFile?.absolutePath}")
        
        // 6. 병합된 파일 콜백 호출
        mergedFile?.let { file ->
            onMergedFileCreated?.invoke(file)
        }
        
        // 7. (옵션) 평가/피드백 로직 추가 가능
        Log.d("EnglishWritingTestUseCase", "execute() 완료")
    }
} 