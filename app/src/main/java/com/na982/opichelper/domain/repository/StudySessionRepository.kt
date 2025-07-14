package com.na982.opichelper.domain.repository

import com.na982.opichelper.domain.entity.StudySession
import kotlinx.coroutines.flow.Flow

interface StudySessionRepository {
    suspend fun createSession(session: StudySession): String
    suspend fun updateSession(session: StudySession)
    suspend fun deleteSession(sessionId: String)
    suspend fun getSession(sessionId: String): StudySession?
    fun getAllSessions(): Flow<List<StudySession>>
    fun getCompletedSessions(): Flow<List<StudySession>>
    fun getActiveSessions(): Flow<List<StudySession>>
} 