#!/bin/bash
# Stop 훅 — 턴이 끝날 때 커밋 안 된 소스 변경이 남아 있으면 한 줄로 상기시킨다.
# 근거: 이 프로젝트는 "커밋 후 반드시 패치 파일 생성" 규약이라, 턴 끝에 미커밋 상태를
#       놓치면 다음 세션이 이어받을 때 혼란/충돌이 생긴다.
# 주의: 패치 파일(*.patch)은 이 프로젝트에서 의도적으로 untracked로 남기는 산출물이므로
#       카운트에서 제외한다. keystore 류도 제외.
cd "$(dirname "$0")/../.." || exit 0
n=$(git status --porcelain 2>/dev/null | grep -v keystore | grep -vE '\.patch$' | grep -c .)
if [ "$n" -gt 0 ]; then
  printf '{"systemMessage":"🧹 커밋 안 된 변경 %s개 — 마무리하려면 /commit-patch (한글 커밋 + 패치 파일)"}\n' "$n"
fi
