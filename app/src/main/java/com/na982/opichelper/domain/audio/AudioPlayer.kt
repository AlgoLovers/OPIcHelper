package com.na982.opichelper.domain.audio

import java.io.File
 
interface AudioPlayer {
    fun play(file: File, onCompletion: () -> Unit)
    fun playAudio(filePath: String)
    fun stop()
    fun stopAudio()
    val isPlaying: Boolean
    
    /**
     * 오디오 파일의 재생 시간을 밀리초 단위로 반환
     */
    fun getDuration(filePath: String): Int
} 