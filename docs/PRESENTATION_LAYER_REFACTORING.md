# Presentation Layer ViewModel 리팩토링 정리 (2024-04)

## 1. 리팩토링 목표
- MainViewModel의 책임 분리 및 복잡도 감소
- 각 기능별 ViewModel 분리 (Composition Pattern)
- Hilt/DI 정책에 맞는 구조로 개선
- 빌드 에러 및 KSP 문제 해결

---

## 2. 주요 변경점

### 2.1 ViewModel 분리 및 Composition 적용
- 기존 MainViewModel이 모든 Presentation Layer 상태와 로직을 담당하던 구조에서,
  - **TtsViewModel**: TTS 관련 상태/로직 담당
  - **MemorizationViewModel**: 암기 테스트(반복듣기, 영작 테스트, 통암기 등) 관련 상태/로직 담당
  - **QaDataViewModel**: QA 데이터 및 진행상황 관련 상태/로직 담당
- **MainViewModel**은 더 이상 암기 테스트 관련 상태/메서드를 포함하지 않음
- 각 ViewModel은 UI 계층(Compose, Fragment 등)에서 각각 생성하여 조합

### 2.2 DI 정책 준수
- ViewModel 간 직접 DI(생성자 주입) 금지
- MainViewModel은 오직 도메인/데이터 계층 객체만 주입받음
- ViewModel 간 데이터 공유는 Repository, UseCase, StateFlow 등으로 처리

### 2.3 빌드 에러 및 KSP 문제 해결
- combine()에 빈 Flow 전달로 인한 타입 추론 에러 → combine 코드 및 관련 함수 삭제
- Application 타입 null 전달로 인한 에러 → init(null) 호출 삭제
- Int? → Int 타입 불일치 → null 체크 후 안전하게 호출
- 빌드 캐시(KSP) 문제 발생 시 clean 후 재빌드로 해결

---

## 3. UI 계층(View)에서의 ViewModel 조합 예시
```kotlin
val mainViewModel: MainViewModel = hiltViewModel()
val ttsViewModel: TtsViewModel = hiltViewModel()
val memorizationViewModel: MemorizationViewModel = hiltViewModel()
val qaDataViewModel: QaDataViewModel = hiltViewModel()
```
- **암기 테스트 관련 UI/로직은 MemorizationViewModel의 StateFlow/메서드를 직접 사용**
- 예시:
```kotlin
val memorizeLevels by memorizationViewModel.memorizeLevels.collectAsState()
val isRunning by memorizationViewModel.isRunning.collectAsState()
val isFullMemorizationMode by memorizationViewModel.isFullMemorizationMode.collectAsState()
// ...
memorizationViewModel.onMemorizeTestButtonClick()
memorizationViewModel.startFullMemorizationMode()
// ...
```

---

## 4. 리팩토링 효과
- ViewModel별 책임 명확화, 유지보수성/확장성 향상
- Hilt/DI 정책에 맞는 안전한 구조
- 빌드 에러 및 내부 캐시 문제까지 모두 해결
- 테스트 및 기능별 단위 개발 용이
- **암기 테스트 로직이 MemorizationViewModel에만 집중되어, MainViewModel이 훨씬 단순해짐**

---

## 5. 참고 사항
- 남은 경고(warning)는 실제 동작과 무관하며, 필요시 추가 정리 가능
- 추가 리팩토링/테스트/문서화 요청은 언제든 환영

---

**최종 적용일:** 2024-04-XX 