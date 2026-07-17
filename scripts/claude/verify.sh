#!/usr/bin/env bash
#
# OPIc Helper 통합 검증 — 루프 프로그래밍의 엔진.
#
# 빠르고 값싼 검사부터 실행해서 최대한 일찍 실패한다(fail fast).
# 아키텍처 위반은 몇 초 만에 잡히는데 굳이 몇 분짜리 Gradle 빌드를 먼저 돌릴 이유가 없다.
#
#   1단계  아키텍처 규칙 + assets JSON   (~2초)
#   2단계  Kotlin 컴파일                  (~수십초~수분)
#   3단계  Android Lint                   (느림, --full 에서만)
#
# 사용법:
#   ./scripts/claude/verify.sh           # 1+2단계 (기본, 루프용)
#   ./scripts/claude/verify.sh --fast    # 1단계만 (편집 중 빠른 확인)
#   ./scripts/claude/verify.sh --full    # 1+2+3단계 (커밋/PR 전)
#
# 종료 코드: 0 = 전부 통과, 1 = 실패 (어느 단계에서 실패했는지 출력)

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT" || exit 1

MODE="${1:-normal}"

if [ -t 1 ]; then
    RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; BLUE=$'\033[0;34m'; BOLD=$'\033[1m'; NC=$'\033[0m'
else
    RED=''; GREEN=''; BLUE=''; BOLD=''; NC=''
fi

step() { echo ""; echo "${BLUE}${BOLD}[$1/$2] $3${NC}"; }
fail() {
    echo ""
    echo "${RED}${BOLD}검증 실패 — $1${NC}"
    echo ""
    echo "$2"
    exit 1
}

case "$MODE" in
    --fast) total=1 ;;
    --full) total=3 ;;
    *)      total=2 ;;
esac

# ── Android SDK 위치 확보 ─────────────────────────────────────────
# Android Studio는 SDK 위치를 자체적으로 알지만 CLI의 gradlew는 모른다.
# local.properties(개인 파일, gitignore됨)도 ANDROID_HOME도 없으면 빌드가
# "SDK location not found"로 실패한다. 흔한 설치 경로를 탐지해서 메워준다.
ensure_android_sdk() {
    [ -f "$ROOT/local.properties" ] && grep -q '^sdk\.dir=' "$ROOT/local.properties" && return 0
    [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME:-}" ] && return 0
    [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT:-}" ] && {
        export ANDROID_HOME="$ANDROID_SDK_ROOT"; return 0
    }

    local candidate
    for candidate in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" \
                     "/usr/local/share/android-sdk" "/opt/android-sdk"; do
        if [ -d "$candidate/platforms" ]; then
            export ANDROID_HOME="$candidate"
            echo "Android SDK 자동 감지: $candidate"
            return 0
        fi
    done

    fail "Android SDK를 찾을 수 없음" \
"gradlew가 SDK 위치를 모릅니다. 다음 중 하나를 하세요:

  1. local.properties 생성 (권장 — 개인 파일이고 gitignore됩니다):
       echo \"sdk.dir=\$HOME/Library/Android/sdk\" > local.properties

  2. 환경변수 설정:
       export ANDROID_HOME=\"\$HOME/Library/Android/sdk\"

Android Studio에서는 IDE가 SDK 위치를 자체적으로 알기 때문에 빌드가 되지만,
CLI 빌드와 CI는 위 설정이 있어야 동작합니다."
}

echo "${BOLD}OPIc Helper 검증 (${MODE})${NC}"

# ── 1단계: 아키텍처 규칙 + assets JSON ────────────────────────────
step 1 $total "아키텍처 규칙 & assets JSON"

arch_out=$(./scripts/claude/arch-check.sh 2>&1)
arch_rc=$?
echo "$arch_out"
if [ $arch_rc -ne 0 ]; then
    fail "아키텍처 규칙 위반" \
"위에 나온 위반을 고치세요. 근거는 CLAUDE.md의 '코드 리뷰 체크리스트'입니다.
컴파일은 통과해도 이건 설계 계약 위반이라 리뷰에서 반드시 걸립니다."
fi

[ "$MODE" = "--fast" ] && { echo ""; echo "${GREEN}${BOLD}통과${NC} (빠른 검사)"; exit 0; }

# ── 2단계: 컴파일 ─────────────────────────────────────────────────
step 2 $total "Kotlin 컴파일 (./gradlew assembleDebug)"

ensure_android_sdk

build_log=$(mktemp)
trap 'rm -f "$build_log"' EXIT

if ./gradlew assembleDebug --console=plain -q > "$build_log" 2>&1; then
    echo "${GREEN}컴파일 성공${NC}"
else
    # Gradle 로그는 길다. Claude가 바로 쓸 수 있게 에러 줄만 추린다.
    errors=$(grep -E '^e:|error:|FAILURE:|Caused by:|Unresolved reference|Compilation error' "$build_log" | head -40)
    [ -z "$errors" ] && errors=$(tail -40 "$build_log")
    fail "컴파일 오류" "$errors

전체 로그: $build_log"
fi

[ "$MODE" != "--full" ] && { echo ""; echo "${GREEN}${BOLD}통과${NC} — 아키텍처 + 컴파일"; exit 0; }

# ── 3단계: Android Lint ───────────────────────────────────────────
step 3 $total "Android Lint (./gradlew lint)"

lint_log=$(mktemp)
trap 'rm -f "$build_log" "$lint_log"' EXIT

if ./gradlew lint --console=plain -q > "$lint_log" 2>&1; then
    echo "${GREEN}Lint 통과${NC}"
else
    errors=$(grep -E 'Error:|error:|FAILURE:' "$lint_log" | head -30)
    [ -z "$errors" ] && errors=$(tail -30 "$lint_log")
    fail "Lint 오류" "$errors

리포트: app/build/reports/lint-results-debug.html"
fi

echo ""
echo "${GREEN}${BOLD}전부 통과${NC} — 아키텍처 + 컴파일 + Lint"
