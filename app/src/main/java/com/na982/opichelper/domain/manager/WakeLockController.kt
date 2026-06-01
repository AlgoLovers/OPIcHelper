package com.na982.opichelper.domain.manager

interface WakeLockController {
    fun acquire()
    fun release()
    fun isHeld(): Boolean
}
