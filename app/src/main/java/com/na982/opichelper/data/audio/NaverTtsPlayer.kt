package com.na982.opichelper.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 네이버 클로바 TTS 플레이어 (한글 전용)
 * 클린 아키텍처 원칙에 따라 단일 책임을 가짐
 * 
 * ⚠️ 요금 정책: 유료 API
 * - 프리미엄: 월 9만원 (100만 자)
 * - 표준: 월 1만원 (10만 자)
 * - 초과 시 1,000자당 0.1원
 * 
 * 현재 TtsPlayerManager에서 주석 처리되어 사용되지 않음
 * 무료 대안으로 삼성 TTS 사용 권장
 */
class NaverTtsPlayer(private val context: Context) : TtsPlayer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 네이버 클로바 API 설정 (실제 사용 시 API 키 필요)
    private val NAVER_CLIENT_ID = ApiKeys.NAVER_CLIENT_ID
    private val NAVER_CLIENT_SECRET = ApiKeys.NAVER_CLIENT_SECRET
    private val NAVER_TTS_URL = "https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts"
    
    private var mediaPlayer: MediaPlayer? = null
    private var isQuotaExceeded = false
    private var isPlaying = false
    
    override fun isAvailable(): Boolean {
        val available = !isQuotaExceeded && NAVER_CLIENT_ID != "YOUR_NAVER_CLIENT_ID"
        Log.d("NaverTtsPlayer", "🔵 네이버 TTS 사용 가능 여부: $available (API 키 설정: ${NAVER_CLIENT_ID != "YOUR_NAVER_CLIENT_ID"})")
        return available
    }
    
    override fun getServiceName(): String {
        return "네이버 클로바"
    }
    
    override suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        return try {
            Log.d("NaverTtsPlayer", "🔵 네이버 TTS 시작: $text")
            isPlaying = true
            
            val audioData = callNaverTtsApi(text)
            if (audioData != null) {
                val tempFile = File(context.cacheDir, "naver_tts_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioData)
                }
                playAudioFile(tempFile, onComplete)
                true
            } else {
                Log.e("NaverTtsPlayer", "🔵 네이버 TTS API 호출 실패")
                isPlaying = false
                onComplete?.invoke()
                false
            }
        } catch (e: Exception) {
            Log.e("NaverTtsPlayer", "🔵 네이버 TTS 오류", e)
            if (e.message?.contains("quota") == true || e.message?.contains("limit") == true) {
                isQuotaExceeded = true
                Log.w("NaverTtsPlayer", "🔵 네이버 TTS 할당량 초과")
            }
            isPlaying = false
            onComplete?.invoke()
            false
        }
    }
    
    private suspend fun callNaverTtsApi(text: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("text", text)
                    put("speaker", "vhyeri")
                    put("speed", 1.0)
                    put("format", "mp3")
                }
                
                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(NAVER_TTS_URL)
                    .addHeader("X-NCP-APIGW-API-KEY-ID", NAVER_CLIENT_ID)
                    .addHeader("X-NCP-APIGW-API-KEY", NAVER_CLIENT_SECRET)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e("NaverTtsPlayer", "API 호출 실패: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e("NaverTtsPlayer", "API 호출 중 오류", e)
                null
            }
        }
    }
    
    private fun playAudioFile(file: File, onComplete: (() -> Unit)?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    Log.d("NaverTtsPlayer", "네이버 TTS 재생 완료")
                    this@NaverTtsPlayer.isPlaying = false
                    onComplete?.invoke()
                    file.delete()
                }
                setOnErrorListener { _, _, _ ->
                    Log.e("NaverTtsPlayer", "네이버 TTS 재생 오류")
                    this@NaverTtsPlayer.isPlaying = false
                    onComplete?.invoke()
                    file.delete()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("NaverTtsPlayer", "MediaPlayer 오류", e)
            this@NaverTtsPlayer.isPlaying = false
            onComplete?.invoke()
            file.delete()
        }
    }
    
    override fun stop() {
        mediaPlayer?.apply {
            if (this@NaverTtsPlayer.isPlaying()) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlaying = false
        Log.d("NaverTtsPlayer", "네이버 TTS 중지")
    }
    
    override fun isPlaying(): Boolean {
        return isPlaying || (mediaPlayer?.isPlaying == true)
    }
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {
        // 단일 TTS 플레이어는 하이라이트를 지원하지 않음
        speak(text, null)
    }
    
    override suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long {
        val start = System.currentTimeMillis()
        val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
        
        speak(text) {
            finished.complete(Unit)
        }
        
        finished.await()
        return System.currentTimeMillis() - start
    }
} 