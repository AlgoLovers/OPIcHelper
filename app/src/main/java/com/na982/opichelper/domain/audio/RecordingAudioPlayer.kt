package com.na982.opichelper.domain.audio

interface RecordingAudioPlayer {
    fun playRecording(filePath: String, onCompletion: () -> Unit)
    fun stopRecording()
}
