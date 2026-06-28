package com.na982.opichelper.domain.repository

interface DataSeeder {
    suspend fun seedIfNeeded()
}
