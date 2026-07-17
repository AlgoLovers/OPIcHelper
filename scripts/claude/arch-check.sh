#!/usr/bin/env bash
#
# OPIc Helper 아키텍처 규칙 검사기
#
# CLAUDE.md의 "코드 리뷰 체크리스트"를 실제로 실행 가능한 검사로 옮긴 것.
# Claude Code hook(파일 수정 직후)과 /verify 루프 양쪽에서 공유해서 사용한다.
#
# 사용법:
#   ./scripts/claude/arch-check.sh              # 전체 소스 검사
#   ./scripts/claude/arch-check.sh a.kt b.kt    # 지정 파일만 검사
#
# 종료 코드: 0 = 위반 없음(경고는 허용), 1 = 위반 있음

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC_ROOT="$ROOT/app/src/main/java/com/na982/opichelper"
ASSETS_ROOT="$ROOT/app/src/main/assets"

errors=0
warnings=0

# 색상 (파이프로 넘길 땐 비활성)
if [ -t 1 ]; then
    RED=$'\033[0;31m'; YELLOW=$'\033[1;33m'; GREEN=$'\033[0;32m'; NC=$'\033[0m'
else
    RED=''; YELLOW=''; GREEN=''; NC=''
fi

violation() {
    # $1=파일:줄, $2=규칙명, $3=설명
    echo "${RED}[위반]${NC} $1"
    echo "       규칙: $2"
    echo "       $3"
    errors=$((errors + 1))
}

caution() {
    echo "${YELLOW}[경고]${NC} $1"
    echo "       $2"
    warnings=$((warnings + 1))
}

# ── 검사 대상 파일 목록 ────────────────────────────────────────────
kotlin_files=()
json_files=()

if [ $# -gt 0 ]; then
    for f in "$@"; do
        [ -f "$f" ] || continue
        case "$f" in
            *.kt)   kotlin_files+=("$f") ;;
            *.json) json_files+=("$f") ;;
        esac
    done
else
    while IFS= read -r f; do kotlin_files+=("$f"); done \
        < <(find "$SRC_ROOT" -name '*.kt' 2>/dev/null)
    while IFS= read -r f; do json_files+=("$f"); done \
        < <(find "$ASSETS_ROOT" -name '*.json' 2>/dev/null)
fi

