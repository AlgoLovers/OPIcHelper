package com.na982.opichelper.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.na982.opichelper.domain.audio.AudioRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderImpl(private val context: Context) : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun startRecording(): File {
        return startRecording("recording_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()))
    }
    
    override fun startRecording(scriptId: String): File {
        // 권한 확인
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("오디오 녹음 권한이 필요합니다.")
        }
        
        // scriptId가 이미 절대 경로인지 확인
        val outputFile = if (scriptId.startsWith("/")) {
            // 이미 절대 경로인 경우
            File(scriptId)
        } else {
            // 상대 경로인 경우
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) recordingsDir.mkdirs()
            val fileName = "${scriptId}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a"
            File(recordingsDir, fileName)
        }
        
        this.outputFile = outputFile
        
        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            throw RuntimeException("녹음을 시작할 수 없습니다: ${e.message}", e)
        }
        
        return outputFile
    }

    override fun stopRecording(): File? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        }
        val file = outputFile
        recorder = null
        outputFile = null
        return file
    }
} 