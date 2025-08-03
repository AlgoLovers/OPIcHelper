#!/bin/bash

# 암기레벨 영작테스트 녹음 파일 로그 분석 스크립트
# 사용법: ./analyze-recording-logs.sh [logfile]

LOG_FILE=${1:-"recording_analysis.log"}
TEMP_FILE="temp_analysis.txt"

echo "🎯 암기레벨 영작테스트 녹음 파일 로그 분석"
echo "=========================================="

# 1. 녹음 파일 생성 분석
echo "📊 1. 녹음 파일 생성 분석"
echo "------------------------"
grep -E "(녹음 파일 저장|saveRecordingFile)" "$LOG_FILE" | \
    awk '{print "📁 " $0}' | \
    head -10

echo ""

# 2. 파일 병합 과정 분석
echo "📊 2. 파일 병합 과정 분석"
echo "------------------------"
grep -E "(병합 시작|병합 완료|MediaCodec 병합|헤더 분석 병합|Fallback 병합)" "$LOG_FILE" | \
    awk '{print "🔗 " $0}' | \
    head -10

echo ""

# 3. 녹음 시간 분석
echo "📊 3. 녹음 시간 분석"
echo "-------------------"
grep -E "(실제 녹음 시간|저장된 TTS 시간)" "$LOG_FILE" | \
    awk '{print "⏱️  " $0}' | \
    head -10

echo ""

# 4. 에러 및 경고 분석
echo "📊 4. 에러 및 경고 분석"
echo "----------------------"
grep -E "(ERROR|FATAL|WARN)" "$LOG_FILE" | \
    awk '{print "⚠️  " $0}' | \
    head -10

echo ""

# 5. 파일 크기 통계
echo "📊 5. 파일 크기 통계"
echo "-------------------"
grep -E "bytes" "$LOG_FILE" | \
    awk '{print "📏 " $0}' | \
    head -10

echo ""

# 6. 영작테스트 진행 상황
echo "📊 6. 영작테스트 진행 상황"
echo "------------------------"
grep -E "(영작 테스트 시작|영작 테스트 완료|문장.*처리)" "$LOG_FILE" | \
    awk '{print "📝 " $0}' | \
    head -10

echo ""

# 7. 통계 요약
echo "📊 7. 통계 요약"
echo "-------------"
echo "총 로그 라인 수: $(wc -l < "$LOG_FILE")"
echo "녹음 관련 로그: $(grep -c "녹음\|recording" "$LOG_FILE")"
echo "병합 관련 로그: $(grep -c "병합\|merge" "$LOG_FILE")"
echo "에러 로그: $(grep -c "ERROR\|FATAL" "$LOG_FILE")"
echo "경고 로그: $(grep -c "WARN" "$LOG_FILE")"

echo ""
echo "🎯 분석 완료! 자세한 내용은 $LOG_FILE 파일을 확인하세요." 