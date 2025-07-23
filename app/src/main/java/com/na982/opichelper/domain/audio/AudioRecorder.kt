package com.na982.opichelper.domain.audio

import java.io.File
 
interface AudioRecorder {
    fun startRecording(): File
    fun startRecording(scriptId: String): File
    fun stopRecording(): File?
} 