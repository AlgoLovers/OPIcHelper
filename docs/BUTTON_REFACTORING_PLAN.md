# 버튼 리팩토링 계획 (Button Refactoring Plan)

## 🔍 현재 문제점 분석

### 1. 버튼 기능 혼동 문제
- **문제**: 암기레벨에 따라 버튼이 다른 기능을 수행해야 하는데, 상태 관리가 복잡하고 중복됨
- **원인**: 하나의 버튼이 여러 상태에 따라 다른 기능을 하는데, 상태 전환이 명확하지 않음
- **영향**: 사용자가 버튼을 눌렀을 때 예상과 다른 동작이 발생

### 2. 인터럽트 처리 문제
- **문제**: 카테고리 변경, 암기레벨 변경, 설정 진입, 스크립트 변경, 앱 종료 시 진행 중인 작업이 안정적으로 중단되지 않음
- **원인**: 중복 초기화로 인한 상태 불일치 발생
- **영향**: 앱이 불안정해지고 예상치 못한 동작 발생

### 3. 동적 UI 변경 문제
- **문제**: 녹음 파일 존재 여부에 따른 UI 변경이 제대로 동작하지 않음
- **원인**: 상태 업데이트 타이밍 문제
- **영향**: 사용자 경험 저하

## 🏗️ 클린 아키텍처 기반 리팩토링

### 1. 단일 책임 원칙 적용

#### 새로운 클래스 구조
```
domain/
├── entity/
│   ├── ButtonState.kt          # 버튼 상태 정의
│   └── ButtonFunction.kt       # 버튼 기능 정의
├── audio/
│   ├── ButtonStateManager.kt   # 버튼 상태 중앙 관리
│   ├── InterruptManager.kt     # 인터럽트 처리 중앙 관리
│   └── ButtonActionHandler.kt  # 버튼 동작 처리
```

#### 각 클래스의 책임
- **ButtonStateManager**: 버튼 상태만을 관리 (단일 책임)
- **InterruptManager**: 인터럽트 처리만을 담당 (단일 책임)
- **ButtonActionHandler**: 버튼 동작만을 처리 (단일 책임)

### 2. 의존성 역전 원칙 적용

#### 인터페이스 정의
```kotlin
interface ButtonAction {
    fun execute()
    fun stop()
    fun pause()
    fun resume()
}

interface InterruptHandler {
    fun handleCategoryChange()
    fun handleMemorizeLevelChange()
    fun handleScriptChange()
    fun handleSettingsEnter()
    fun handleAppExit()
    fun handleBackPress()
}
```

#### 추상화된 버튼 동작
- 버튼의 구체적인 동작을 추상화하여 인터페이스로 정의
- 구체적인 구현을 분리하여 유연성 확보

### 3. 상태 머신 패턴 도입

#### 버튼 상태 정의
```kotlin
sealed class ButtonState {
    object Idle : ButtonState()
    object Loading : ButtonState()
    object Playing : ButtonState()
    object Recording : ButtonState()
    object Paused : ButtonState()
    object Error : ButtonState()
}
```

#### 상태 전환 규칙
- **Idle → Loading**: 버튼 클릭 시
- **Loading → Playing**: 작업 시작 시
- **Playing → Idle**: 작업 완료 또는 중지 시
- **Playing → Paused**: 일시 중지 시
- **Error → Idle**: 오류 복구 시

## 🔧 구현된 개선사항

### 1. 중앙화된 버튼 상태 관리

#### ButtonStateManager
- 모든 버튼의 상태를 중앙에서 관리
- 상태 변경 시 자동으로 UI 업데이트
- 중복 상태 제거로 일관성 확보

#### 주요 기능
```kotlin
fun updateButtonState(buttonType: ButtonFunction, state: ButtonState)
fun getButtonConfig(buttonType: ButtonFunction): ButtonConfig
fun resetAllButtons()
```

### 2. 체계적인 인터럽트 처리

#### InterruptManager
- 모든 인터럽트 상황을 체계적으로 처리
- 안전한 리소스 정리 보장
- 중복 초기화 방지

