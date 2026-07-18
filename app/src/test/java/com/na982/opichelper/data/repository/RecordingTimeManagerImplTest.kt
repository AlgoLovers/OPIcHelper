package com.na982.opichelper.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.na982.opichelper.domain.manager.AppLogger
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 문장별 녹음 시간 저장/조회 검증 (Robolectric). 영작테스트의 재생 하이라이트 타이밍이
 * 이 값에 의존하며, getAllRecordingTimes가 내부 리스트를 참조로 노출하지 않고
 * 방어적 복사(스냅샷)를 반환하는지도 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingTimeManagerImplTest {

    private lateinit var mgr: RecordingTimeManagerImpl

    @Before
    fun setUp() {
        mgr = RecordingTimeManagerImpl(
            ApplicationProvider.getApplicationContext<Context>(),
            mockk<AppLogger>(relaxed = true),
            Gson()
        )
    }

    @Test
    fun `저장한 녹음 시간을 조회한다`() {
        mgr.saveRecordingTime("집", 0, 0, 1500L)
        assertEquals(1500L, mgr.getRecordingTime("집", 0, 0))
    }

    @Test
    fun `여러 문장의 시간을 순서대로 반환한다`() {
        mgr.saveRecordingTime("집", 0, 0, 1000L)
        mgr.saveRecordingTime("집", 0, 1, 2000L)
        assertEquals(listOf(1000L, 2000L), mgr.getAllRecordingTimes("집", 0))
    }

    @Test
    fun `범위를 벗어난 문장 인덱스는 null`() {
        mgr.saveRecordingTime("집", 0, 0, 1000L)
        assertNull(mgr.getRecordingTime("집", 0, 5))
    }

    @Test
    fun `hasRecordingTimes와 clear 동작`() {
        mgr.saveRecordingTime("집", 0, 0, 1000L)
        assertTrue(mgr.hasRecordingTimes("집", 0))

        mgr.clearRecordingTimes("집", 0)
        assertFalse(mgr.hasRecordingTimes("집", 0))
        assertTrue(mgr.getAllRecordingTimes("집", 0).isEmpty())
    }

    @Test
    fun `getAllRecordingTimes는 방어적 복사를 반환한다`() {
        mgr.saveRecordingTime("집", 0, 0, 1000L)
        val snapshot = mgr.getAllRecordingTimes("집", 0)

        // 이후 내부 리스트가 커져도 앞서 받은 스냅샷은 변하지 않아야 함
        mgr.saveRecordingTime("집", 0, 1, 2000L)
        assertEquals(1, snapshot.size)
        assertEquals(listOf(1000L), snapshot)
    }
}
