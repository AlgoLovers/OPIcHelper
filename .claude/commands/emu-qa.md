---
description: 에뮬레이터에 앱을 띄워 스크린샷으로 UI를 눈으로 검증하는 QA 루프 (라이트/다크 테마 포함)
argument-hint: "[옵션: 확인할 화면/시나리오]"
allowed-tools: Bash, Read
---

# /emu-qa — 에뮬레이터 스크린샷 QA 루프

`verify.sh`는 컴파일·구조까지만 본다. 이 루프는 Compose 화면의 **실제 렌더**(레이아웃 잘림, 인셋, 좌우 비대칭, 다크모드 대비, 폰트 오버플로)를 눈으로 잡는다. Pillow 미리보기가 못 잡는 실기기 버그 전용.

> ⚠️ **오디오/TTS·녹음은 에뮬레이터로 검증 불가 — 실기기가 필요하다.** 이 루프는 **레이아웃/테마 회귀 전용**이다. 재생·녹음 동작 검증에는 쓰지 마라.

확인 요청: $ARGUMENTS

## 부팅 (함정 주의)

```bash
export PATH="$PATH:$HOME/Library/Android/sdk/emulator:$HOME/Library/Android/sdk/platform-tools"
emulator -list-avds                      # 사용 가능한 AVD 확인 후 하나 고른다
nohup emulator -avd <AVD_이름> -no-window -no-audio -no-boot-anim -no-snapshot -gpu host &
adb wait-for-device
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
adb shell settings put global window_animation_scale 0
```

- **`-gpu host` 필수** — swiftshader면 `screencap`이 흰 화면(8KB)만 뱉는다.
- AVD가 없으면 Android Studio Device Manager에서 만들거나 `avdmanager`로 생성.

## 루프

1. `./gradlew -q :app:assembleDebug` → `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. 실행: `adb shell am start -n com.na982.opichelper/.MainActivity`
3. 캡처: `adb exec-out screencap -p > shot.png` → **Read로 눈 확인**.
4. 조작: `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml` → text/bounds 파싱 → `adb shell input tap x y`. 좌표 하드코딩보다 bounds 파싱이 안정적.
5. 문제 발견 → 코드 수정 → 1로.

## 테마 매트릭스 (UI 변경이면 필수)

- **라이트와 다크 둘 다 확인**: `adb shell cmd uimode night yes`(다크) / `no`(라이트).
  "흰 배경 + 흰 글씨" 류 대비 사고는 다크 한 번만 돌려도 잡힌다.
- 이 앱은 표면(surface)·그라데이션·onPrimary 텍스트를 많이 쓰므로 다크에서 대비가 깨지기 쉽다.

## 함정 모음

- **QA JSON(assets) 교체 후엔 `adb shell pm clear com.na982.opichelper`** — 이 앱은 Room에 시딩하므로 `-r` 재설치만으론 DB가 유지돼 재시딩이 안 된다. `pm clear`로 DB를 비워야 새 콘텐츠가 반영된다.
- 스크린샷 좌우 대칭/정렬은 화면을 축소해 보지 말고 원본 해상도로 Read해서 판단.
- 끝나면 `adb emu kill`로 정리.
