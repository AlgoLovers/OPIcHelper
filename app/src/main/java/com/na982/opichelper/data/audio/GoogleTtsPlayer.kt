package com.na982.opichelper.data.audio

import android.content.Context
import android.os.Build
import android.util.Log
import java.util.*

/**
 * Google TTS 플레이어 (영문 전용)
 * BaseTtsPlayer를 상속하여 공통 로직 재사용
 * Android 버전별 TTS 성능 최적화 적용
 */
class GoogleTtsPlayer(context: Context) : BaseTtsPlayer(
    context = context,
    locale = Locale.US,
    serviceName = "Google TTS",
    logTag = "GoogleTtsPlayer"
) {
    // Android 버전별 TTS 성능 최적화 설정
    override fun getSpeechRate(): Float {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (S24 등 최신 기기): 영문 적합 속도
                0.8f
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13 (Tab S6 Lite 등): 중간 속도
                0.8f
            }
            else -> {
                // Android 12 이하: 기본 속도
                0.7f
            }
        }
    }
    
    override fun getPitch(): Float {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+: 기본 피치
                1.0f
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13: 기본 피치
                1.0f
            }
            else -> {
                // Android 12 이하: 기본 피치
                1.0f
            }
        }
    }
    
    override suspend fun speakWithHighlight(text: String, onHighlight: (Int?) -> Unit) {
        // 단일 TTS 플레이어는 하이라이트를 지원하지 않음
        speak(text, null)
    }
    
    override suspend fun speakAndGetDuration(text: String, isKorean: Boolean, rate: Float): Long {
        val start = System.currentTimeMillis()
        val finished = kotlinx.coroutines.CompletableDeferred<Unit>()
        
        // Android 버전별 영문 최적화
        val optimizedRate = if (!isKorean) {
            val baseRate = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    // Android 14+: 5% 더 빠르게 (80% → 84%)
                    (rate * 1.05f).coerceAtMost(0.9f)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13: 3% 더 빠르게
                    (rate * 1.03f).coerceAtMost(0.9f)
                }
                else -> {
                    // Android 12 이하: 기본 속도
                    (rate * 1.0f).coerceAtMost(0.8f)
                }
            }
            // 영문 TTS 최적화 속도 적용
            baseRate
        } else {
            rate
        }
        
        // 임시로 속도 설정
        val originalRate = getSpeechRate()
        tts?.setSpeechRate(optimizedRate)
        
        speak(text) {
            // 원래 속도로 복원
            tts?.setSpeechRate(originalRate)
            finished.complete(Unit)
        }
        
        finished.await()
        return System.currentTimeMillis() - start
    }
    
    fun destroy() {
        release()
    }
    
    /**
     * TTS 플레이어 완전 해제 (앱 종료 시 사용)
     */
    override fun release() {
        try {
            // Google TTS 플레이어 완전 해제
            super.release()
        } catch (e: Exception) {
            Log.e(logTag, "Google TTS 플레이어 해제 중 오류", e)
        }
    }
} 