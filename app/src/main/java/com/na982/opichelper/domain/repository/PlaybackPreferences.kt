package com.na982.opichelper.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PlaybackPreferences {
    fun getRepeatListeningCount(): Int
    fun setRepeatListeningCount(count: Int)
    val repeatListeningCount: StateFlow<Int>

    fun getAnswerPlayCount(): Int
    fun setAnswerPlayCount(count: Int)
    val answerPlayCount: StateFlow<Int>

    fun isAutoAdvance(): Boolean
    fun setAutoAdvance(enabled: Boolean)
}
