package com.na982.opichelper.data.audio

/**
 * API 키 관리 클래스
 * 실제 API 키는 이 파일에 설정하세요.
 * 
 * 보안을 위해 이 파일을 .gitignore에 추가하는 것을 권장합니다.
 * 
 * ⚠️ 요금 정책 안내:
 * - 네이버 클로바: 유료 API (월 9만원 프리미엄 / 월 1만원 표준)
 * - 카카오 음성: 유료 API (정확한 요금 불명)
 * - 현재 프로젝트에서는 무료 삼성 TTS 사용 권장
 */
object ApiKeys {
    // 네이버 클라우드 플랫폼 API 키 (유료 - 사용하지 않음)
    const val NAVER_CLIENT_ID = "YOUR_NAVER_CLIENT_ID"
    const val NAVER_CLIENT_SECRET = "YOUR_NAVER_CLIENT_SECRET"
    
    // 카카오 개발자 API 키 (유료 - 사용하지 않음)
    const val KAKAO_API_KEY = "YOUR_KAKAO_API_KEY"
} 