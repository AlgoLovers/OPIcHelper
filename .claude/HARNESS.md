# 개발 하네스 & 루프

Claude Code로 이 프로젝트를 개발할 때 쓰는 자동 검증 환경.

## 왜 필요한가

CLAUDE.md에는 "Domain은 Data를 import하지 않는다" 같은 규칙이 문장으로 적혀 있었다. 문장은 지켜지지 않는다 — 사람도 AI도 잊는다. 하네스는 그 규칙을 **실행 가능한 검사**로 옮겨서, 위반이 리뷰까지 가지 않고 편집 시점에 잡히게 한다.

루프는 그 검사를 신호 삼아 통과할 때까지 고치는 것이다. 신호가 없으면 루프도 없다.

## 구성

```
.claude/
  settings.json            권한 + hook 등록
  hooks/
    arch-guard.sh          파일 수정 직후 아키텍처 검사 (PostToolUse)
    bash-guard.sh          위험 명령 / 커밋 규칙 위반 차단 (PreToolUse)
  commands/
    fix-loop.md            /fix-loop  — 검증 통과까지 자동 수정 루프
    add-qa.md              /add-qa    — 학습 문항 추가
    commit-patch.md        /commit-patch — 규칙 준수 커밋
  agents/
    arch-reviewer.md       자동 검사가 못 잡는 것을 리뷰하는 서브에이전트

scripts/claude/
  verify.sh                통합 검증 (루프 엔진)
  arch-check.sh            아키텍처 규칙 검사기
  validate_qa_json.py      assets QA JSON 스키마 검증기
```

`arch-check.sh`가 검사 엔진이고, hook과 `verify.sh`가 그것을 공유한다. 규칙을 고칠 곳은 한 군데뿐이다.

## 검증 단계

`verify.sh`는 값싼 검사부터 실행해 일찍 실패한다. 아키텍처 위반은 2초면 잡히는데 몇 분짜리 Gradle 빌드를 먼저 돌릴 이유가 없다.

| 단계 | 내용 | 시간 | 실행 조건 |
|------|------|------|-----------|
| 1 | 아키텍처 규칙 + assets JSON | ~2초 | 항상 |
| 2 | Kotlin 컴파일 (`assembleDebug`) | 수십초~ | 기본, `--full` |
| 3 | Android Lint | 느림 | `--full` |

```bash
./scripts/claude/verify.sh --fast   # 1단계만 — 편집 중 빠른 확인
./scripts/claude/verify.sh          # 1+2 — 루프 기본값
./scripts/claude/verify.sh --full   # 1+2+3 — 커밋/PR 전
```

### Android SDK 자동 탐지

Android Studio는 SDK 위치를 자체적으로 알지만 CLI의 `gradlew`는 모른다. `local.properties`에 `sdk.dir`가 없고 `ANDROID_HOME`도 없으면 빌드가 `SDK location not found`로 실패한다.

