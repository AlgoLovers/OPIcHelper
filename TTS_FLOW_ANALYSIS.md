# TTS 플로우 분석 및 개선 방안

## 🔍 현재 TTS 플로우 분석

### **1. 의존성 주입 (DI) 플로우**
```
AppModule
├── provideGoogleTtsPlayer() → GoogleTtsPlayer
├── provideSamsungTtsPlayer() → SamsungTtsPlayer
└── provideTtsOrchestrator() → TtsOrchestrator(GoogleTtsPlayer, SamsungTtsPlayer)
    ↓
OPicHelperApplication
    ↓
MainViewModel
    ↓
TtsPlaybackController
    ↓
TtsOrchestrator
    ↓
BaseTtsPlayer ← GoogleTtsPlayer, SamsungTtsPlayer
```

### **2. TTS 컴포넌트 계층 구조**
```
UI (MainScreen)
    ↓ (사용자 액션)
MainViewModel
    ↓ (TTS 재생 요청)
TtsPlaybackController (상태 관리)
    ↓ (TTS 조율)
TtsOrchestrator (언어 감지 및 폴백)
    ↓ (실제 TTS 재생)
BaseTtsPlayer ← GoogleTtsPlayer (영문), SamsungTtsPlayer (한글)
```

### **3. 현재 문제점들**

#### **문제 1: 중복된 TTS 인스턴스**
- `TtsPlaybackController`에 `ttsPlayer: TtsPlayer` 주입
- `TtsOrchestrator`에 `googleTtsPlayer`, `samsungTtsPlayer` 주입
- 같은 TTS 인스턴스가 여러 곳에 존재

#### **문제 2: 복잡한 상태 관리**
- `TtsPlaybackController`에서 재생 상태 관리
- `BaseTtsPlayer`에서 초기화 상태 관리
- `TtsOrchestrator`에서 폴백 상태 관리

#### **문제 3: 불명확한 책임 분리**
- `TtsPlaybackController`: 상태 관리 + 재생 제어
- `TtsOrchestrator`: 언어 감지 + 폴백 + 재생
- `BaseTtsPlayer`: 초기화 + 재생

## 🎯 개선된 TTS 플로우 설계

### **1. 명확한 책임 분리**

#### **TtsPlayer (인터페이스)**
- **책임**: 단일 TTS 서비스 재생
- **구현체**: GoogleTtsPlayer, SamsungTtsPlayer
- **기능**: speak(), stop(), isAvailable()

#### **TtsOrchestrator**
- **책임**: 언어 감지, 적절한 TTS 선택, 폴백 처리
- **의존성**: GoogleTtsPlayer, SamsungTtsPlayer
- **기능**: speak(), speakWithHighlight()

#### **TtsPlaybackController**
- **책임**: 재생 상태 관리, 하이라이트 관리
- **의존성**: TtsOrchestrator
- **기능**: playQuestion(), playAnswer(), 상태 관리

#### **MainViewModel**
- **책임**: UI 상태 관리, 사용자 액션 처리
- **의존성**: TtsPlaybackController
- **기능**: TTS 재생 요청, 상태 구독

### **2. 개선된 의존성 주입**

```kotlin
@Module
object AppModule {
    // TTS Players
    @Provides @Singleton
    fun provideGoogleTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return GoogleTtsPlayer(context)
    }
    
    @Provides @Singleton
    fun provideSamsungTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return SamsungTtsPlayer(context)
    }
    
    // TTS Orchestrator
    @Provides @Singleton
    fun provideTtsOrchestrator(
        @ApplicationContext context: Context,
        googleTtsPlayer: TtsPlayer,
        samsungTtsPlayer: TtsPlayer
    ): TtsOrchestrator {
        return TtsOrchestrator(context, googleTtsPlayer, samsungTtsPlayer)
    }
    
    // TTS Playback Controller
    @Provides @Singleton
    fun provideTtsPlaybackController(
        ttsOrchestrator: TtsOrchestrator,
        audioPlayer: AudioPlayer
    ): TtsPlaybackController {
        return TtsPlaybackController(ttsOrchestrator, audioPlayer)
    }
}
```

### **3. 개선된 플로우**

```
사용자 액션 (TTS 재생 버튼 클릭)
    ↓
MainViewModel.playQuestion()
    ↓
TtsPlaybackController.playQuestion()
    ↓
TtsOrchestrator.speakWithHighlight()
    ↓
언어 감지 (영문/한글)
    ↓
적절한 TTS 선택 (GoogleTtsPlayer/SamsungTtsPlayer)
    ↓
BaseTtsPlayer.speak()
    ↓
TextToSpeech API 호출
    ↓
하이라이트 콜백 → UI 업데이트
```

## 🛠️ 수정 계획

### **1단계: TtsPlaybackController 수정**
- `TtsPlayer` 의존성 제거
- `TtsOrchestrator`만 사용하도록 변경

### **2단계: TtsOrchestrator 단순화**
- 언어 감지 및 폴백 로직 명확화
- 불필요한 재시도 로직 제거

### **3단계: BaseTtsPlayer 안정화**
- 초기화 로직 단순화
- 명확한 에러 처리

### **4단계: DI 설정 정리**
- 중복 의존성 제거
- 명확한 의존성 주입

## 📋 체크리스트

- [ ] TtsPlaybackController에서 TtsPlayer 의존성 제거
- [ ] TtsOrchestrator 단순화
- [ ] BaseTtsPlayer 초기화 로직 개선
- [ ] DI 설정 정리
- [ ] 플로우 테스트
- [ ] 로그 정리 