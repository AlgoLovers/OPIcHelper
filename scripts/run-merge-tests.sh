#!/bin/bash

# 파일 병합 테스트 실행 스크립트
# 사용법: ./run-merge-tests.sh [device_id]

DEVICE_ID=${1:-""}
LOG_FILE="merge_test_results.log"

echo "🎯 파일 병합 테스트 실행"
echo "========================"

# 1. 단위 테스트 실행
echo "📊 1. 단위 테스트 실행"
echo "----------------------"
if [ -z "$DEVICE_ID" ]; then
    ./gradlew testDebugUnitTest --tests "*.AudioFileMergeUnitTest" | tee -a "$LOG_FILE"
else
    ./gradlew testDebugUnitTest --tests "*.AudioFileMergeUnitTest" -PdeviceId="$DEVICE_ID" | tee -a "$LOG_FILE"
fi

echo ""

# 2. 통합 테스트 실행
echo "📊 2. 통합 테스트 실행"
echo "----------------------"
if [ -z "$DEVICE_ID" ]; then
    ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.na982.opichelper.presentation.ui.AudioFileMergeTest | tee -a "$LOG_FILE"
else
    ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.na982.opichelper.presentation.ui.AudioFileMergeTest -PdeviceId="$DEVICE_ID" | tee -a "$LOG_FILE"
fi

echo ""

# 3. 기존 병합 파일 테스트 실행
echo "📊 3. 기존 병합 파일 테스트 실행"
echo "-------------------------------"
if [ -z "$DEVICE_ID" ]; then
    ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.na982.opichelper.presentation.ui.EnglishWritingTestMergedFileTest | tee -a "$LOG_FILE"
else
    ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.na982.opichelper.presentation.ui.EnglishWritingTestMergedFileTest -PdeviceId="$DEVICE_ID" | tee -a "$LOG_FILE"
fi

echo ""

# 4. 테스트 결과 요약
echo "📊 4. 테스트 결과 요약"
echo "---------------------"
echo "테스트 로그 파일: $LOG_FILE"
echo "테스트 완료 시간: $(date)"

# 5. 로그 분석
echo ""
echo "📊 5. 로그 분석"
echo "-------------"
if [ -f "$LOG_FILE" ]; then
    echo "총 로그 라인 수: $(wc -l < "$LOG_FILE")"
    echo "성공 메시지: $(grep -c "PASSED\|SUCCESS\|통과" "$LOG_FILE")"
    echo "실패 메시지: $(grep -c "FAILED\|ERROR\|실패" "$LOG_FILE")"
    echo "병합 관련 로그: $(grep -c "병합\|merge" "$LOG_FILE")"
    echo "파일 관련 로그: $(grep -c "파일\|file" "$LOG_FILE")"
else
    echo "로그 파일이 생성되지 않았습니다."
fi

echo ""
echo "🎯 테스트 실행 완료!" 