`verify.sh`는 흔한 설치 경로(`~/Library/Android/sdk` 등)를 탐지해서 이 구멍을 메운다. 다만 `./gradlew`를 **직접** 호출할 때는 적용되지 않으므로, 확실히 하려면:

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" >> local.properties   # 개인 파일, gitignore됨
```

## 검사 규칙

`arch-check.sh`가 검사하는 것. 근거는 전부 CLAUDE.md의 '코드 리뷰 체크리스트'다.

| 규칙 | 수준 | 근거 |
|------|------|------|
| Domain → Data import 금지 | 위반 | Clean Architecture DIP. 의존성은 Data → Domain 방향 |
| Presentation → Data import 금지 | 위반 | ViewModel은 Domain만 알아야 한다 |
| Repository 인터페이스에 UI import 금지 | 위반 | ISP. UI 통지는 SharedFlow로 |
| 비밀정보 하드코딩 금지 | 위반 | keystore 자격증명은 `System.getenv()`로 |
| assets QA JSON 스키마 | 위반 | Gson이 non-null을 강제 못 해 런타임 NPE가 된다 |
| `stopWithoutClearingHighlight()` 새 호출부 | 경고 | 이중 stop은 TTS 엔진을 불안정하게 만든다 |

**의도적으로 검사하지 않는 것:**

- `data/`가 `domain/`의 **인터페이스**를 import하는 것 — 정상적인 DIP 구현이다 (`MemorizationModeCoordinatorImpl` 등). 구현체 import만 문제인데 이름만으로 구분되지 않아 오탐이 난다. `arch-reviewer` 에이전트의 몫이다.
- 코드 스타일/포맷 — 이 프로젝트엔 포맷터가 없고, 그건 의도된 선택이다. 149개 파일에 ktlint를 소급 적용하면 diff가 오염된다.

규칙은 전부 **기존 149개 Kotlin 파일과 61개 JSON에 대해 오탐 0**을 확인하고 도입했다. 오탐이 나는 검사는 곧 무시당하고, 무시당하는 검사는 없느니만 못하다.

## 루프 사용법

```
/fix-loop
```

`verify.sh`를 돌려서 실패하면 **하나씩** 고치고 다시 검증하기를 통과할 때까지 반복한다.

루프의 규칙:

- **원인을 고친다. 검사를 고치지 않는다.** `scripts/claude/`를 수정해 통과시키는 건 부정행위다.
- **같은 실패 3회면 멈춘다.** 접근이 틀렸다는 뜻이므로 사람에게 판단을 넘긴다.
- **학습 콘텐츠는 지어내지 않는다.** `answer_en`이 비었다고 영어 답안을 창작하면 사용자가 틀린 표현을 외운다. 그럴듯한 오답은 무답보다 나쁘다.

주기적으로 돌리려면 내장 `/loop`와 조합할 수 있다:

```
/loop 10m /fix-loop
```

## hook 동작

**파일 수정 직후** (`arch-guard.sh`): `.kt` 또는 `assets/*.json`을 수정하면 그 파일만 검사한다. 위반이면 exit 2로 진단을 Claude에게 되돌려주고, Claude가 스스로 고친다. 무관한 파일은 조용히 통과한다.

**Bash 실행 전** (`bash-guard.sh`): 다음을 차단한다.

- 커밋 메시지의 AI 서명 (`Co-Authored-By`, 🤖 등) — **전역 기본 설정은 이걸 넣으라고 하지만 이 프로젝트 CLAUDE.md는 금지한다.** 습관적으로 틀리기 쉬워서 hook이 강제한다.
- `rm -rf ~`, `rm -rf /` 같은 광범위 삭제
- `git reset --hard`, `git clean -fd`, `--force` 푸시 (`--force-with-lease`는 허용)
- keystore 자격증명 출력, keystore 파일 조작 — 릴리스 keystore를 잃으면 앱 업데이트를 영영 못 올린다

정상 작업(`./gradlew assembleDebug`, `rm -rf app/build`, `git push origin main` 등)은 통과한다.

## 권한 설정

`settings.json`의 `allow`는 읽기 전용이거나 되돌릴 수 있는 명령만 담는다 (gradle 빌드/테스트, git 조회, 검증 스크립트). `deny`는 keystore·`local.properties`·`google-services.json` 읽기를 막는다.

개인 설정이 필요하면 `.claude/settings.local.json`에 둔다 (gitignore됨).

## 한계

- **단위 테스트는 도메인 순수 로직 위주로만 있다.** `app/src/test`에 `SentenceSplitterTest`, `MemorizeLevelTest`, `ScriptProgressTest`, `ValidateScriptEditUseCaseTest`가 있고 `verify.sh`(기본/`--full`)가 `testDebugUnitTest`로 실행한다. 다만 **ViewModel·TTS 오케스트레이션 등 상태·동작 계층은 아직 단위 테스트가 얇다** — 커버리지 확대 여지가 크다. mockk·coroutines-test 의존성은 선언돼 있으니 활용하면 된다.
- 계측(UI) 테스트는 에뮬레이터가 필요해 verify 루프에 넣지 않았다. UI 시각 검증은 `/emu-qa` 커맨드(라이트/다크 스크린샷 루프)로 대체한다. 단, 오디오/TTS·녹음은 실기기가 필요하다.
- Lint는 느려서 기본 단계에서 뺐다.
