package com.na982.opichelper.domain.repository

interface AppDataPreferences {
    fun getSeedVersion(): Int
    fun setSeedVersion(version: Int)
}
