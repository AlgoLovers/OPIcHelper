# 📊 암기레벨 영작테스트 녹음 파일 로그 분석 가이드

## 🎯 목적
암기레벨 영작테스트에서 녹음 파일이 잘 생성되고 병합되는지 분석하기 위한 로그 관리 및 분석 방법

## 📋 로그 개선 사항

### 1. 로그 레벨 구조화
- **INFO**: 중요한 진행 상황 (테스트 시작/완료, 파일 병합 완료)
- **DEBUG**: 상세 정보 (개별 문장 처리, 파일 저장)
- **VERBOSE**: 매우 상세한 정보 (바이트 단위 처리 등)

### 2. 로그 태그 통일
- `AudioFileManager`: 파일 관리 관련
- `EnglishWritingTest`: 영작테스트 진행 관련
- `RecordingFileRepositoryImpl`: 녹음 파일 저장소 관련

### 3. 로그 메시지 간소화
- 긴 파일 경로 대신 파일명만 표시
- 반복적인 정보는 DEBUG 레벨로 이동
- 핵심 정보만 INFO 레벨에 유지

## 🔍 분석 포인트

### 1. 녹음 파일 생성 확인
```
[INFO] EnglishWritingTest: 영작 테스트 시작: bank/0
[INFO] EnglishWritingTest: 영작 테스트 진행: 총 3문장, 시작 인덱스: 0
[INFO] AudioFileManager: 녹음 파일 저장: english_writing_bank_0_0.m4a (12345 bytes)
```

### 2. 파일 병합 과정 확인
```
[INFO] AudioFileManager: 오디오 파일 병합 시작: 영작테스트_bank_0_20241201_143022
[INFO] AudioFileManager: MediaCodec 병합 완료
[INFO] AudioFileManager: 오디오 파일 병합 완료: 영작테스트_bank_0_20241201_143022.m4a (45678 bytes)
```

### 3. 녹음 시간 분석
```
[INFO] EnglishWritingTest: 문장 1 실제 녹음 시간: 3500ms
[INFO] EnglishWritingTest: 문장 2 실제 녹음 시간: 4200ms
[INFO] EnglishWritingTest: 문장 3 실제 녹음 시간: 3800ms
```

## 🛠️ 로그 필터링 방법

### 1. ADB 로그캣 필터링
```bash
# 녹음 관련 로그만 필터링
adb logcat | grep -E "(AudioFileManager|EnglishWritingTest|녹음|병합)"

# 특정 태그만 필터링
adb logcat AudioFileManager:V EnglishWritingTest:V *:S

# 에러 로그만 확인
adb logcat | grep -E "(ERROR|FATAL)"
```

### 2. Android Studio Logcat 필터링
- **Package Name**: `com.na982.opichelper`
- **Log Tag**: `AudioFileManager|EnglishWritingTest`
- **Log Level**: `Info` 이상

## 📊 분석 체크리스트

### ✅ 녹음 파일 생성 확인
- [ ] 각 문장별 녹음 파일이 생성되는가?
- [ ] 파일 크기가 적절한가? (0 bytes가 아닌가?)
- [ ] 파일명이 올바른 형식인가?

### ✅ 파일 병합 확인
- [ ] 병합 과정에서 에러가 발생하지 않는가?
- [ ] 최종 병합 파일이 생성되는가?
- [ ] 병합된 파일 크기가 개별 파일들의 합과 비슷한가?

### ✅ 녹음 시간 분석
- [ ] 각 문장의 녹음 시간이 적절한가?
- [ ] TTS 시간과 실제 녹음 시간이 일치하는가?
- [ ] 너무 짧거나 긴 녹음이 없는가?

## 🚨 문제 해결 가이드

### 1. 녹음 파일이 생성되지 않는 경우
```
[ERROR] AudioFileManager: 녹음 파일 저장 실패
```
- 마이크 권한 확인
- 저장소 공간 확인
- AudioRecorder 상태 확인

### 2. 파일 병합이 실패하는 경우
```
[WARN] AudioFileManager: MediaCodec 병합 실패, 헤더 분석 방식 사용
[WARN] AudioFileManager: 헤더 분석 병합 실패, fallback 방식 사용
```
- 개별 파일들이 올바른 형식인지 확인
- 파일 크기가 0이 아닌지 확인
- MediaCodec 지원 여부 확인

### 3. 녹음 시간이 부정확한 경우
```
[DEBUG] EnglishWritingTest: 문장 1 저장된 TTS 시간 없음 - 문장 길이로 계산: 3000ms
```
- 반복듣기에서 TTS 시간이 저장되었는지 확인
- 문장 길이 계산 로직 확인

## 🔧 로그 설정 변경

### DEBUG 모드 활성화
```kotlin
// AudioFileManagerImpl.kt
private const val DEBUG_MERGE = true
private const val DEBUG_FILE_OPERATIONS = true

// EnglishWritingTestRepositoryImpl.kt  
private const val DEBUG_DETAILED = true
```

### 로그 레벨 변경
```kotlin
// LogFilter.kt
private const val LOG_LEVEL_DEBUG = true
private const val LOG_LEVEL_VERBOSE = true
```

## 📈 성능 모니터링

### 1. 녹음 시간 통계
- 평균 녹음 시간
- 최소/최대 녹음 시간
- 표준 편차

### 2. 파일 크기 통계
- 개별 파일 크기 분포
- 병합 파일 크기
- 파일 압축률

### 3. 처리 시간 통계
- 문장별 처리 시간
- 전체 테스트 완료 시간
- 병합 처리 시간

## 🎯 최적화 권장사항

1. **로그 레벨 조정**: 분석 시에만 DEBUG 모드 활성화
2. **필터링 활용**: 필요한 로그만 선택적으로 확인
3. **정기적 모니터링**: 주기적으로 녹음 품질 확인
4. **자동화**: 로그 분석 스크립트 작성 고려 