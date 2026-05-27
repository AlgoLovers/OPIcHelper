package com.na982.opichelper.data.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.na982.opichelper.domain.audio.AudioRecorder

class AudioRecorderImpl(private val context: Context) : AudioRecorder {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    @Volatile private var recorder: MediaRecorder? = null
    @Volatile private var outputFile: File? = null

    @Synchronized
    override fun startRecording(): File {
        return startRecording("recording_" + dateFormat.format(Date()))
    }

    @Synchronized
    override fun startRecording(scriptId: String): File {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("오디오 녹음 권한이 필요합니다.")
        }

        val outputFile = if (scriptId.startsWith("/")) {
            File(scriptId)
        } else {
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) recordingsDir.mkdirs()
            val fileName = "${scriptId}_${dateFormat.format(Date())}.m4a"
            File(recordingsDir, fileName)
        }

        this.outputFile = outputFile

        try {
            recorder = createMediaRecorder().apply {
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
            this.outputFile = null
            throw RuntimeException("녹음을 시작할 수 없습니다: ${e.message}", e)
        }

        return outputFile
    }

    @Synchronized
    override fun stopRecording(): File? {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w("AudioRecorderImpl", "MediaRecorder.stop() 실패", e)
        }
        try {
            recorder?.release()
        } catch (e: Exception) {
            Log.w("AudioRecorderImpl", "MediaRecorder.release() 실패", e)
        }
        val file = outputFile
        recorder = null
        outputFile = null
        return file
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
} 