#### 처리 시나리오
1. **카테고리 변경**: 모든 TTS 중지 → 버튼 상태 초기화 → 진행상황 저장
2. **암기레벨 변경**: 모든 TTS 중지 → 버튼 상태 초기화 → 새로운 레벨 로드
3. **스크립트 변경**: 모든 TTS 중지 → 버튼 상태 초기화 → 새 스크립트 로드
4. **설정 진입**: TTS 일시 중지 → 상태 유지 (복귀 시 복원)
5. **앱 종료**: 모든 TTS 중지 → 버튼 상태 초기화 → 리소스 정리

### 3. 스마트 버튼 컴포넌트

#### SmartButton
- 상태에 따른 동적 UI 변경
- 조건부 렌더링으로 성능 최적화
- 일관된 사용자 경험 제공

#### 주요 특징
```kotlin
@Composable
fun SmartButton(
    buttonConfig: ButtonConfig,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

### 4. 동적 UI 변경 개선

#### 조건부 버튼 표시
- 녹음 파일 존재 여부에 따른 동적 표시
- 암기레벨에 따른 버튼 텍스트 변경
- 상태에 따른 버튼 색상 변경

## 📋 테스트 시나리오

### 1. 버튼 기능 테스트
- [ ] 각 암기레벨에서 버튼이 올바른 기능 수행
- [ ] 버튼 상태 변경 시 UI가 정확히 업데이트
- [ ] 재생 중 버튼 클릭 시 중지 기능 동작

### 2. 인터럽트 처리 테스트
- [ ] 카테고리 변경 시 진행 중인 작업 안전 중단
- [ ] 암기레벨 변경 시 상태 초기화
- [ ] 스크립트 변경 시 진행상황 저장
- [ ] 설정 진입 시 TTS 일시 중지
- [ ] 앱 종료 시 리소스 정리

### 3. 동적 UI 테스트
- [ ] 녹음 파일 존재 시 재생 버튼 표시
- [ ] 암기레벨 변경 시 버튼 텍스트 변경
- [ ] 상태 변경 시 버튼 색상 변경

## 🚀 성능 개선

### 1. 메모리 사용량 최적화
- 중복 상태 제거로 메모리 사용량 감소
- 조건부 렌더링으로 불필요한 UI 업데이트 방지

### 2. 반응성 향상
- 중앙화된 상태 관리로 UI 업데이트 속도 개선
- 인터럽트 처리 최적화로 앱 반응성 향상

### 3. 안정성 개선
- 체계적인 인터럽트 처리로 앱 안정성 향상
- 오류 처리 강화로 예외 상황 대응

## 🔄 마이그레이션 계획

### 1단계: 새로운 클래스 구현
- [x] ButtonState.kt 생성
- [x] ButtonStateManager.kt 구현
- [x] InterruptManager.kt 구현
- [x] ButtonActionHandler.kt 구현

### 2단계: DI 설정
- [x] AppModule.kt에 새로운 클래스들 추가
- [x] 의존성 주입 설정

### 3단계: UI 컴포넌트 구현
- [x] SmartButton.kt 구현
- [x] MainScreenRefactored.kt 구현

### 4단계: ViewModel 업데이트
- [x] MainViewModel에 새로운 메서드들 추가
- [x] 기존 로직과 새로운 시스템 연동

### 5단계: 테스트 및 검증
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 수행
- [ ] 사용자 테스트 진행

## 📊 기대 효과

### 1. 코드 품질 향상
- 단일 책임 원칙으로 코드 가독성 향상
- 의존성 역전으로 유연성 확보
- 상태 머신으로 예측 가능한 동작

### 2. 사용자 경험 개선
- 안정적인 인터럽트 처리
- 일관된 버튼 동작
- 동적 UI 변경으로 직관적인 인터페이스

### 3. 유지보수성 향상
- 명확한 책임 분리
- 확장 가능한 구조
- 테스트 가능한 코드

## 🎯 다음 단계

1. **테스트 구현**: 새로운 시스템에 대한 단위 테스트 및 통합 테스트 작성
2. **점진적 마이그레이션**: 기존 MainScreen을 새로운 시스템으로 점진적 교체
3. **성능 모니터링**: 메모리 사용량 및 반응성 개선 효과 측정
4. **사용자 피드백**: 실제 사용자 테스트를 통한 개선사항 확인 