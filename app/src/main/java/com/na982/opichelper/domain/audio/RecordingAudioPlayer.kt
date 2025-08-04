package com.na982.opichelper.domain.audio

/**
 * 녹음 재생 전용 AudioPlayer 인터페이스
 * TTS와 분리된 독립적인 녹음 재생 시스템
 */
interface RecordingAudioPlayer {
    /**
     * 녹음 파일 재생 (하이라이트 포함)
     * @param filePath 재생할 파일 경로
     * @param onHighlight 하이라이트 콜백 (문장 인덱스)
     * @param onCompletion 재생 완료 콜백
     */
    fun playRecording(filePath: String, onHighlight: (Int) -> Unit, onCompletion: () -> Unit)
    
    /**
     * 녹음 파일 재생 (녹음 시간 정보 포함)
     * @param filePath 재생할 파일 경로
     * @param recordingTimes 각 문장별 녹음 시간 리스트 (밀리초)
     * @param onHighlight 하이라이트 콜백 (문장 인덱스)
     * @param onCompletion 재생 완료 콜백
     */
    fun playRecordingWithTimes(filePath: String, recordingTimes: List<Long>, onHighlight: (Int) -> Unit, onCompletion: () -> Unit)
    
    /**
     * 녹음 파일 재생 (기본)
     * @param filePath 재생할 파일 경로
     * @param onCompletion 재생 완료 콜백
     */
    fun playRecording(filePath: String, onCompletion: () -> Unit)
    
    /**
     * 재생 중지
     */
    fun stopRecording()
    
    /**
     * 현재 재생 상태
     */
    val isPlaying: Boolean
    
    /**
     * 파일 재생 시간 가져오기 (밀리초)
     */
    fun getDuration(filePath: String): Int
    
    /**
     * 녹음 재생 시작 (동기적)
     */
    fun startRecordingPlayback(filePath: String)
} 