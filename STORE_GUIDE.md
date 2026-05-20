# OPIc Helper — Google Play Store 등록 가이드

## 목차

1. [사전 준비 체크리스트](#1-사전-준비-체크리스트)
2. [릴리스 키스토어 생성](#2-릴리스-키스토어-생성)
3. [Release 빌드](#3-release-빌드)
4. [Google Play Console 등록 절차](#4-google-play-console-등록-절차)
5. [앱 아이콘 교체 가이드](#5-앱-아이콘-교체-가이드)
6. [개인정보처리방침 필수 항목](#6-개인정보처리방침-필수-항목)
7. [출시 후 이슈 대응 가이드](#7-출시-후-이슈-대응-가이드)
8. [출시 전 최종 점검](#8-출시-전-최종-점검)
9. [알려진 제한사항](#9-알려진-제한사항)

---

## 1. 사전 준비 체크리스트

### 필수
- [ ] Google Play 개발자 계정 생성 ($25 일회성 등록비)
- [ ] 릴리스 키스토어 생성
- [ ] release APK/AAB 빌드 성공
- [ ] 앱 아이콘 교체 (현재 기본 안드로이드 아이콘 — **반드시 커스텀 아이콘 필요**)
- [ ] 개인정보처리방침 URL 준비
- [ ] 콘솔 등록 정보 작성 (앱 설명, 스크린샷 등)
- [ ] 흰색 단색 알림 아이콘 준비 (ic_notification.xml — 현재 임시 방패 아이콘, 커스텀 아이콘에 맞게 교체 필요)

### 권장
- [ ] Google Play App Signing 등록
- [ ] 내부 테스트 트랙에서 사전 검증
- [ ] 다양한 화면 크기 기기에서 UI 테스트
- [ ] Android 12+ 기기에서 스플래시 화면 이중 표시 테스트
- [ ] Firebase Crashlytics 또는 유사 크래시 리포팅 SDK 연동

---

## 2. 릴리스 키스토어 생성

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 환경변수 설정
```bash
export KEYSTORE_FILE=release.keystore
export KEYSTORE_PASSWORD=<키스토어 비밀번호>
export KEY_ALIAS=release
export KEY_PASSWORD=<키 비밀번호>
```

### 주의사항
- **keystore 파일 절대 분실 금지** — 분실 시 앱 업데이트 불가
- `.gitignore`에 `*.keystore`, `*.jks` 이미 포함됨
- 백업 복사본을 안전한 곳(클라우드 금고 등)에 보관

---

## 3. Release 빌드

```bash
# 환경변수 설정 후
./gradlew assembleRelease
```

빌드 산출물: `app/build/outputs/apk/release/app-release.apk`

### AAB (Android App Bundle) 빌드 (권장)
```bash
./gradlew bundleRelease
```

빌드 산출물: `app/build/outputs/bundle/release/app-release.aab`

---

## 4. Google Play Console 등록 절차

### 4-1. 앱 만들기
1. Play Console → 앱 만들기
2. 앱 이름: OPIc Helper
3. 기본 언어: 한국어
4. 유료/무료: 무료

### 4-2. 스토어 등록 정보

| 항목 | 내용 |
|------|------|
| 앱 이름 | OPIc Helper |
| 짧은 설명 (80자) | OPIc 영어 말하기 시험 대비 반복 듣기, 영작 테스트, 통암기 학습 앱 |
| 상세 설명 | OPIc 영어 말하기 시험을 효과적으로 준비할 수 있는 학습 도구입니다. 반복 듣기, 부분 암기(영작 테스트), 통암기 3가지 모드로 단계별 학습이 가능합니다. |
| 앱 아이콘 | 512x512 PNG (현재 기본 아이콘 교체 필요) |
| 기능 그래픽 | 1024x500 PNG |
| 스크린샷 | 폰 2~8장, 태블릿 2~8장 (권장) |
| 카테고리 | 교육 |
| 개인정보처리방침 | URL 필수 입력 |

### 4-3. 콘텐츠 등급 (IARC 설문)

IARC 설문 작성 시 주의사항:

| 질문 | 답변 | 비고 |
|------|------|------|
| 광고 포함 여부 | 아니오 | |
| 사용자 정보 수집 | 아니오 | SharedPreferences에 로컬 저장만, 외부 서버 전송 없음 |
| 폭력/성적 콘텐츠 | 없음 | |
| 앱 내 구매 | 없음 | |
| 마이크 접근 | 예 | 녹음 기능 (통암기 모드) |
| 알림 전송 | 예 | TTS 백그라운드 재생 포그라운드 서비스 알림 |

### 4-4. 대상 연령
- 18세 이상 권장 (특별한 제한 없음)

### 4-5. Android 12+ 스플래시 화면 대응

현재 `SplashActivity`가 커스텀 스플래시를 구현하고 있어, Android 12+ 기기에서 시스템 스플래시와 앱 스플래시가 이중으로 표시될 수 있음. 출시 후 `SplashScreen` compat 라이브러리로 마이그레이션 권장.

---

## 5. 앱 아이콘 교체 가이드

현재 `mipmap-*/ic_launcher_foreground.png`가 모든 밀도에서 동일한 크기(108x108px)입니다. **기본 템플릿 아이콘이므로 반드시 교체해야 합니다.**

### 필요한 아이콘 크기

| 밀도 | ic_launcher.png | ic_launcher_foreground.png |
|------|-----------------|---------------------------|
| mdpi | 48x48 | 108x108 |
| hdpi | 72x72 | 162x162 |
| xhdpi | 96x96 | 216x216 |
| xxhdpi | 144x144 | 324x324 |
| xxxhdpi | 192x192 | 432x432 |

### 적응형 아이콘 구조
- `mipmap-anydpi-v26/ic_launcher.xml` — 이미 존재
- 전경: `@mipmap/ic_launcher_foreground`
- 배경: `#2196F3` (파란색)

### 모노크롬 아이콘 (Android 13+ 테마 아이콘)
- `mipmap-anydpi-v26/ic_launcher.xml`에 `monochrome` 속성 추가 필요
- 단색 벡터 드로어블 필요

### 알림 아이콘
- `drawable/ic_notification.xml` — 흰색 단색 벡터 아이콘
- 앱 아이콘 교체 시 이 파일도 함께 업데이트 필요
- Android 5.0+에서 알림 아이콘은 반드시 흰색 단색이어야 함

---

## 6. 개인정보처리방침 필수 항목

### 앱이 수집/저장하는 데이터

| 데이터 | 저장 위치 | 목적 |
|-------|----------|------|
| 학습 레벨 | SharedPreferences (user_prefs) | 사용자 설정 유지 |
| TTS 속도 설정 | SharedPreferences (user_prefs) | 사용자 설정 유지 |
| 암기 레벨 | SharedPreferences (user_prefs) | 통암기 레벨 유지 |
| 반복 듣기 횟수 | SharedPreferences (user_prefs) | 사용자 설정 유지 |
| 답변 재생 횟수 | SharedPreferences (user_prefs) | 사용자 설정 유지 |
| 자동 진행 설정 | SharedPreferences (user_prefs) | 사용자 설정 유지 |
| 온보딩 완료 여부 | SharedPreferences (user_prefs) | 최초 실행 여부 |
| 네비게이션 상태 | SharedPreferences (opic_prefs) | 이어서 듣기 기능 |
| 앱 종료 상태 | SharedPreferences (opic_prefs) | 앱 복원 |
| 카테고리 진행상황 | SharedPreferences (opic_prefs) | 학습 진행 추적 |
| 녹음 파일 | 내부 저장소 (filesDir/recordings) | 통암기 재생 |
| 병합 오디오 파일 | 내부 저장소 (filesDir/merged) | 오디오 병합 재생 |
| 녹음 시간 | SharedPreferences (recording_times) | 적응형 녹음 시간 |

### 수집하지 않는 데이터
- 개인 식별 정보, 위치, 연락처, 사진
- 외부 서버 전송 없음 (INTERNET 권한 미사용)
- 앱 삭제 시 모든 데이터 자동 삭제

### 앱이 사용하는 권한

| 권한 | 목적 | 비고 |
|------|------|------|
| `RECORD_AUDIO` | 통암기 모드 녹음 | 런타임 권한 요청 |
| `WAKE_LOCK` | 화면 꺼짐 상태에서 TTS 재생 | |
| `FOREGROUND_SERVICE` | TTS 백그라운드 재생 | |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | API 34+ 미디어 포그라운드 서비스 | |
| `POST_NOTIFICATIONS` | API 33+ 알림 권한 | 런타임 권한 요청 |

---

## 7. 출시 후 이슈 대응 가이드

### 7-1. 자주 발생 가능한 이슈

| 이슈 | 원인 | 대응 |
|------|------|------|
| TTS가 재생되지 않음 | 기기에 TTS 엔진 미설치 | 설정 → 음성 입력 및 출력 → TTS 엔진 설치 안내 |
| 한글 TTS 음질 저하 | Samsung TTS 미설치 | 설정에서 Samsung TTS 활성화 안내 |
| 백그라운드에서 TTS 중단 | 배터리 최적화 | 앱 설정에서 배터리 최적화 제외 안내 |
| 녹음 권한 거부 | Android 권한 시스템 | 설정 → 앱 → 권한에서 녹음 허용 안내 |
| 알림이 표시되지 않음 | Android 13+ POST_NOTIFICATIONS | 설정 → 앱 → 알림 허용 안내 |
| Android 12+ 이중 스플래시 | 커스텀 SplashActivity | SplashScreen compat 라이브러리 마이그레이션 |

### 7-2. 크래시 대응 프로세스

1. Play Console → 비정상 종료 보고서 확인
2. 난독화된 스택트레이스 해독
   ```bash
   # R8 retrace 사용
   $ANDROID_HOME/tools/retrace app/build/outputs/mapping/release/mapping.txt stacktrace.txt
   ```
3. 원인 파악 후 핫픽스 브랜치에서 수정
4. versionCode 증가 후 release 빌드
5. 출시 트랙에 업로드

### 7-3. ANR 대응

1. Play Console → ANR 보고서 확인
2. traces.txt 분석 — 메인 스레드 블록 원인 파악
3. 주요 원인: 메인 스레드 I/O, TTS 초기화, SharedPreferences 동기 쓰기
4. 코루틴 `Dispatchers.IO` 사용 확인

### 7-4. versionCode 업데이트 규칙

```kotlin
// build.gradle.kts
versionCode = N  // 반드시 이전 버전보다 큰 정수
versionName = "X.Y.Z"  // 의미론적 버전
```

- X: 주요 기능 변경
- Y: 기능 추가
- Z: 버그 수정

### 7-5. ProGuard 매핑 파일 관리

```bash
# 빌드 후 매핑 파일 위치
app/build/outputs/mapping/release/mapping.txt

# 반드시 백업 — 이전 버전의 크래시 스택트레이스 해독에 필요
```

### 7-6. 긴급 출시 중단 / 롤백

치명적 버그 발견 시:
1. Play Console → 프로덕션 → 일시중지
2. 핫픽스 브랜치에서 수정
3. versionCode 증가 후 긴급 업데이트 업로드
4. 스테이지드 롤아웃으로 점진적 배포 (10% → 25% → 50% → 100%)

### 7-7. 스테이지드 롤아웃 전략

| 단계 | 트랙 | 목적 |
|------|------|------|
| 1 | 내부 테스트 | 개발팀 자체 검증 |
| 2 | 비공개 테스트 | 소수 사용자 검증 |
| 3 | 공개 테스트 | 일반 사용자 검증 |
| 4 | 프로덕션 (10%) | 점진적 배포 시작 |
| 5 | 프로덕션 (100%) | 전면 배포 |

### 7-8. Google Play 정책 변경 대응

- 연례 API 레벨 요구사항 변경 확인 (신규 앱: 매년 8월 31일까지 최신 타겟 필요)
- 권한 선언 정책 변경 시 대응
- Play Console 알림 및 개발자 이메일 정기 확인

---

## 8. 출시 전 최종 점검

```bash
# 1. 린트 검사
./gradlew lint

# 2. Debug 빌드
./gradlew assembleDebug

# 3. Release 빌드
./gradlew assembleRelease

# 4. AAB 빌드
./gradlew bundleRelease

# 5. APK 크기 확인
ls -lh app/build/outputs/apk/release/app-release.apk
```

### 기기 테스트 체크리스트

- [ ] 카테고리 선택 → 질문 표시
- [ ] 질문 TTS 재생
- [ ] 답변 TTS 재생
- [ ] 반복듣기 모드 전체 사이클
- [ ] 영작 테스트 모드 전체 사이클
- [ ] 통암기 모드 전체 사이클
- [ ] 화면 꺼짐 상태에서 TTS 재생
- [ ] 설정 화면 동작
- [ ] 앱 종료 후 재실행 시 진행상황 유지
- [ ] 다양한 화면 크기에서 UI 정상 표시
- [ ] Android 12+ 기기에서 스플래시 화면 정상 표시
- [ ] Android 13+ 기기에서 알림 권한 요청 정상 동작
- [ ] Android 14+ 기기에서 포그라운드 서비스 정상 동작

---

## 9. 알려진 제한사항

| 항목 | 내용 |
|------|------|
| 화면 방향 | 세로 모드만 지원 (Android 16에서 무시될 수 있음) |
| TTS 엔진 | 기기에 설치된 엔진에 의존 |
| 한글 TTS | Samsung TTS 권장, 미설치 시 Google TTS 폴백 |
| 녹음 형식 | AAC/M4A (128kbps, 44100Hz) |
| 최소 API | 24 (Android 7.0) |
| 타겟 API | 35 (Android 15) |
| 스플래시 | Android 12+에서 이중 스플래시 가능 (SplashScreen 마이그레이션 필요) |
| 앱 아이콘 | 기본 템플릿 아이콘 (커스텀 아이콘 교체 필요) |
| 모노크롬 아이콘 | Android 13+ 테마 아이콘 미지원 |
