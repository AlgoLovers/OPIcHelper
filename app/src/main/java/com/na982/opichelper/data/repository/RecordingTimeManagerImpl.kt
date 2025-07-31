package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.repository.RecordingTimeManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences를 사용한 녹음 시간 관리 구현체
 */
@Singleton
class RecordingTimeManagerImpl @Inject constructor(
    private val context: Context
) : RecordingTimeManager {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("recording_times", Context.MODE_PRIVATE)
    private val gson = Gson()
    
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
        
        Log.d("RecordingTimeManagerImpl", "녹음 시간 저장: $key, 문장 $sentenceIndex, 시간 ${recordingTimeMs}ms, 전체: $times")
    }
    
    override fun getRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int): Long? {
        val times = getAllRecordingTimes(category, scriptIndex)
        val result = if (sentenceIndex < times.size) times[sentenceIndex] else null
        Log.d("RecordingTimeManagerImpl", "녹음 시간 조회: ${getKey(category, scriptIndex)}, 문장 $sentenceIndex, 결과: $result")
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
                Log.e("RecordingTimeManagerImpl", "JSON 파싱 오류: $json", e)
                emptyList()
            }
        } else {
            emptyList()
        }
        
        Log.d("RecordingTimeManagerImpl", "전체 녹음 시간 조회: $key, 결과: $result")
        return result
    }
    
    override fun hasRecordingTimes(category: String, scriptIndex: Int): Boolean {
        val key = getKey(category, scriptIndex)
        val hasData = prefs.contains(key)
        Log.d("RecordingTimeManagerImpl", "녹음 시간 존재 확인: $key, 결과: $hasData")
        return hasData
    }
    
    override fun clearRecordingTimes(category: String, scriptIndex: Int) {
        val key = getKey(category, scriptIndex)
        prefs.edit().remove(key).apply()
    }
    
    private fun getKey(category: String, scriptIndex: Int): String {
        return "recording_times_${category}_${scriptIndex}"
    }
} 