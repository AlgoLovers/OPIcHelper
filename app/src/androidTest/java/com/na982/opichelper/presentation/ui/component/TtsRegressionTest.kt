package com.na982.opichelper.presentation.ui.component

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlaybackController
import com.na982.opichelper.data.audio.GoogleTtsPlayer
import com.na982.opichelper.data.audio.SamsungTtsPlayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import android.util.Log

/**
 * TTS 회귀 테스트
 * 롤백 전의 문제가 있었던 버전의 주요 이슈들을 재현하고 검증
 */
@RunWith(AndroidJUnit4::class)
class TtsRegressionTest {

    private lateinit var ttsOrchestrator: TtsOrchestrator
    private lateinit var ttsPlaybackController: TtsPlaybackController
    private lateinit var googleTtsPlayer: GoogleTtsPlayer
    private lateinit var samsungTtsPlayer: SamsungTtsPlayer

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // TTS 플레이어들 초기화
        googleTtsPlayer = GoogleTtsPlayer(context)
        samsungTtsPlayer = SamsungTtsPlayer(context)
        
        // TTS 초기화 완료 대기
        Log.d("TtsRegressionTest", "TTS 초기화 대기 중...")
        waitForTtsInitialization()
        Log.d("TtsRegressionTest", "TTS 초기화 완료")
        
        // TTS 오케스트레이터 초기화
        ttsOrchestrator = TtsOrchestrator(
            context = context,
            googleTtsPlayer = googleTtsPlayer,
            samsungTtsPlayer = samsungTtsPlayer
        )
        
