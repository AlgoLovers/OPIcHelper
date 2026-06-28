package com.na982.opichelper.domain.audio

import java.io.File

interface AudioPlayer {
    fun playAudio(filePath: String)
    fun stop()
}
