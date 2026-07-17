#!/usr/bin/env bash
#
# PostToolUse hook — 파일이 수정된 직후 아키텍처 규칙을 검사한다.
#
# Claude가 Edit/Write로 파일을 고치면 곧바로 실행되어, 위반이 있으면 exit 2로
# stderr를 Claude에게 되돌려준다. Claude는 그 피드백을 받아 스스로 고친다.
# 사람이 리뷰에서 잡던 것을 편집 시점으로 당기는 게 목적이다.
#
# stdin: {"tool_name":"Edit","tool_input":{"file_path":"..."},...}
# exit 0 = 통과 / exit 2 = 위반을 Claude에게 피드백

set -uo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"

input=$(cat)
file_path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

# 파일 경로가 없는 도구(MultiEdit 등 일부 형태)거나 파일이 사라졌으면 조용히 통과
[ -z "$file_path" ] && exit 0
[ -f "$file_path" ] || exit 0

# 검사 대상이 아닌 파일은 통과
case "$file_path" in
    *.kt) ;;
    */assets/*.json) ;;
    *) exit 0 ;;
esac

# 이 프로젝트 밖의 파일은 검사하지 않음
case "$file_path" in
    "$PROJECT_DIR"/*) ;;
    *) exit 0 ;;
esac

output=$("$PROJECT_DIR/scripts/claude/arch-check.sh" "$file_path" 2>&1)
status=$?

if [ $status -ne 0 ]; then
    {
        echo "방금 수정한 파일이 이 프로젝트의 아키텍처 규칙을 위반합니다."
        echo ""
        echo "$output"
        echo ""
        echo "규칙의 근거는 CLAUDE.md의 '코드 리뷰 체크리스트'입니다. 수정한 뒤 진행하세요."
    } >&2
    exit 2
fi

exit 0
