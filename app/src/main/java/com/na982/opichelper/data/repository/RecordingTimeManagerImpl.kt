package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.manager.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.repository.RecordingTimeManager

/**
 * SharedPreferences를 사용한 녹음 시간 관리 구현체
 */
class RecordingTimeManagerImpl(
    private val context: Context,
    private val appLogger: AppLogger
) : RecordingTimeManager {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    @Synchronized
    override fun saveRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int, recordingTimeMs: Long) {
        val key = getKey(category, scriptIndex)
        val times = getAllRecordingTimes(category, scriptIndex).toMutableList()
        
        // 리스트 크기를 sentenceIndex + 1로 확장
        while (times.size <= sentenceIndex) {
            times.add(0L)
        }
        
        times[sentenceIndex] = recordingTimeMs
        
        val json = gson.toJson(times)
        prefs.edit().putString(key, json).apply()
        
        // 녹음 시간 저장 완료
    }
    
    override fun getRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int): Long? {
        val times = getAllRecordingTimes(category, scriptIndex)
        val result = if (sentenceIndex < times.size) times[sentenceIndex] else null
        // 녹음 시간 조회 완료
        return result
    }
    
    override fun getAllRecordingTimes(category: String, scriptIndex: Int): List<Long> {
        val key = getKey(category, scriptIndex)
        val json = prefs.getString(key, null)
        
        val result = if (json != null) {
            try {
                val type = object : TypeToken<List<Long>>() {}.type
                gson.fromJson<List<Long>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                appLogger.e("RecordingTimeManagerImpl", "JSON 파싱 오류: $json", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // 전체 녹음 시간 조회 완료
        return result
    }
    
    override fun hasRecordingTimes(category: String, scriptIndex: Int): Boolean {
        val key = getKey(category, scriptIndex)
        val hasData = prefs.contains(key)
        // 녹음 시간 존재 확인 완료
        return hasData
    }
    
    override fun clearRecordingTimes(category: String, scriptIndex: Int) {
        val key = getKey(category, scriptIndex)
        prefs.edit().remove(key).apply()
    }
    
    private fun getKey(category: String, scriptIndex: Int): String {
        return "${KEY_RECORDING_TIMES_PREFIX}${category}_${scriptIndex}"
    }

    companion object {
        private const val PREFS_NAME = "recording_times"
        private const val KEY_RECORDING_TIMES_PREFIX = "recording_times_"
    }
} 