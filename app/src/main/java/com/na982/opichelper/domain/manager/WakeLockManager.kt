package com.na982.opichelper.domain.manager

import android.content.Context
import android.os.PowerManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WakeLock 관리자
 * 앱 실행 중 화면이 꺼지지 않도록 WakeLock을 관리
 * 안전장치로 30분 타임아웃 적용
 */
@Singleton
class WakeLockManager @Inject constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    companion object {
        private const val TAG = "WakeLockManager"
        private const val WAKELOCK_TIMEOUT_MS = 30 * 60 * 1000L // 30분 안전 타임아웃
    }

    fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) {
            Log.d(TAG, "WakeLock이 이미 획득되어 있음")
            return
        }

        try {
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "OPicHelper:WakeLock"
            ).apply {
                acquire(WAKELOCK_TIMEOUT_MS)
            }
            Log.d(TAG, "WakeLock 획득 완료 (타임아웃: ${WAKELOCK_TIMEOUT_MS}ms)")
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock 획득 실패", e)
        }
    }

    fun releaseWakeLock() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d(TAG, "WakeLock 해제 완료")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock 해제 실패", e)
            wakeLock = null
        }
    }

    fun isWakeLockHeld(): Boolean = wakeLock?.isHeld == true
}
