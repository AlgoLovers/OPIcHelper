package com.na982.opichelper.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.na982.opichelper.domain.audio.TtsPlayer
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 카카오 음성 API TTS 플레이어 (한글 전용)
 * 클린 아키텍처 원칙에 따라 단일 책임을 가짐
 * 
 * ⚠️ 요금 정책: 유료 API (정확한 요금 불명)
 * - 카카오 공식 쿼터 문서에 음성 API 정보 없음
 * - 별도 유료 서비스일 가능성 높음
 * - 요금 정책 확인 필요
 * 
 * 현재 TtsPlayerManager에서 주석 처리되어 사용되지 않음
 * 무료 대안으로 삼성 TTS 사용 권장
 */
class KakaoTtsPlayer(private val context: Context) : TtsPlayer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 카카오 API 설정
    private val KAKAO_API_KEY = ApiKeys.KAKAO_API_KEY
    private val KAKAO_TTS_URL = "https://kakaoi-newtone-openapi.kakao.com/v1/synthesize"
    
    private var mediaPlayer: MediaPlayer? = null
    private var isQuotaExceeded = false
    private var isPlaying = false
    
    override fun isAvailable(): Boolean {
        val available = !isQuotaExceeded && KAKAO_API_KEY != "YOUR_KAKAO_API_KEY"
        Log.d("KakaoTtsPlayer", "🟡 카카오 TTS 사용 가능 여부: $available (API 키 설정: ${KAKAO_API_KEY != "YOUR_KAKAO_API_KEY"})")
        return available
    }
    
    override fun getServiceName(): String {
        return "카카오 음성"
    }
    
    override suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
        return try {
            Log.d("KakaoTtsPlayer", "🟡 카카오 TTS 시작: $text")
            isPlaying = true
            
            val audioData = callKakaoTtsApi(text)
            if (audioData != null) {
                val tempFile = File(context.cacheDir, "kakao_tts_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioData)
                }
                playAudioFile(tempFile, onComplete)
                true
            } else {
                Log.e("KakaoTtsPlayer", "🟡 카카오 TTS API 호출 실패")
                isPlaying = false
                onComplete?.invoke()
                false
            }
        } catch (e: Exception) {
            Log.e("KakaoTtsPlayer", "🟡 카카오 TTS 오류", e)
            if (e.message?.contains("quota") == true || e.message?.contains("limit") == true) {
                isQuotaExceeded = true
                Log.w("KakaoTtsPlayer", "🟡 카카오 TTS 할당량 초과")
            }
            isPlaying = false
            onComplete?.invoke()
            false
        }
    }
    
    private suspend fun callKakaoTtsApi(text: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = text.toRequestBody("text/plain".toMediaType())
                
                val request = Request.Builder()
                    .url(KAKAO_TTS_URL)
                    .addHeader("Authorization", "KakaoAK $KAKAO_API_KEY")
                    .addHeader("Content-Type", "text/plain")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e("KakaoTtsPlayer", "API 호출 실패: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e("KakaoTtsPlayer", "API 호출 중 오류", e)
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
                    Log.d("KakaoTtsPlayer", "카카오 TTS 재생 완료")
                    this@KakaoTtsPlayer.isPlaying = false
                    onComplete?.invoke()
                    file.delete()
                }
                setOnErrorListener { _, _, _ ->
                    Log.e("KakaoTtsPlayer", "카카오 TTS 재생 오류")
                    this@KakaoTtsPlayer.isPlaying = false
                    onComplete?.invoke()
                    file.delete()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("KakaoTtsPlayer", "MediaPlayer 오류", e)
            this@KakaoTtsPlayer.isPlaying = false
            onComplete?.invoke()
            file.delete()
        }
    }
    
    override fun stop() {
        mediaPlayer?.apply {
            if (this@KakaoTtsPlayer.isPlaying()) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlaying = false
        Log.d("KakaoTtsPlayer", "카카오 TTS 중지")
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