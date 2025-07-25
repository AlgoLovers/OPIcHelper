#!/bin/bash

# OPicHelper 테스트 실행 스크립트
# 사용법: ./scripts/run-tests.sh [option]
# 옵션:
#   unit      - 단위 테스트만 실행
#   instrumented - Instrumented 테스트만 실행
#   all       - 모든 테스트 실행 (기본값)
#   coverage  - 커버리지와 함께 테스트 실행

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 테스트 결과 요약 함수
print_test_summary() {
    local test_type=$1
    local result=$2
    
    if [ $result -eq 0 ]; then
        log_success "$test_type 테스트가 성공적으로 완료되었습니다!"
    else
        log_error "$test_type 테스트가 실패했습니다!"
        exit $result
    fi
}

# 단위 테스트 실행
run_unit_tests() {
    log_info "단위 테스트를 실행합니다..."
    
    # 테스트 디렉토리 확인
    if [ ! -d "app/src/test" ]; then
        log_error "테스트 디렉토리를 찾을 수 없습니다: app/src/test"
        return 1
    fi
    
    # 단위 테스트 실행
    ./gradlew test --info
    
    print_test_summary "단위" $?
}

# Instrumented 테스트 실행
run_instrumented_tests() {
    log_info "Instrumented 테스트를 실행합니다..."
    
    # 테스트 디렉토리 확인
    if [ ! -d "app/src/androidTest" ]; then
        log_error "Instrumented 테스트 디렉토리를 찾을 수 없습니다: app/src/androidTest"
        return 1
    fi
    
    # Instrumented 테스트 실행
    ./gradlew connectedAndroidTest --info
    
    print_test_summary "Instrumented" $?
}

# 커버리지와 함께 테스트 실행
run_tests_with_coverage() {
    log_info "커버리지와 함께 테스트를 실행합니다..."
    
    # 커버리지 설정 확인
    if ! grep -q "jacoco" app/build.gradle.kts; then
        log_warning "JaCoCo 커버리지 플러그인이 설정되지 않았습니다."
        log_info "app/build.gradle.kts에 JaCoCo 플러그인을 추가하세요."
    fi
    
    # 커버리지와 함께 테스트 실행
    ./gradlew test jacocoTestReport --info
    
    print_test_summary "커버리지" $?
    
    # 커버리지 리포트 위치 안내
    if [ -f "app/build/reports/jacoco/test/html/index.html" ]; then
        log_info "커버리지 리포트: app/build/reports/jacoco/test/html/index.html"
    fi
}

# 모든 테스트 실행
run_all_tests() {
    log_info "모든 테스트를 실행합니다..."
    
    # 단위 테스트 실행
    run_unit_tests
    
    # Instrumented 테스트 실행 (에뮬레이터가 있는 경우)
    if command -v adb &> /dev/null && adb devices | grep -q "emulator"; then
        run_instrumented_tests
    else
        log_warning "ADB 또는 에뮬레이터가 감지되지 않아 Instrumented 테스트를 건너뜁니다."
        log_info "Instrumented 테스트를 실행하려면 Android 에뮬레이터를 실행하세요."
    fi
    
    log_success "모든 테스트가 완료되었습니다!"
}

# 메인 함수
main() {
    local option=${1:-all}
    
    log_info "OPicHelper 테스트 실행을 시작합니다..."
    
    # Gradle 래퍼 확인
    if [ ! -f "gradlew" ]; then
        log_error "Gradle 래퍼를 찾을 수 없습니다. 프로젝트 루트 디렉토리에서 실행하세요."
        exit 1
    fi
    
    # 실행 권한 부여
    chmod +x gradlew
    
    case $option in
        "unit")
            run_unit_tests
            ;;
        "instrumented")
            run_instrumented_tests
            ;;
        "coverage")
            run_tests_with_coverage
            ;;
        "all")
            run_all_tests
            ;;
        *)
            log_error "잘못된 옵션입니다: $option"
            echo "사용법: $0 [unit|instrumented|coverage|all]"
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@" 