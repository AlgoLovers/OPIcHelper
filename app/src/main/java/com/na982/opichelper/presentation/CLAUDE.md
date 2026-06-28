# Presentation Layer — UI, ViewModel, 네비게이션

사용자 인터페이스와 상태 관리. Compose UI + MVVM.
ViewModel은 Domain 계층만 참조, UI는 ViewModel의 StateFlow만 구독.

## 패키지 구조

패키지 구조: [ARCHITECTURE_PRESENTATION.md 섹션 2](.claude/architecture/ARCHITECTURE_PRESENTATION.md)

## viewmodel/ — 상태 관리

ViewModel 의존성/UiState 상세: [ARCHITECTURE_PRESENTATION.md 섹션 4](.claude/architecture/ARCHITECTURE_PRESENTATION.md)

CurrentMode/ModeGroup: [ARCHITECTURE.md 섹션 5](.claude/architecture/ARCHITECTURE.md)

상태 흐름: [ARCHITECTURE_PRESENTATION.md 섹션 5](.claude/architecture/ARCHITECTURE_PRESENTATION.md)

## ui/component/ — 재사용 컴포저블

FlipCard.kt (3D 플립), HighlightText.kt (하이라이트 텍스트), PipOverlay.kt (PiP 오버레이), PipPermissionDialog.kt (PiP 권한), PlayStopToggleButton.kt (재생/정지 토글), OnboardingDialog.kt (온보딩), SearchDialog.kt (검색), EditScriptBottomSheet.kt (스크립트 편집), SectionHeader.kt (섹션 헤더)

## ui/navigation/

AppNavigation.kt — NavHost. Main/Settings/Statistics 3개 라우트. PiP 모드 시 PipOverlay로 전환

## ui/screen/ — 화면

| 파일 | 역할 |
|------|------|
| `MainScreen.kt` | 메인 화면. 6개 ViewModel + MemorizationModeCoordinator 구독 |
| `SettingsScreen.kt` | 설정: 학습 레벨, TTS 속도, 앱 정보 |
| `StatisticsScreen.kt` | 학습 통계 화면 |
| `CategoryLevelRow.kt` | 카테고리/레벨 선택 행 |
| `QuestionActionRow.kt` | 질문 액션 버튼 행 |
| `AnswerSection.kt` | 답변 섹션 (카드 + 버튼) |
| `MainScreenDialogs.kt` | 메인 화면 다이얼로그 모음 |
| `MainScreenSideEffects.kt` | 메인 화면 부수효과 (LaunchedEffect) |
| `MainScreenSnackbarCollector.kt` | 스낵바 수집 (다중 ViewModel 이벤트 병합) |
| `HighlightIndexResolver.kt` | 하이라이트 인덱스 해석 (모드별 분기) |
| `EditScriptState.kt` | 스크립트 편집 상태 |
| `PipInfoSection.kt` | PiP 정보 섹션 |
| `PlaybackSettingsSection.kt` | 재생 설정 섹션 |
| `AppInfoSection.kt` | 앱 정보 섹션 |
| `UserLevelSection.kt` | 사용자 레벨 섹션 |
| `SettingsScreenComponents.kt` | 설정 화면 하위 컴포넌트 |

## ui/screen/MainScreenComponentsUI/ — MainScreen 하위 컴포넌트

AppTitle.kt (앱 타이틀바), CategorySelector.kt (카테고리 드롭다운), MemorizeLevelSelector.kt (암기 모드 선택), QuestionCard.kt (질문 카드), AnswerCard.kt (답변 카드), QuestionPlayButton.kt (질문 재생/정지), AnswerPlayButton.kt (답변 재생/정지), MemorizeLevelPlaybackButton.kt (모드별 동적 재생 버튼), FullMemorizationRecordingButton.kt (통암기 녹음), RecordingAnimation.kt (녹음 중 애니메이션), NavigationSection.kt (이전/다음), NextQuestionButton.kt, PreviousQuestionButton.kt

하이라이트 연동: [ARCHITECTURE_PRESENTATION.md 섹션 7](.claude/architecture/ARCHITECTURE_PRESENTATION.md)

## 아키텍처 규칙
- UI는 ViewModel의 StateFlow만 구독 (`collectAsState()`)
- ViewModel은 Domain 계층만 참조 (Data 직접 import 금지)
- Compose 컴포넌트는 상태 비저장, ViewModel에서 상태 관리
- 재사용 컴포넌트는 `component/`, 화면 전용은 `MainScreenComponentsUI/`
- 로깅은 `AppLogger` 인터페이스 사용 (`android.util.Log` 직접 호출 금지)
