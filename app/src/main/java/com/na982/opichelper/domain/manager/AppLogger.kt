package com.na982.opichelper.domain.manager

interface AppLogger {
    fun e(tag: String, msg: String, t: Throwable? = null)
    fun w(tag: String, msg: String)
}
