## 무엇을 / 왜 (What & Why)

<!-- 이 PR이 바꾸는 것과 이유. 관련 이슈가 있으면: Closes #___ -->

## 관측 가능한 결과 (Observable outcome)

<!-- 사용자가 보는 동작 또는 지표가 무엇이 바뀌는가? "동작함"은 결과가 아니다. -->

## 완료 체크리스트 (CLAUDE.md 준수)

- [ ] `./scripts/claude/verify.sh --full` 통과 (아키텍처 + 컴파일 + 단위 테스트 + Lint)
- [ ] QA 콘텐츠 변경 시 `python3 scripts/claude/validate_qa_json.py --all` 통과 (+ 필요 시 `qa-content-auditor` 검수)
- [ ] TTS 재생 경로 변경 시 stop/speak 경쟁 상태 확인 (필요 시 `arch-reviewer` 리뷰)
- [ ] UI 변경 시 라이트/다크 테마 확인 (`/emu-qa`) 또는 해당 없음
- [ ] 비밀정보 / `local.properties` / keystore / 바이너리 커밋 안 함
- [ ] 한글 커밋 메시지 + 패치 파일(`NNNN-*.patch`) 규약 준수, AI 서명 없음
