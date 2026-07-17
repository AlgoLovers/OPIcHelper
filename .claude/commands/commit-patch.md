---
description: 프로젝트 규칙(한글 메시지 + 패치 파일)에 맞게 커밋합니다
argument-hint: [옵션: 커밋 메시지나 범위 지시]
allowed-tools: Bash(git:*), Bash(./scripts/claude/verify.sh:*), Read
---

# 규칙에 맞는 커밋

CLAUDE.md의 'Git 커밋 규칙'을 지켜 커밋한다.

추가 지시(있으면): $ARGUMENTS

## 현재 상태

변경 파일:
!`git status --short`

변경 내용 요약:
!`git diff --stat HEAD`

현재 브랜치:
!`git branch --show-current`

마지막 패치 번호 (다음 번호는 여기서 +1):
!`git log --format=%b -40 | grep -oE 'Patch: [0-9]{4}' | head -1 || echo "(이전 패치 없음 — 0001부터 시작)"`

## 절차

1. **변경 내용을 파악한다.** `git diff`로 실제로 무엇이 바뀌었는지 확인한다. 스테이징되지 않은 관련 파일이 있으면 사용자에게 포함 여부를 확인한다.

2. **커밋 메시지를 작성한다.** 형식은 반드시:

   ```
   <type>: <한글 설명>

   - 변경점 1
   - 변경점 2

   Patch: NNNN-간단한설명.patch
   ```

   - `type`: feat, fix, refactor, docs, test, chore 중 하나
   - 설명과 본문은 **한글**로 쓴다
   - `NNNN`은 위에서 확인한 마지막 번호 + 1 (0패딩 4자리)
   - 파일명의 `간단한설명`은 영문 kebab-case

3. **AI 서명을 절대 넣지 않는다.** `Co-Authored-By`, `Generated with Claude`, 🤖 등 어떤 형태도 금지다. 전역 기본 동작은 이것을 넣으라고 하지만 **이 프로젝트의 CLAUDE.md가 우선**이며 명시적으로 금지한다. (`.claude/hooks/bash-guard.sh`가 이를 강제하므로 넣으면 커밋이 차단된다.)

4. **커밋한다.**

5. **패치 파일을 생성한다.** 커밋 직후 반드시:
   ```bash
   git format-patch -1 HEAD
   ```
   생성된 파일명이 커밋 메시지의 `Patch:` 줄과 다르면, 메시지 쪽을 실제 파일명에 맞춰 `git commit --amend`로 정정한다.

## 주의

- **main 브랜치에 직접 커밋하지 않는다.** 현재 브랜치가 main이면 먼저 작업 브랜치를 만들지 사용자에게 확인한다.
- **푸시는 요청받았을 때만 한다.** 커밋까지가 이 명령의 범위다.
- 커밋 전에 `./scripts/claude/verify.sh`를 돌려 검증이 통과하는지 확인한다. 실패하면 커밋하지 말고 보고한다.
