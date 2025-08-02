package com.na982.opichelper.domain.manager

import android.content.Context
import android.os.PowerManager
import com.na982.opichelper.LogIgnoreRule
import io.mockk.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * WakeLockManager 테스트
 * 화면 켜짐 유지를 위한 WakeLock 관리 기능 테스트
 */
class WakeLockManagerTest {

    @get:Rule
    val logIgnoreRule = LogIgnoreRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPowerManager: PowerManager

    @Mock
    private lateinit var mockWakeLock: PowerManager.WakeLock

    private lateinit var wakeLockManager: WakeLockManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Context 모킹
        whenever(mockContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mockPowerManager)
        whenever(mockPowerManager.newWakeLock(any(), any())).thenReturn(mockWakeLock)
        
        wakeLockManager = WakeLockManager(mockContext)
        
        // WakeLock 획득을 통해 mockWakeLock 설정
        whenever(mockWakeLock.isHeld).thenReturn(false)
        wakeLockManager.acquireWakeLock()
    }

    @Test
    fun `WakeLock_획득_성공`() {
        // Given: WakeLock이 해제된 상태
        whenever(mockWakeLock.isHeld).thenReturn(false)
        whenever(mockWakeLock.acquire()).thenAnswer { }

        // When: WakeLock 획득
        wakeLockManager.acquireWakeLock()

        // Then: WakeLock이 정상적으로 획득됨
        verify(mockWakeLock).acquire()
    }

    @Test
    fun `WakeLock_이미_획득된_상태에서_재획득_시도`() {
        // Given: WakeLock이 이미 획득된 상태
        whenever(mockWakeLock.isHeld).thenReturn(true)

        // When: WakeLock 재획득 시도
        wakeLockManager.acquireWakeLock()

        // Then: 재획득하지 않고 기존 WakeLock 유지
        verify(mockWakeLock, never()).acquire()
    }

    @Test
    fun `WakeLock_해제_성공`() {
        // Given: WakeLock이 획득된 상태
        whenever(mockWakeLock.isHeld).thenReturn(true)
        whenever(mockWakeLock.release()).thenAnswer { }

        // When: WakeLock 해제
        wakeLockManager.releaseWakeLock()

        // Then: WakeLock이 정상적으로 해제됨
        verify(mockWakeLock).release()
    }

    @Test
    fun `WakeLock_이미_해제된_상태에서_재해제_시도`() {
        // Given: WakeLock이 이미 해제된 상태
        whenever(mockWakeLock.isHeld).thenReturn(false)

        // When: WakeLock 재해제 시도
        wakeLockManager.releaseWakeLock()

        // Then: 재해제하지 않음
        verify(mockWakeLock, never()).release()
    }

    @Test
    fun `WakeLock_상태_확인_획득됨`() {
        // Given: WakeLock이 획득된 상태
        whenever(mockWakeLock.isHeld).thenReturn(true)

        // When: WakeLock 상태 확인
        val isHeld = wakeLockManager.isWakeLockHeld()

        // Then: true 반환
        assertTrue(isHeld)
    }

    @Test
    fun `WakeLock_상태_확인_해제됨`() {
        // Given: WakeLock이 해제된 상태
        whenever(mockWakeLock.isHeld).thenReturn(false)

        // When: WakeLock 상태 확인
        val isHeld = wakeLockManager.isWakeLockHeld()

        // Then: false 반환
        assertFalse(isHeld)
    }

    @Test
    fun `안전한_해제_성공`() {
        // Given: WakeLock이 획득된 상태
        whenever(mockWakeLock.isHeld).thenReturn(true)
        whenever(mockWakeLock.release()).thenAnswer { }

        // When: 안전한 해제 실행
        wakeLockManager.safeRelease()

        // Then: WakeLock이 정상적으로 해제됨 (release 호출됨)
        verify(mockWakeLock).release()
    }

    @Test
    fun `안전한_해제_이미_해제된_상태`() {
        // Given: WakeLock이 이미 해제된 상태
        whenever(mockWakeLock.isHeld).thenReturn(false)

        // When: 안전한 해제 실행
        wakeLockManager.safeRelease()

        // Then: 예외 없이 정상 처리
        verify(mockWakeLock, never()).release()
    }

    @Test
    fun `WakeLock_획득_실패_처리`() {
        // Given: WakeLock 획득 시 예외 발생
        whenever(mockWakeLock.isHeld).thenReturn(false)
        whenever(mockWakeLock.acquire()).thenThrow(RuntimeException("WakeLock 획득 실패"))

        // When: WakeLock 획득 시도
        wakeLockManager.acquireWakeLock()

        // Then: 예외가 발생해도 앱이 크래시되지 않음
        // (로그만 출력되고 정상적으로 처리됨)
    }

    @Test
    fun `WakeLock_해제_실패_처리`() {
        // Given: WakeLock 해제 시 예외 발생
        whenever(mockWakeLock.isHeld).thenReturn(true)
        whenever(mockWakeLock.release()).thenThrow(RuntimeException("WakeLock 해제 실패"))

        // When: WakeLock 해제 시도
        wakeLockManager.releaseWakeLock()

        // Then: 예외가 발생해도 앱이 크래시되지 않음
        // (로그만 출력되고 정상적으로 처리됨)
    }
} 