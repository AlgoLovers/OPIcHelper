package com.na982.opichelper.domain.audio

import java.io.File

interface AudioPlayer {
    fun play(file: File, onCompletion: () -> Unit)
    fun playAudio(filePath: String)
    fun stop()
}
