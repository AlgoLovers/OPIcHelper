package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.na982.opichelper.domain.entity.StudyDailyRecord
import com.na982.opichelper.domain.manager.AppLogger
import com.na982.opichelper.domain.repository.StudySessionRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StudySessionRepositoryImpl(
    context: Context,
    private val appLogger: AppLogger,
    private val gson: Gson
) : StudySessionRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "study_sessions"
        private const val KEY_DAILY_PREFIX = "daily_"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
    }

    override fun recordSession(durationMs: Long, completedCount: Int) {
        try {
            val today = LocalDate.now().format(DATE_FORMAT)
            val key = KEY_DAILY_PREFIX + today
            val existing = getDailyRecord(today)
            val updated = existing.copy(
                studyDurationMs = existing.studyDurationMs + durationMs,
                completedScripts = existing.completedScripts + completedCount
            )
            prefs.edit().putString(key, gson.toJson(updated)).apply()
            updateStreak()
        } catch (e: Exception) {
            appLogger.e("StudySessionRepo", "세션 기록 실패", e)
        }
    }

    override fun getDailyRecords(days: Int): List<StudyDailyRecord> {
        val records = mutableListOf<StudyDailyRecord>()
        val today = LocalDate.now()
        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            records.add(getDailyRecord(date.format(DATE_FORMAT)))
        }
        return records
    }

    override fun getStreak(): Int {
        var streak = 0
        val today = LocalDate.now()
        for (i in 0L..365) {
            val date = today.minusDays(i)
            val record = getDailyRecord(date.format(DATE_FORMAT))
            if (record.studyDurationMs > 0 || record.completedScripts > 0) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    override fun getLongestStreak(): Int {
        return prefs.getInt(KEY_LONGEST_STREAK, 0)
    }

    override fun getTotalStudyDurationMs(): Long {
        var total = 0L
        for ((key, _) in prefs.all) {
            if (key.startsWith(KEY_DAILY_PREFIX)) {
                try {
                    val record = gson.fromJson(prefs.getString(key, null), StudyDailyRecord::class.java)
                    total += record.studyDurationMs
                } catch (e: Exception) {
                    appLogger.e("StudySessionRepo", "총 학습 시간 계산 중 오류: $key", e)
                }
            }
        }
        return total
    }

    override fun getTotalCompletedScripts(): Int {
        var total = 0
        for ((key, _) in prefs.all) {
            if (key.startsWith(KEY_DAILY_PREFIX)) {
                try {
                    val record = gson.fromJson(prefs.getString(key, null), StudyDailyRecord::class.java)
                    total += record.completedScripts
                } catch (e: Exception) {
                    appLogger.e("StudySessionRepo", "완료 스크립트 계산 중 오류: $key", e)
                }
            }
        }
        return total
    }

    private fun getDailyRecord(date: String): StudyDailyRecord {
        return try {
            val json = prefs.getString(KEY_DAILY_PREFIX + date, null)
            if (json != null) gson.fromJson(json, StudyDailyRecord::class.java) else StudyDailyRecord(date)
        } catch (e: Exception) {
            appLogger.e("StudySessionRepo", "일일 기록 로드 실패: $date", e)
            StudyDailyRecord(date)
        }
    }

    private fun updateStreak() {
        val currentStreak = getStreak()
        val longest = prefs.getInt(KEY_LONGEST_STREAK, 0)
        if (currentStreak > longest) {
            prefs.edit().putInt(KEY_LONGEST_STREAK, currentStreak).apply()
        }
    }
}
