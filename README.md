# OPIC Helper

OPIC 학습을 위한 Android 애플리케이션입니다. MVVM 아키텍처와 Jetpack Compose를 사용하여 개발되었습니다.

## 기능

- OPIC 질문 연습
- 카테고리별 질문 분류
- 난이도별 질문 제공
- 학습 세션 기록

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM (Model-View-ViewModel)
- **의존성 주입**: Hilt (향후 추가 예정)
- **데이터베이스**: Room (향후 추가 예정)
- **네트워킹**: Retrofit (향후 추가 예정)
- **비동기 처리**: Coroutines

## 프로젝트 구조

```
app/src/main/java/com/na982/opichelper/
├── domain/
│   └── entity/
│       ├── Question.kt
│       ├── QuestionCategory.kt
│       ├── QuestionDifficulty.kt
│       └── StudySession.kt
├── presentation/
│   ├── ui/
│   │   ├── screen/
│   │   │   └── MainScreen.kt
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── viewmodel/
│       └── MainViewModel.kt
└── MainActivity.kt
```

## 빌드 및 실행

1. Android Studio에서 프로젝트를 엽니다.
2. Gradle 동기화를 완료합니다.
3. 앱을 실행합니다.

## 개발 환경

- Android Studio Hedgehog | 2023.1.1
- Kotlin 1.9.22
- Compose BOM 2024.10.00
- Minimum SDK: 24
- Target SDK: 36

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 
>>>>>>> b66416a (Initial commit: OPIC Helper Android app with MVVM + Compose)