# ── 규칙 1: Domain 계층은 Data 계층을 import하지 않는다 (DIP) ──────
# 의존성 방향은 항상 Data → Domain. 역방향은 Clean Architecture 위반.
check_domain_imports_data() {
    local file="$1"
    case "$file" in */domain/*) ;; *) return ;; esac

    local hit
    hit=$(grep -n '^import com\.na982\.opichelper\.data\.' "$file" 2>/dev/null)
    [ -z "$hit" ] && return

    while IFS= read -r line; do
        local lineno symbol
        lineno="${line%%:*}"
        symbol=$(echo "$line" | sed 's/.*import //')
        violation "${file#$ROOT/}:${lineno}" \
            "Domain 계층은 Data 계층을 직접 import할 수 없음 (DIP)" \
            "$symbol 를 import했습니다. 의존성은 Data → Domain 방향이어야 합니다. domain/에 인터페이스를 정의하고 data/에서 구현하세요."
    done <<< "$hit"
}

# ── 규칙 2: Presentation 계층은 Data 계층을 import하지 않는다 ──────
# ViewModel은 Domain(UseCase/Entity/Repository 인터페이스)만 알아야 한다.
check_presentation_imports_data() {
    local file="$1"
    case "$file" in */presentation/*) ;; *) return ;; esac

    local hit
    hit=$(grep -n '^import com\.na982\.opichelper\.data\.' "$file" 2>/dev/null)
    [ -z "$hit" ] && return

    while IFS= read -r line; do
        local lineno symbol
        lineno="${line%%:*}"
        symbol=$(echo "$line" | sed 's/.*import //')
        violation "${file#$ROOT/}:${lineno}" \
            "Presentation 계층은 Data 계층을 직접 import할 수 없음" \
            "$symbol 를 import했습니다. ViewModel은 Domain의 UseCase/Entity/Repository 인터페이스만 사용하고, 구현체 주입은 Hilt(di/)에 맡기세요."
    done <<< "$hit"
}

# ── 규칙 3: Repository 인터페이스에 UI를 끌어들이지 않는다 (ISP) ───
# UI 콜백은 SharedFlow 이벤트로 대체 완료된 상태를 유지한다.
check_repository_no_ui() {
    local file="$1"
    case "$file" in */domain/repository/*) ;; *) return ;; esac

    local hit
    hit=$(grep -n '^import \(androidx\.compose\|android\.view\|android\.widget\)' "$file" 2>/dev/null)
    [ -z "$hit" ] && return

    while IFS= read -r line; do
        local lineno symbol
        lineno="${line%%:*}"
        symbol=$(echo "$line" | sed 's/.*import //')
        violation "${file#$ROOT/}:${lineno}" \
            "Repository 인터페이스는 UI에 의존할 수 없음 (ISP)" \
            "$symbol 를 import했습니다. UI 통지가 필요하면 SharedFlow 이벤트로 노출하세요."
    done <<< "$hit"
}

# ── 규칙 4: 비밀정보 하드코딩 금지 ────────────────────────────────
# 이 프로젝트는 keystore 자격증명을 System.getenv()로 읽는다. 그 방식을 유지한다.
check_hardcoded_secrets() {
    local file="$1"
    local hit
    # 값이 실제로 채워진 리터럴만 탐지.
    # 키워드와 '=' 사이에 타입/게터가 낄 수 있다: val apiKey: String get() = "..."
    # 정상 패턴(System.getenv 폴백, BuildConfig 주입, SharedPreferences 키 상수)은 제외한다.
    hit=$(grep -nEi '(api_?key|password|secret|token|credential)[a-z0-9_]*[^"]*=[[:space:]]*"[^"]{8,}"' "$file" 2>/dev/null \
        | grep -v 'System\.getenv' \
        | grep -v 'BuildConfig\.' \
        | grep -vE '(KEY_|PREF_|_KEY[[:space:]]*=)')
    [ -z "$hit" ] && return

    while IFS= read -r line; do
        local lineno
        lineno="${line%%:*}"
        violation "${file#$ROOT/}:${lineno}" \
            "비밀정보를 코드에 하드코딩할 수 없음" \
            "자격증명은 System.getenv() 또는 BuildConfig로 주입하세요 (app/build.gradle.kts의 signingConfigs 방식 참조)."
    done <<< "$hit"
}

# ── 규칙 5: TTS 이중 stop 호출 주의 (경고) ────────────────────────
# stopWithoutClearingHighlight()는 PlaybackViewModel의 하이라이트 보존 목적으로만
# 존재한다. 새 호출부가 생기면 TTS 엔진 불안정 위험이 있어 사람이 확인해야 한다.
check_tts_stop_usage() {
    local file="$1"
    # 인터페이스 정의부와 구현부, 기존 정당한 호출부는 제외
    case "$file" in
        */domain/audio/TtsPlaybackController.kt) return ;;
        */data/audio/TtsPlaybackControllerImpl.kt) return ;;
        */presentation/viewmodel/PlaybackViewModel.kt) return ;;
    esac

    local hit
    hit=$(grep -n 'stopWithoutClearingHighlight()' "$file" 2>/dev/null)
    [ -z "$hit" ] && return

    while IFS= read -r line; do
        local lineno
        lineno="${line%%:*}"
        caution "${file#$ROOT/}:${lineno}" \
            "stopWithoutClearingHighlight() 새 호출부입니다. 대부분의 경우 stopTts()가 맞습니다 — 이중 stop 호출은 TTS 엔진을 불안정하게 만듭니다. 하이라이트 보존이 꼭 필요한 경우에만 사용하세요."
    done <<< "$hit"
}

# ── 규칙 6: assets JSON 스키마 검증 ───────────────────────────────
# 앱은 assets JSON을 동적으로 로딩한다. 깨진 JSON은 런타임에야 터진다.
check_assets_json() {
    local file="$1"
    case "$file" in *"/assets/"*) ;; *) return ;; esac

    local result
    result=$(python3 "$ROOT/scripts/claude/validate_qa_json.py" "$file" 2>&1)
    local rc=$?
    if [ $rc -ne 0 ]; then
        violation "${file#$ROOT/}" \
            "assets QA JSON 스키마 위반" \
            "$result"
    fi
}

# ── 실행 ──────────────────────────────────────────────────────────
echo "아키텍처 규칙 검사 — Kotlin ${#kotlin_files[@]}개 / JSON ${#json_files[@]}개"
echo ""

for file in ${kotlin_files+"${kotlin_files[@]}"}; do
    check_domain_imports_data "$file"
    check_presentation_imports_data "$file"
    check_repository_no_ui "$file"
    check_hardcoded_secrets "$file"
    check_tts_stop_usage "$file"
done

for file in ${json_files+"${json_files[@]}"}; do
    check_assets_json "$file"
done

# ── 결과 ──────────────────────────────────────────────────────────
if [ $errors -eq 0 ] && [ $warnings -eq 0 ]; then
    echo "${GREEN}통과${NC} — 위반 없음"
    exit 0
fi

echo ""
echo "─────────────────────────────────"
echo "위반 ${errors}건 / 경고 ${warnings}건"

if [ $errors -gt 0 ]; then
    exit 1
fi
exit 0
