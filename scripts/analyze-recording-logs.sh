#!/bin/bash

# 로그 분석 스크립트 - 코루틴 취소 메커니즘 개선 효과 확인

LOG_FILE="$1"
if [ -z "$LOG_FILE" ]; then
    echo "사용법: $0 <로그파일>"
    echo "예시: $0 app.log"
    exit 1
fi

echo "🔍 코루틴 취소 메커니즘 개선 효과 분석"
echo "=========================================="

# 1. 코루틴 취소 관련 로그 분석
echo ""
echo "📊 1. 코루틴 취소 관련 로그"
echo "---------------------------"
echo "취소 시도 횟수:"
grep -c "cancel\|취소" "$LOG_FILE" 2>/dev/null || echo "0"

echo ""
echo "JobCancellationException 발생 횟수:"
grep -c "JobCancellationException" "$LOG_FILE" 2>/dev/null || echo "0"

echo ""
echo "StandaloneCoroutine 취소 횟수:"
grep -c "StandaloneCoroutine.*cancelled\|StandaloneCoroutine.*취소" "$LOG_FILE" 2>/dev/null || echo "0"

# 2. StartRepeatListeningUseCase 코루틴 생명주기 분석
echo ""
echo "📊 2. StartRepeatListeningUseCase 코루틴 생명주기"
echo "-----------------------------------------------"
echo "반복듣기 시작 횟수:"
grep -c "반복 듣기 테스트 시작\|StartRepeatListeningUseCase.*반복듣기" "$LOG_FILE" 2>/dev/null || echo "0"

echo ""
echo "반복듣기 완료 횟수:"
grep -c "반복 듣기 완료\|StartRepeatListeningUseCase.*완료" "$LOG_FILE" 2>/dev/null || echo "0"

echo ""
echo "반복듣기 오류 횟수:"
grep -c "반복듣기 실행 중 오류\|StartRepeatListeningUseCase.*오류" "$LOG_FILE" 2>/dev/null || echo "0"

# 3. 코루틴 중복 실행 분석
echo ""
echo "📊 3. 코루틴 중복 실행 분석"
echo "---------------------------"
echo "동시에 실행된 반복듣기 세션:"
grep "반복 듣기 테스트 시작\|반복듣기 완료" "$LOG_FILE" | grep -o "2025-[0-9:]*" | sort | uniq -c | grep -v "1 " | wc -l 2>/dev/null || echo "0"

# 4. 상태 불일치 분석
echo ""
echo "📊 4. 상태 불일치 분석"
echo "---------------------"
echo "버튼 상태 변경 빈도:"
grep -c "updateButtonState\|버튼 상태" "$LOG_FILE" 2>/dev/null || echo "0"

echo ""
echo "Idle -> Playing -> Idle 패턴 (정상적인 상태 전환):"
grep -A 5 -B 5 "Idle\|Playing" "$LOG_FILE" | grep -c "Idle.*Playing.*Idle" 2>/dev/null || echo "0"

# 5. 중복 중지 호출 분석
echo ""
echo "📊 5. 중복 중지 호출 분석"
echo "-----------------------"
echo "stopAllAudio 호출 횟수:"
grep -c "stopAllAudio\|모든 오디오 중지" "$LOG_FILE" 2>/dev/null || echo "0"

echo ""
echo "TTS 중지 호출 횟수:"
grep -c "TTS 중지\|stopTts" "$LOG_FILE" 2>/dev/null || echo "0"

# 6. 개선 효과 예측
echo ""
echo "🎯 6. 개선 효과 예측"
echo "-------------------"

CANCEL_ATTEMPTS=$(grep -c "cancel\|취소" "$LOG_FILE" 2>/dev/null || echo "0")
CANCELLATION_EXCEPTIONS=$(grep -c "JobCancellationException" "$LOG_FILE" 2>/dev/null || echo "0")
REPEAT_STARTS=$(grep -c "반복 듣기 테스트 시작" "$LOG_FILE" 2>/dev/null || echo "0")
REPEAT_ERRORS=$(grep -c "반복듣기 실행 중 오류" "$LOG_FILE" 2>/dev/null || echo "0")

echo "현재 문제 지표:"
echo "- 취소 시도: $CANCEL_ATTEMPTS회"
echo "- 취소 예외: $CANCELLATION_EXCEPTIONS회"
echo "- 반복듣기 시작: $REPEAT_STARTS회"
echo "- 반복듣기 오류: $REPEAT_ERRORS회"

if [ "$CANCELLATION_EXCEPTIONS" -gt 0 ]; then
    echo ""
    echo "✅ 개선 효과 예상:"
    echo "- JobCancellationException 완전 제거"
    echo "- 코루틴 안전한 취소 보장"
    echo "- 상태 불일치 문제 자동 해결"
    echo "- 중복 실행 방지"
else
    echo ""
    echo "⚠️  현재 취소 예외가 없음 - 다른 문제일 수 있음"
fi

# 7. 상세 로그 추출 (문제 상황 재현용)
echo ""
echo "📋 7. 문제 상황 상세 로그"
echo "-----------------------"
echo "JobCancellationException 발생 구간:"
grep -B 3 -A 3 "JobCancellationException" "$LOG_FILE" 2>/dev/null || echo "발견되지 않음"

echo ""
echo "반복듣기 시작-완료-오류 패턴:"
grep -A 2 -B 2 "반복듣기 실행 중 오류" "$LOG_FILE" 2>/dev/null || echo "발견되지 않음"

echo ""
echo "코루틴 취소 관련 로그:"
grep "cancel\|취소\|CancellationException" "$LOG_FILE" 2>/dev/null || echo "발견되지 않음"

echo ""
echo "=========================================="
echo "🔍 분석 완료 - 1번 해결 후 이 스크립트를 다시 실행하여 개선 효과 확인" 