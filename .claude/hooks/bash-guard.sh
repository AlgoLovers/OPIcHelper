#!/usr/bin/env bash
#
# PreToolUse hook — Bash 명령이 실행되기 전에 검사해서 위험한 것을 막는다.
#
# 두 종류를 막는다:
#   1. 되돌릴 수 없는 파괴적 명령 (작업 손실, 히스토리 파괴)
#   2. 이 프로젝트의 커밋 규칙 위반 (CLAUDE.md "Git 커밋 규칙")
#
# 특히 Co-Authored-By 차단이 중요하다. 전역 설정은 커밋에 Co-Authored-By를 넣으라고
# 지시하지만 이 프로젝트의 CLAUDE.md는 명시적으로 금지한다. 프로젝트 규칙이 우선이고,
# 이건 습관적으로 틀리기 쉬운 지점이라 사람이 아니라 hook이 지키게 한다.
#
# stdin: {"tool_name":"Bash","tool_input":{"command":"..."},...}
# exit 0 = 허용 / exit 2 = 차단하고 stderr를 Claude에게 전달

set -uo pipefail

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // empty' 2>/dev/null)
[ -z "$cmd" ] && exit 0

block() {
    { echo "차단됨: $1"; echo ""; echo "$2"; } >&2
    exit 2
}

# ── 1. 커밋 메시지에 AI 서명 금지 (CLAUDE.md Git 커밋 규칙) ────────
if printf '%s' "$cmd" | grep -q 'git commit'; then
    if printf '%s' "$cmd" | grep -qiE 'Co-Authored-By|Generated with .*Claude|🤖'; then
        block "커밋 메시지에 AI 서명이 포함되어 있습니다." \
"CLAUDE.md의 'Git 커밋 규칙'은 커밋 메시지에 AI 코딩 관련 내용(Co-Authored-By 등)을
포함하지 않도록 규정합니다. 전역 기본 동작과 반대이므로 주의하세요.

올바른 형식:
    <type>: <한글 설명>

    Patch: 000X-xxx.patch"
    fi
fi

# ── 2. 되돌릴 수 없는 파괴적 명령 ─────────────────────────────────
# rm -rf 로 넓은 범위를 지우는 경우 (프로젝트 루트, 홈, 절대경로 루트)
if printf '%s' "$cmd" | grep -qE 'rm[[:space:]]+(-[a-zA-Z]*[rR][a-zA-Z]*f|-[a-zA-Z]*f[a-zA-Z]*[rR])[[:space:]]'; then
    if printf '%s' "$cmd" | grep -qE 'rm[[:space:]]+-[a-zA-Z]+[[:space:]]+(/|~|\$HOME|\.|\.\.|\*|"?\$\{?CLAUDE_PROJECT_DIR)[[:space:]]*$|rm[[:space:]]+-[a-zA-Z]+[[:space:]]+(/|~/|\$HOME/)[^[:space:]]*[[:space:]]*$'; then
        block "광범위한 재귀 삭제입니다: $cmd" \
"프로젝트 루트나 홈 디렉토리를 통째로 지우려 하고 있습니다.
정말 필요하다면 구체적인 하위 경로를 지정하세요. 빌드 산출물 정리는 ./gradlew clean 을 쓰세요."
    fi
fi

# git 히스토리/작업 파괴
if printf '%s' "$cmd" | grep -qE 'git[[:space:]]+push[[:space:]]+.*(--force([^-]|$)|-f([[:space:]]|$))'; then
    if ! printf '%s' "$cmd" | grep -q -- '--force-with-lease'; then
        block "강제 푸시입니다: $cmd" \
"원격 히스토리를 덮어써 다른 커밋을 잃을 수 있습니다.
꼭 필요하면 --force-with-lease 를 쓰고, main 브랜치에는 하지 마세요."
    fi
fi

if printf '%s' "$cmd" | grep -qE 'git[[:space:]]+reset[[:space:]]+--hard'; then
    block "git reset --hard 입니다: $cmd" \
"커밋되지 않은 작업이 전부 사라집니다.
변경을 잠시 치워두려면 git stash 를, 특정 파일만 되돌리려면 git checkout -- <file> 을 쓰세요."
fi

if printf '%s' "$cmd" | grep -qE 'git[[:space:]]+clean[[:space:]]+-[a-zA-Z]*[fd]'; then
    block "git clean 입니다: $cmd" \
"추적되지 않는 파일이 삭제됩니다. local.properties 나 keystore 처럼 복구 불가능한 파일이
날아갈 수 있습니다. 먼저 git clean -n 으로 무엇이 지워질지 확인하세요."
fi

# ── 3. 서명 자격증명 노출 방지 ────────────────────────────────────
# 이 프로젝트는 keystore 자격증명을 환경변수로 읽는다 (app/build.gradle.kts signingConfigs).
# 그 값을 출력하거나 keystore를 복사/삭제하는 명령을 막는다.
if printf '%s' "$cmd" | grep -qE '(echo|printf|cat)[^|;&]*\$(KEYSTORE_PASSWORD|KEY_PASSWORD|KEYSTORE_FILE|KEY_ALIAS)'; then
    block "서명 자격증명을 출력하려 합니다: $cmd" \
"keystore 비밀번호는 출력하면 세션 로그에 평문으로 남습니다.
설정 여부만 확인하려면: [ -n \"\$KEYSTORE_PASSWORD\" ] && echo '설정됨'"
fi

if printf '%s' "$cmd" | grep -qE '(rm|mv|cp)[[:space:]]+[^|;&]*\.(keystore|jks)([[:space:]]|$)'; then
    block "keystore 파일을 조작하려 합니다: $cmd" \
"릴리스 keystore를 잃으면 앱 업데이트를 영영 게시할 수 없습니다 (Play 스토어는 동일 서명을 요구).
정말 필요하면 직접 실행하세요."
fi

exit 0
