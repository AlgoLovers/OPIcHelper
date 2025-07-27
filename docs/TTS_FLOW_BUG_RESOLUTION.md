# TTS 플로우 버그 해결

## 개요
반복듣기 기능에서 "TTS 오케스트레이터가 초기화되지 않음" 오류가 발생한 버그와 그 해결 과정을 분석한 문서입니다.

## 문제 상황
- **증상**: 앱 실행 후 반복듣기 기능 사용 시 "🎤 TTS 오케스트레이터가 초기화되지 않음" 오류 발생
- **영향**: TTS 재생이 정상적으로 작동하지 않음
- **발생 시점**: 리팩토링 후 복잡한 초기화 로직 도입 이후

## 정상 동작하는 버전 (6327436) - TTS 플로우 분석

### 1. TtsOrchestrator 구조
```kotlin
class TtsOrchestrator @Inject constructor(
    private val context: Context,
    private val googleTtsPlayer: TtsPlayer,
    private val samsungTtsPlayer: TtsPlayer
) {
    private val koreanTtsPlayers = listOf(samsungTtsPlayer)
    private var currentKoreanTtsIndex = 0
    
    // ✅ 핵심: isInitialized 변수가 없음
    // ✅ 핵심: init 블록이 없음
    // ✅ 핵심: waitForInitialization() 메서드가 없음
}
```

### 2. 정상 버전의 TTS 재생 플로우
```
1. speak(text) 호출
   ↓
2. 언어 감지 (한글/영문)
   ↓
3. speakKorean() 또는 speakEnglish() 직접 호출
   ↓
4. TtsPlayer.speak() 직접 호출
   ↓
5. TTS 재생 성공/실패 결과 반환
```

### 3. 정상 버전의 핵심 특징
- **단순성**: 복잡한 초기화 상태 관리 없음
- **직접성**: TtsPlayer에 직접 위임
- **즉시성**: 호출 즉시 TTS 재생 시도
- **폴백**: 한글 TTS 실패 시 자동으로 다음 서비스 시도

## 문제가 있던 버전 - TTS 플로우 분석

### 1. TtsOrchestrator 구조 (문제 버전)
```kotlin
class TtsOrchestrator @Inject constructor(...) {
    private var isInitialized = false  // ❌ 문제의 근원
    
    init {
        // ❌ 복잡한 초기화 로직
        isInitialized = false  // 또는 true
        Log.d("TtsOrchestrator", "MainViewModel에서 초기화 대기")
    }
    
    suspend fun waitForInitialization(): Boolean { ... }  // ❌ 불필요한 복잡성
    fun setInitialized(initialized: Boolean) { ... }      // ❌ 외부 상태 조작
}
```

### 2. 문제 버전의 TTS 재생 플로우
```
1. speak(text) 호출
   ↓
2. isInitialized 상태 확인  // ❌ 여기서 실패
   ↓ (false인 경우)
3. "TTS 오케스트레이터가 초기화되지 않음" 오류 로그
   ↓
4. onComplete?.invoke() 호출하고 false 반환
   ↓
5. TTS 재생 실패
```

### 3. 문제 버전의 초기화 플로우 (MainViewModel)
```
1. MainViewModel.initializeViewModel() 호출
   ↓
2. ttsPlaybackController.cleanupTts()
   ↓
3. app.ttsOrchestrator.waitForInitialization() 호출
   ↓
4. 각 BaseTtsPlayer.waitForInitialization() 호출
   ↓
5. initializationDeferred?.await() ?: false  // ❌ 여기서 문제
   ↓
6. app.ttsOrchestrator.setInitialized(result)
```

## 근본 원인 분석

### 1. 주요 문제점
1. **불필요한 복잡성**: 원래 단순하게 작동하던 TTS 재생에 복잡한 초기화 상태 관리 도입
2. **비동기 초기화 오류**: `initializationDeferred`가 null이거나 완료되지 않은 상태에서 `await()` 호출
3. **상태 불일치**: `TtsOrchestrator.isInitialized`와 실제 `TtsPlayer`들의 초기화 상태 불일치
4. **의존성 문제**: `MainViewModel`에서 `TtsOrchestrator`의 내부 상태를 외부에서 조작

### 2. 구체적 실패 지점
```kotlin
// BaseTtsPlayer.waitForInitialization()
suspend fun waitForInitialization(): Boolean {
    return try {
        initializationDeferred?.await() ?: false  // ❌ null이면 false 반환
    } catch (e: Exception) {
        Log.e(logTag, "$serviceName 초기화 대기 중 오류", e)
        false
    }
}
```

**문제**: `initializationDeferred`가 null인 경우 무조건 false 반환
**결과**: `TtsOrchestrator.waitForInitialization()`도 false 반환
**최종**: `TtsOrchestrator.isInitialized`가 false로 설정되어 모든 TTS 재생 차단

## 해결 방법

### 1. 단순성으로의 회귀
- `TtsOrchestrator`에서 `isInitialized` 상태 관리 제거
- `waitForInitialization()` 메서드 제거
- `setInitialized()` 메서드 제거
- `init` 블록의 복잡한 로직 제거

### 2. 직접 위임 방식 복원
```kotlin
// ✅ 해결된 버전
suspend fun speak(text: String, onComplete: (() -> Unit)?): Boolean {
    val isKorean = text.any { it.code in 0xAC00..0xD7AF || it.code in 0x3131..0x318E }
    
    return if (isKorean) {
        speakKorean(text, onComplete)  // 직접 호출
    } else {
        speakEnglish(text, onComplete)  // 직접 호출
    }
}
```

### 3. MainViewModel 단순화
```kotlin
// ✅ 해결된 버전 (필요시)
// 복잡한 TTS 초기화 로직 제거
// TtsOrchestrator는 자체적으로 TtsPlayer에 위임
```

## 교훈

### 1. 설계 원칙
- **KISS (Keep It Simple, Stupid)**: 단순하게 작동하는 코드를 복잡하게 만들지 말 것
- **단일 책임 원칙**: `TtsOrchestrator`는 TTS 조율만 담당, 초기화 상태 관리는 각 `TtsPlayer`에 위임
- **의존성 역전**: 상위 레벨에서 하위 레벨의 내부 상태를 직접 조작하지 말 것

### 2. 리팩토링 주의사항
- 정상 작동하는 코드를 리팩토링할 때는 기능을 단계별로 검증
- 복잡한 상태 관리 도입 시 실제 필요성 검토
- 비동기 초기화 로직은 특히 신중하게 설계

### 3. 디버깅 방법
- 로그를 통한 플로우 추적의 중요성
- 상태 변화 지점 정확한 파악
- 원래 작동하던 버전과의 차이점 분석

## 결론

이번 버그는 **과도한 복잡성 도입**으로 인한 전형적인 문제였습니다. 원래 단순하고 명확하게 작동하던 TTS 시스템에 불필요한 초기화 상태 관리 로직을 도입하면서 발생했습니다. 

핵심 교훈은 **"작동하는 코드를 건드리지 말라 (If it ain't broke, don't fix it)"** 입니다. 특히 비동기 처리가 포함된 복잡한 시스템에서는 더욱 신중해야 합니다.

해결은 단순히 **원래의 단순한 구조로 되돌리는 것**이었으며, 이를 통해 TTS 기능이 정상적으로 복원되었습니다. 