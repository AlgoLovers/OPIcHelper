# 최근 업데이트 내역 (Recent Updates)

## 2024년 12월 - 암기 레벨 저장/로드 및 녹음 재생 버튼 개선

### 🔧 주요 수정사항

#### 1. 암기 레벨 저장/로드 버그 수정
**문제**: 앱 재시작 시 이전에 선택한 암기 레벨이 복원되지 않음
- **원인**: `setSelectedMemorizeLevel()`에서 SharedPreferences 저장 로직 누락
- **해결**: `MainViewModel.kt`의 `setSelectedMemorizeLevel()` 메서드에 SharedPreferences 저장 로직 추가
```kotlin
// 추가된 코드
prefs?.edit()?.putString(PREF_KEY_LAST_MEMORIZE_LEVEL, level)?.apply()
Log.d("MainViewModel", "암기레벨 SharedPreferences 저장: $level")
```

#### 2. 암기 레벨별 녹음 재생 버튼 표시 로직 개선
**문제**: 통암기 모드에서 녹음 파일이 있어도 재생 버튼이 표시되지 않음
- **원인**: `selectedLevel`과 `isFullMemorizationMode` 상태 불일치
- **해결**: `MainScreen.kt`에서 `when (selectedLevel)` 기반 조건부 버튼 표시 로직 구현

#### 3. 녹음 재생 버튼이 없을 때 UI 개선
**문제**: 녹음 재생 버튼이 없을 때 "답변 1회 재생" 버튼이 작게 표시됨
- **해결**: 녹음 재생 버튼이 없을 때 Spacer 제거하여 "답변 1회 재생" 버튼이 가로 공간을 모두 차지하도록 수정

### 📋 개선된 기능들

#### 암기 레벨 관리
- ✅ 암기 레벨 선택 시 즉시 SharedPreferences 저장
- ✅ 앱 재시작 시 저장된 암기 레벨 정확히 복원
- ✅ 암기 레벨별 버튼 텍스트 동적 변경

#### 녹음 재생 버튼 표시 로직
- ✅ "반복 듣기" 모드: 녹음 재생 버튼 없음
- ✅ "영작 테스트" 모드: 병합 파일 있으면 재생 버튼 표시
- ✅ "통암기" 모드: 통암기 녹음 파일 있으면 재생 버튼 표시
- ✅ 녹음 재생 버튼 없을 때 "답변 1회 재생" 버튼 확장

#### 상태 관리 개선
- ✅ 중복 상태 변수 제거 및 단일 책임 원칙 적용
- ✅ ViewModel 간 상태 동기화 개선
- ✅ 상세한 로깅으로 디버깅 용이성 향상

### 🧪 테스트 시나리오

#### 암기 레벨 저장/로드 테스트
1. 앱 실행 후 암기 레벨을 "통암기"로 변경
2. 앱 종료 후 재시작
3. **예상 결과**: 암기 레벨이 "통암기"로 복원됨

#### 녹음 재생 버튼 표시 테스트
1. "집" 카테고리의 첫 번째 스크립트에서 "통암기" 모드 선택
2. 녹음 파일이 있는 경우 "통암기 녹음 재생" 버튼 표시 확인
3. "영작 테스트" 모드에서 병합 파일이 있는 경우 재생 버튼 표시 확인
4. "반복 듣기" 모드에서는 녹음 재생 버튼이 표시되지 않음 확인

### 🔄 수정된 파일들

#### `app/src/main/java/com/na982/opichelper/presentation/viewmodel/MainViewModel.kt`
- `setSelectedMemorizeLevel()`: SharedPreferences 저장 로직 추가
- 암기 레벨 저장/로드 플로우 개선

#### `app/src/main/java/com/na982/opichelper/presentation/ui/screen/MainScreen.kt`
- 암기 레벨별 조건부 녹음 재생 버튼 표시 로직 구현
- UI 레이아웃 개선 (Spacer 제거로 버튼 확장)

#### `app/src/main/java/com/na982/opichelper/presentation/viewmodel/MemorizationViewModel.kt`
- `AudioFileManager` 직접 사용으로 파일 존재 확인 정확도 향상
- 스크립트 변경 시 자동 녹음 파일 상태 확인 로직 추가

### 📊 성능 개선

- **상태 관리 최적화**: 중복 상태 제거로 메모리 사용량 감소
- **파일 시스템 접근 최적화**: 직접 `AudioFileManager` 사용으로 불필요한 중간 계층 제거
- **UI 반응성 향상**: 조건부 렌더링 최적화로 UI 업데이트 속도 개선

### 🐛 해결된 버그들

1. **암기 레벨 저장 실패**: SharedPreferences 저장 로직 누락으로 인한 앱 재시작 시 기본값 복원 문제
2. **통암기 재생 버튼 미표시**: 상태 불일치로 인한 버튼 표시 로직 오류
3. **UI 레이아웃 불균형**: 녹음 재생 버튼 없을 때 "답변 1회 재생" 버튼이 작게 표시되는 문제

### 🔮 향후 개선 계획

- [ ] 자동화된 테스트 케이스 추가
- [ ] 상태 관리 아키텍처 추가 리팩토링
- [ ] 사용자 피드백 기반 UI/UX 개선
- [ ] 성능 모니터링 도구 도입

---

**마지막 업데이트**: 2024년 12월
**버전**: 1.0.0
**담당자**: 개발팀 