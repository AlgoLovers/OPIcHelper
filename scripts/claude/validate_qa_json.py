#!/usr/bin/env python3
"""
OPIc Helper assets QA JSON 검증기.

검증 기준은 문서가 아니라 실제 파서다:
  app/src/main/java/com/na982/opichelper/data/repository/LeveledQaDataLoader.kt

그 파일의 QaCategoryAsset / QaItemAsset 데이터 클래스가 계약이다:

    data class QaCategoryAsset(val title: String, val items: List<QaItemAsset>)
    data class QaItemAsset(
        val id: String?,          // nullable — 없으면 "" 로 대체됨
        val question_en: String,  // 이하 non-null
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String,
    )

Gson은 리플렉션으로 객체를 만들기 때문에 Kotlin의 non-null 타입을 강제하지 못한다.
즉 필수 필드가 빠진 JSON도 파싱은 "성공"하고 해당 필드에 null이 들어간 뒤,
한참 뒤 UI에서 NPE로 터진다. 이 검증기가 그 갭을 막는다.

레벨 폴더(al/ih/ih_raw/im)만 앱이 읽는다 — LeveledQaDataLoader.levelFolderMapping 참조.

사용법:
    validate_qa_json.py <file.json> [...]      # 지정 파일 검증
    validate_qa_json.py --all                  # assets 전체 검증
종료 코드: 0 = 정상, 1 = 스키마 위반
"""

import json
import sys
import glob
import os

# LeveledQaDataLoader.levelFolderMapping 과 일치해야 함
KNOWN_LEVEL_FOLDERS = {"al", "ih", "ih_raw", "im"}

# QaItemAsset 의 non-null 필드
REQUIRED_ITEM_FIELDS = ("question_en", "question_ko", "answer_en", "answer_ko")

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS_ROOT = os.path.join(REPO_ROOT, "app", "src", "main", "assets")


def validate_file(path):
    """파일 하나를 검증하고 오류 메시지 리스트를 반환한다."""
    errors = []
    rel = os.path.relpath(path, REPO_ROOT)

    try:
        with open(path, encoding="utf-8") as fh:
            data = json.load(fh)
    except json.JSONDecodeError as e:
        return [f"JSON 파싱 실패 (line {e.lineno}, col {e.colno}): {e.msg}"]
    except OSError as e:
        return [f"파일을 읽을 수 없음: {e}"]

    # 앱이 실제로 읽는 폴더인지 확인
    parent = os.path.basename(os.path.dirname(path))
    if os.path.dirname(os.path.dirname(os.path.abspath(path))) == ASSETS_ROOT:
        if parent not in KNOWN_LEVEL_FOLDERS:
            errors.append(
                f"'{parent}' 폴더는 앱이 로딩하지 않습니다. "
                f"레벨 폴더는 {sorted(KNOWN_LEVEL_FOLDERS)} 뿐입니다 "
                f"(LeveledQaDataLoader.levelFolderMapping). "
                f"새 레벨을 추가하려면 UserLevel enum과 매핑도 함께 수정해야 합니다."
            )

    if not isinstance(data, dict):
        return [f"최상위는 객체여야 합니다 (현재: {type(data).__name__})"]

    # title: String (non-null)
    title = data.get("title")
    if title is None:
        errors.append("'title' 필드가 없습니다. QaItem.category 로 쓰이는 필수 값입니다.")
    elif not isinstance(title, str) or not title.strip():
        errors.append(f"'title' 은 비어있지 않은 문자열이어야 합니다 (현재: {title!r})")

    # items: List<QaItemAsset> (non-null)
    items = data.get("items")
    if items is None:
        return errors + ["'items' 배열이 없습니다."]
    if not isinstance(items, list):
        return errors + [f"'items' 는 배열이어야 합니다 (현재: {type(items).__name__})"]
    if not items:
        errors.append("'items' 가 비어 있습니다. 이 카테고리는 앱에서 빈 목록으로 보입니다.")

    seen_ids = {}
    for idx, item in enumerate(items):
        where = f"items[{idx}]"

        if not isinstance(item, dict):
            errors.append(f"{where}: 객체여야 합니다 (현재: {type(item).__name__})")
            continue

        # non-null 필수 필드 — Gson이 잡아주지 못하는 지점
        for field in REQUIRED_ITEM_FIELDS:
            if field not in item:
                errors.append(
                    f"{where}: '{field}' 필드가 없습니다. "
                    f"Gson은 파싱에 성공하지만 런타임에 null이 되어 NPE로 이어집니다."
                )
            else:
                value = item[field]
                if not isinstance(value, str):
                    errors.append(
                        f"{where}.{field}: 문자열이어야 합니다 (현재: {type(value).__name__})"
                    )
                elif not value.strip():
                    errors.append(f"{where}.{field}: 비어 있습니다.")

        # id: String? — 없어도 되지만, 있으면 문자열이고 중복이 없어야 함
        if "id" in item and item["id"] is not None:
            item_id = item["id"]
            if not isinstance(item_id, str):
                errors.append(
                    f"{where}.id: 문자열이어야 합니다 (현재: {type(item_id).__name__})"
                )
            elif item_id in seen_ids:
                errors.append(
                    f"{where}.id: '{item_id}' 가 items[{seen_ids[item_id]}] 와 중복됩니다. "
                    f"id는 진행상황/녹음 파일의 키로 쓰이므로 중복 시 데이터가 섞입니다."
                )
            else:
                seen_ids[item_id] = idx

    return errors


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__.strip())
        return 2

    if args[0] == "--all":
        paths = sorted(glob.glob(os.path.join(ASSETS_ROOT, "**", "*.json"), recursive=True))
        if not paths:
            print(f"검증할 JSON이 없습니다: {ASSETS_ROOT}")
            return 0
    else:
        paths = args

    total_errors = 0
    bad_files = 0

    for path in paths:
        errors = validate_file(path)
        if errors:
            bad_files += 1
            total_errors += len(errors)
            rel = os.path.relpath(os.path.abspath(path), REPO_ROOT)
            print(f"{rel}")
            for e in errors:
                print(f"  - {e}")

    if total_errors:
        print(f"\n{bad_files}개 파일에서 {total_errors}건의 스키마 위반")
        return 1

    if args[0] == "--all":
        print(f"통과 — QA JSON {len(paths)}개 모두 정상")
    return 0


if __name__ == "__main__":
    sys.exit(main())
