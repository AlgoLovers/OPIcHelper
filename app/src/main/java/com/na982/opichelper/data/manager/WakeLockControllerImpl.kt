package com.na982.opichelper.data.manager

import android.content.Context
import android.os.PowerManager
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.manager.WakeLockController

class WakeLockControllerImpl(
    context: Context,
    private val appLogger: AppLogger
) : WakeLockController {
    private val appContext = context.applicationContext
    @Volatile
    private var wakeLock: PowerManager.WakeLock? = null
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    companion object {
        private const val TAG = "WakeLockController"
        private const val WAKELOCK_TIMEOUT_MS = 30 * 60 * 1000L
    }

    override fun acquire() {
        if (wakeLock?.isHeld == true) {
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
        } catch (e: Exception) {
            appLogger.e(TAG, "WakeLock 획득 실패", e)
        }
    }

    override fun release() {
        try {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            appLogger.e(TAG, "WakeLock 해제 실패", e)
            wakeLock = null
        }
    }

    override fun isHeld(): Boolean = wakeLock?.isHeld == true
}