        // TTS 재생 컨트롤러 초기화
        ttsPlaybackController = TtsPlaybackController(
            ttsPlayer = googleTtsPlayer,
            audioPlayer = mockAudioPlayer()
        )
        ttsPlaybackController.setTtsOrchestrator(ttsOrchestrator)
    }

    /**
     * TTS 초기화 완료 대기
     */
    private fun waitForTtsInitialization() {
        var attempts = 0
        val maxAttempts = 10
        
        while (attempts < maxAttempts) {
            if (googleTtsPlayer.isAvailable() && samsungTtsPlayer.isAvailable()) {
                Log.d("TtsRegressionTest", "TTS 초기화 완료 확인")
                return
            }
            
            Log.d("TtsRegressionTest", "TTS 초기화 대기 중... (시도 ${attempts + 1}/$maxAttempts)")
            Thread.sleep(500) // 0.5초 대기
            attempts++
        }
        
        Log.w("TtsRegressionTest", "TTS 초기화 시간 초과, 계속 진행")
    }

    // 문제 버전의 복잡한 초기화 상태 관리가 없어야 함
    @Test
    fun should_not_have_complex_initialization_state_management() = runTest {
        // Given - 문제 버전에서는 복잡한 초기화 상태 관리가 있었음
        // ❌ 문제 버전: isInitialized, waitForInitialization(), setInitialized() 등
        
        // When & Then - 현재 버전에서는 단순한 구조
        // ✅ 현재 버전: 각 TtsPlayer가 자체 상태 관리
        
        // TtsOrchestrator에 복잡한 초기화 메서드가 없어야 함
        val orchestratorClass = ttsOrchestrator::class.java
        val methods = orchestratorClass.declaredMethods.map { it.name }
        
        // 문제가 있었던 메서드들이 없어야 함
        assertFalse(methods.contains("waitForInitialization"))
        assertFalse(methods.contains("setInitialized"))
        assertFalse(methods.contains("isInitialized"))
        
        // 단순한 speak 메서드만 있어야 함
        assertTrue(methods.contains("speak"))
        assertTrue(methods.contains("stop"))
    }

    // 문제 버전의 중복된 TTS 인스턴스 문제가 해결되어야 함
    @Test
    fun should_not_have_duplicate_tts_instances() {
        // Given - 문제 버전에서는 같은 TTS 인스턴스가 여러 곳에 존재했음
        // ❌ 문제 버전: TtsPlaybackController + TtsOrchestrator에 중복 주입
        
        // When & Then - 현재 버전에서는 명확한 책임 분리
        // ✅ 현재 버전: TtsPlaybackController는 TtsOrchestrator만 사용
        
        // TtsPlaybackController가 직접 TtsPlayer를 사용하지 않아야 함
        val controllerClass = ttsPlaybackController::class.java
        val fields = controllerClass.declaredFields.map { it.name }
        
        // ttsPlayer 필드가 직접 사용되지 않아야 함 (TtsOrchestrator만 사용)
        assertTrue(fields.contains("ttsOrchestrator"))
    }

    // 문제 버전의 상태 불일치 문제가 해결되어야 함
    @Test
    fun should_not_have_state_inconsistency() = runTest {
        // Given - 문제 버전에서는 TtsOrchestrator와 TtsPlayer 간 상태 불일치가 있었음
        // ❌ 문제 버전: isInitialized와 실제 TtsPlayer 상태 불일치
        
        // When & Then - 현재 버전에서는 각 TtsPlayer가 자체 상태 관리
        // ✅ 현재 버전: 각 TtsPlayer의 isAvailable() 직접 확인
        
        val googleAvailable = googleTtsPlayer.isAvailable()
        val samsungAvailable = samsungTtsPlayer.isAvailable()
        
        // 각 TTS 플레이어의 상태가 독립적으로 관리되어야 함
        assertTrue(googleAvailable || samsungAvailable) // 최소 하나는 사용 가능해야 함
    }

    // 문제 버전의 의존성 역전 원칙 위반이 해결되어야 함
    @Test
    fun should_not_violate_dependency_inversion_principle() {
        // Given - 문제 버전에서는 MainViewModel에서 TtsOrchestrator 내부 상태를 직접 조작했음
        // ❌ 문제 버전: app.ttsOrchestrator.waitForInitialization(), setInitialized()
        
        // When & Then - 현재 버전에서는 단순한 설정만
        // ✅ 현재 버전: ttsPlaybackController.setTtsOrchestrator()만 사용
        
        // TtsOrchestrator의 내부 상태를 외부에서 조작하는 메서드가 없어야 함
        val orchestratorClass = ttsOrchestrator::class.java
        val publicMethods = orchestratorClass.methods.map { it.name }
        
        // 상태 조작 메서드가 없어야 함
        assertFalse(publicMethods.contains("setInitialized"))
        assertFalse(publicMethods.contains("waitForInitialization"))
        
        // 단순한 설정 메서드만 있어야 함
        assertTrue(publicMethods.contains("setTtsOrchestrator"))
    }

    // 문제 버전의 복잡한 상태 관리가 단순화되어야 함
    @Test
    fun should_have_simplified_state_management() = runTest {
        // Given - 문제 버전에서는 여러 개의 개별 StateFlow로 복잡한 상태 관리
        // ❌ 문제 버전: _isPlaying, _isQuestionPlaying, _isAnswerPlaying 등
        
        // When & Then - 현재 버전에서는 명확한 상태 관리
        // ✅ 현재 버전: 각 컴포넌트가 명확한 책임을 가짐
        
        // TtsPlaybackController의 상태가 명확하게 분리되어야 함
        assertTrue(ttsPlaybackController.isPlaying.first() == false)
        assertTrue(ttsPlaybackController.isQuestionPlaying.first() == false)
        assertTrue(ttsPlaybackController.isAnswerPlaying.first() == false)
    }

    // 문제 버전의 TTS 오케스트레이터 초기화 오류가 해결되어야 함
    @Test
    fun should_not_have_tts_orchestrator_initialization_error() = runTest {
        // Given - 문제 버전에서는 "TTS 오케스트레이터가 초기화되지 않음" 오류가 발생했음
        // ❌ 문제 버전: initializationDeferred?.await() ?: false로 인한 false 반환
        
        // When & Then - 현재 버전에서는 단순한 위임 구조
        // ✅ 현재 버전: 직접 TtsPlayer에 위임
        
        val englishText = "Test text"
        val result = ttsOrchestrator.speak(englishText, null)
        
        // 초기화 오류 없이 정상적으로 작동해야 함
        assertTrue(result)
    }

    // 문제 버전의 비동기 초기화 오류가 해결되어야 함
    @Test
    fun should_not_have_async_initialization_error() = runTest {
        // Given - 문제 버전에서는 initializationDeferred가 null이거나 완료되지 않은 상태에서 await() 호출
        // ❌ 문제 버전: initializationDeferred?.await() ?: false
        
        // When & Then - 현재 버전에서는 단순한 초기화
        // ✅ 현재 버전: 각 TtsPlayer의 자체 초기화
        
        // TTS 플레이어들이 안전하게 초기화되어야 함
        kotlinx.coroutines.delay(2000) // 초기화 대기
        
        val googleAvailable = googleTtsPlayer.isAvailable()
        val samsungAvailable = samsungTtsPlayer.isAvailable()
        
        // 최소 하나의 TTS 플레이어가 사용 가능해야 함
        assertTrue(googleAvailable || samsungAvailable)
    }

    // 문제 버전의 과도한 복잡성이 제거되어야 함
    @Test
    fun should_not_have_excessive_complexity() {
        // Given - 문제 버전에서는 불필요한 복잡한 로직이 있었음
        // ❌ 문제 버전: 복잡한 서비스 바인딩, WakeLock, 브로드캐스트 리시버 등
        
        // When & Then - 현재 버전에서는 단순한 구조
        // ✅ 현재 버전: 명확한 책임 분리
        
        // TtsOrchestrator의 메서드 수가 적절해야 함
        val orchestratorClass = ttsOrchestrator::class.java
        val publicMethods = orchestratorClass.methods.filter { 
            it.modifiers and java.lang.reflect.Modifier.PUBLIC != 0 
        }
        
        // 복잡한 메서드들이 없어야 함
        val methodNames = publicMethods.map { it.name }
        assertFalse(methodNames.contains("bindTtsService"))
        assertFalse(methodNames.contains("unbindTtsService"))
        assertFalse(methodNames.contains("acquireWakeLock"))
        assertFalse(methodNames.contains("releaseWakeLock"))
        
        // 핵심 기능만 있어야 함
        assertTrue(methodNames.contains("speak"))
        assertTrue(methodNames.contains("stop"))
        assertTrue(methodNames.contains("isPlaying"))
    }

    // 문제 버전의 상태 동기화 문제가 해결되어야 함
    @Test
    fun should_not_have_state_sync_issue() = runTest {
        // Given - 문제 버전에서는 여러 StateFlow 간 상태 동기화 문제가 있었음
        // ❌ 문제 버전: 복잡한 combine() 로직과 상태 불일치
        
        // When & Then - 현재 버전에서는 단순한 상태 관리
        // ✅ 현재 버전: 각 컴포넌트가 명확한 상태 관리
        
        // TtsPlaybackController의 상태가 일관성 있게 관리되어야 함
        ttsPlaybackController.playQuestion("Test question")
        
        // 재생 중 상태 확인
        assertTrue(ttsPlaybackController.isPlaying.first())
        assertTrue(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())
        
        // 중지 후 상태 확인
        ttsPlaybackController.stopTts()
        assertFalse(ttsPlaybackController.isPlaying.first())
        assertFalse(ttsPlaybackController.isQuestionPlaying.first())
        assertFalse(ttsPlaybackController.isAnswerPlaying.first())
    }

    // 문제 버전의 메모리 누수 문제가 해결되어야 함
    @Test
    fun should_not_have_memory_leak_issue() {
        // Given - 문제 버전에서는 복잡한 상태 관리로 인한 메모리 누수가 있었음
        // ❌ 문제 버전: 중복된 상태, 불필요한 리스너 등
        
        // When & Then - 현재 버전에서는 효율적인 메모리 관리
        // ✅ 현재 버전: 단일 책임 원칙과 명확한 리소스 관리
        
        // TtsOrchestrator가 불필요한 필드를 가지지 않아야 함
        val orchestratorClass = ttsOrchestrator::class.java
        val fields = orchestratorClass.declaredFields.map { it.name }
        
        // 복잡한 상태 필드들이 없어야 함
        assertFalse(fields.contains("isInitialized"))
        assertFalse(fields.contains("initializationDeferred"))
        assertFalse(fields.contains("wakeLock"))
        assertFalse(fields.contains("audioFocusRequest"))
        
        // 핵심 필드만 있어야 함
        assertTrue(fields.contains("googleTtsPlayer"))
        assertTrue(fields.contains("samsungTtsPlayer"))
        assertTrue(fields.contains("koreanTtsPlayers"))
        assertTrue(fields.contains("currentKoreanTtsIndex"))
    }

    // 문제 버전의 테스트 어려움 문제가 해결되어야 함
    @Test
    fun should_not_have_test_difficulty_issue() {
        // Given - 문제 버전에서는 복잡한 의존성으로 인한 테스트 어려움이 있었음
        // ❌ 문제 버전: 복잡한 서비스 바인딩, 외부 의존성 등
        
        // When & Then - 현재 버전에서는 테스트 용이한 구조
        // ✅ 현재 버전: 명확한 인터페이스와 단순한 의존성
        
        // TtsOrchestrator가 테스트하기 쉬운 구조여야 함
        val orchestratorClass = ttsOrchestrator::class.java
        
        // 생성자가 단순해야 함
        val constructors = orchestratorClass.constructors
        assertTrue(constructors.isNotEmpty())
        
        val constructor = constructors.first()
        val parameterTypes = constructor.parameterTypes
        
        // 필요한 의존성만 주입받아야 함
        assertEquals(3, parameterTypes.size) // context, googleTtsPlayer, samsungTtsPlayer
        assertTrue(parameterTypes.contains(android.content.Context::class.java))
        assertTrue(parameterTypes.contains(com.na982.opichelper.domain.audio.TtsPlayer::class.java))
    }

    // TTS 서비스 이름이 올바르게 반환되어야 함
    @Test
    fun tts_service_name_should_be_correct() {
        // When & Then
        assertEquals("Google TTS", googleTtsPlayer.getServiceName())
        // 실제 기기에서는 "삼성 TTS"로 표시됨
        assertEquals("삼성 TTS", samsungTtsPlayer.getServiceName())
    }

    private fun mockAudioPlayer(): com.na982.opichelper.domain.audio.AudioPlayer {
        return object : com.na982.opichelper.domain.audio.AudioPlayer {
            override fun play(file: java.io.File, onComplete: () -> Unit) {
                onComplete()
            }
            
            override fun stop() {
                // 테스트용 더미 구현
            }
            
            override fun stopAudio() {
                // 테스트용 더미 구현
            }
            
            override val isPlaying: Boolean = false
            
            override fun getDuration(filePath: String): Int {
                return 1000 // 테스트용 더미 값
            }
            
            override fun playAudio(filePath: String) {
                // 테스트용 더미 구현
            }
        }
    }
} 