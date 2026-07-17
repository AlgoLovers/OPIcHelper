---
description: OPIc 학습 문항(QA)을 assets에 추가하거나 수정합니다
argument-hint: <레벨> <카테고리> [문항 내용]
allowed-tools: Bash(python3 scripts/claude/validate_qa_json.py:*), Bash(ls:*), Bash(find:*), Read, Edit, Write, Glob
---

# QA 문항 추가/수정

요청: $ARGUMENTS

## 현재 assets 구조

!`ls app/src/main/assets/`

레벨별 카테고리 수:
!`for d in app/src/main/assets/*/; do printf "%-10s %s개\n" "$(basename $d)" "$(ls $d/*.json 2>/dev/null | wc -l | tr -d ' ')"; done`

## JSON 포맷 (실제 파서 기준)

계약은 `data/repository/LeveledQaDataLoader.kt`의 `QaCategoryAsset` / `QaItemAsset`이다.
**루트 CLAUDE.md의 JSON 포맷 설명은 실제와 다르므로 신뢰하지 말 것** — 아래가 실제 포맷이다:

```json
{
  "title": "한글 카테고리명",
  "items": [
    {
      "id": "1",
      "question_en": "영어 질문",
      "question_ko": "한글 질문",
      "answer_en": "영어 모범답안",
      "answer_ko": "한글 번역"
    }
  ]
}
```

- 레벨은 **폴더**로 결정된다: `al`, `ih`, `ih_raw`, `im` (`LeveledQaDataLoader.levelFolderMapping`). 항목 안에 레벨 필드는 없다.
- `title`이 앱에 표시되는 카테고리명이 된다.
- `question_en`, `question_ko`, `answer_en`, `answer_ko`는 **모두 필수**다. Gson이 non-null Kotlin 필드를 강제하지 못해서, 빠뜨리면 파싱은 성공하고 나중에 런타임에 터진다.
- `id`는 진행상황과 녹음 파일의 키다. 파일 안에서 중복되면 안 된다.
- `theme`, `category` 필드는 일부 파일에 있지만 파서가 무시한다. 새로 넣을 이유가 없다.
- 새 JSON은 assets에 넣기만 하면 자동 인식된다 — 코드 수정 불필요.

## 절차

1. **기존 파일을 먼저 읽는다.** 같은 카테고리가 이미 있으면 새로 만들지 말고 거기에 항목을 추가한다. `id`는 기존 최대값 + 1로 정한다.
2. 문항을 작성/수정한다.
3. **반드시 검증한다:**
   ```bash
   python3 scripts/claude/validate_qa_json.py <수정한파일>
   ```
   통과할 때까지 고친다.

## 학습 콘텐츠에 대한 원칙

이 앱은 사용자가 **실제 OPIc 시험을 준비하려고** 만든 것이다. 콘텐츠의 정확성이 곧 앱의 가치다.

- 영어 답안은 해당 레벨(AL / IH / IM)에 맞는 실제 시험 수준으로 쓴다. 레벨을 넘나드는 답안은 학습에 해롭다.
- 한글 번역은 영어 답안과 의미가 일치해야 한다.
- **불확실하면 지어내지 말고 사용자에게 묻는다.** 그럴듯한 오답은 아무 답도 없는 것보다 나쁘다 — 사용자가 틀린 표현을 외우게 된다.
