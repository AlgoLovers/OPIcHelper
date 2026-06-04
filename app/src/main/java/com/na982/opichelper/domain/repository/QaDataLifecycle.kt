package com.na982.opichelper.domain.repository

interface QaDataLifecycle {
    suspend fun init()
    suspend fun reload()
    fun release()
}
