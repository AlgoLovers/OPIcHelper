package com.na982.opichelper.domain.usecase

import com.na982.opichelper.domain.repository.StudySessionRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordStudySessionUseCase @Inject constructor(
    private val studySessionRecorder: StudySessionRecorder
) {
    // 앱 수명과 함께 사는 @Singleton이므로 앱 스코프 IO 스코프를 둔다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var sessionStartTimeMs: Long = 0L

    fun startSession() {
        sessionStartTimeMs = System.currentTimeMillis()
    }

    fun endSession() {
        if (sessionStartTimeMs <= 0L) return
        val durationMs = System.currentTimeMillis() - sessionStartTimeMs
        sessionStartTimeMs = 0L
        if (durationMs <= 0L) return
        // recordSession은 SharedPreferences+Gson 순회(getStreak 최대 366회)를 포함한 무거운
        // 동기 IO다. endSession()은 MainActivity.onPause에서 호출되므로 메인 스레드에서 돌리면
        // 앱을 백그라운드로 보낼 때마다 프레임 드랍/ANR 위험이 있다. IO 디스패처로 옮긴다.
        scope.launch { studySessionRecorder.recordSession(durationMs) }
    }
}
