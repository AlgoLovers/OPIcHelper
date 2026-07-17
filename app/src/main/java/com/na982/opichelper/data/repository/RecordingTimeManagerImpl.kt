package com.na982.opichelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.domain.manager.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.na982.opichelper.domain.repository.RecordingTimeManager

class RecordingTimeManagerImpl(
    private val context: Context,
    private val appLogger: AppLogger,
    private val gson: Gson
) : RecordingTimeManager {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cache = mutableMapOf<String, MutableList<Long>>()

    @Synchronized
    override fun saveRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int, recordingTimeMs: Long) {
        val key = getKey(category, scriptIndex)
        val times = cache.getOrPut(key) { loadFromPrefs(key) }

        while (times.size <= sentenceIndex) {
            times.add(0L)
        }

        times[sentenceIndex] = recordingTimeMs
        prefs.edit().putString(key, gson.toJson(times.toList())).apply()
    }

    override fun getRecordingTime(category: String, scriptIndex: Int, sentenceIndex: Int): Long? {
        val times = getAllRecordingTimes(category, scriptIndex)
        return if (sentenceIndex < times.size) times[sentenceIndex] else null
    }

    @Synchronized
    override fun getAllRecordingTimes(category: String, scriptIndex: Int): List<Long> {
        val key = getKey(category, scriptIndex)
        // 내부 가변 리스트를 그대로 반환하면, 소비자가 delay를 끼워 순회하는 동안
        // saveRecordingTime이 같은 리스트를 add/set으로 변경해 인덱스 오류·데이터 꼬임이
        // 발생할 수 있다. 불변 스냅샷을 반환한다.
        return cache.getOrPut(key) { loadFromPrefs(key) }.toList()
    }

    override fun hasRecordingTimes(category: String, scriptIndex: Int): Boolean {
        val key = getKey(category, scriptIndex)
        return cache.containsKey(key) || prefs.contains(key)
    }

    @Synchronized
    override fun clearRecordingTimes(category: String, scriptIndex: Int) {
        val key = getKey(category, scriptIndex)
        cache.remove(key)
        prefs.edit().remove(key).apply()
    }

    private fun loadFromPrefs(key: String): MutableList<Long> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        return try {
            gson.fromJson<List<Long>>(json, longListType)?.toMutableList() ?: mutableListOf()
        } catch (e: Exception) {
            appLogger.e("RecordingTimeManagerImpl", "JSON 파싱 오류: $json", e)
            mutableListOf()
        }
    }

    private fun getKey(category: String, scriptIndex: Int): String {
        return "${KEY_RECORDING_TIMES_PREFIX}${category}_${scriptIndex}"
    }

    companion object {
        private const val PREFS_NAME = "recording_times"
        private const val KEY_RECORDING_TIMES_PREFIX = "recording_times_"
        private val longListType = object : TypeToken<List<Long>>() {}.type
    }
}
