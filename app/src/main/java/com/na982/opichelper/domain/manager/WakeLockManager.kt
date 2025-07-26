package com.na982.opichelper.domain.manager

import android.content.Context
import android.os.PowerManager
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WakeLock 관리자
 * 앱 실행 중 화면이 꺼지지 않도록 WakeLock을 관리
 */
@Singleton
class WakeLockManager @Inject constructor(
    private val context: Context
) {
    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    /**
     * WakeLock 획득 (화면 켜짐 유지)
     */
    fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) {
            Log.d("WakeLockManager", "WakeLock이 이미 획득되어 있음")
            return
        }
        
        try {
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "OPicHelper:WakeLock"
            )
            wakeLock?.acquire()
            Log.d("WakeLockManager", "WakeLock 획득 완료 - 화면 켜짐 유지")
        } catch (e: Exception) {
            Log.e("WakeLockManager", "WakeLock 획득 실패", e)
        }
    }
    
    /**
     * WakeLock 해제
     */
    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                wakeLock = null
                Log.d("WakeLockManager", "WakeLock 해제 완료")
            } else {
                Log.d("WakeLockManager", "WakeLock이 이미 해제되어 있음")
            }
        } catch (e: Exception) {
            Log.e("WakeLockManager", "WakeLock 해제 실패", e)
        }
    }
    
    /**
     * WakeLock 상태 확인
     */
    fun isWakeLockHeld(): Boolean {
        return wakeLock?.isHeld == true
    }
    
    /**
     * 안전한 해제 (null 체크 포함)
     */
    fun safeRelease() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d("WakeLockManager", "안전한 WakeLock 해제 완료")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("WakeLockManager", "안전한 WakeLock 해제 실패", e)
        }
    }
} 