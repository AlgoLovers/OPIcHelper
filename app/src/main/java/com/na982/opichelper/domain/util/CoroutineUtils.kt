package com.na982.opichelper.domain.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 코루틴 유틸리티 함수들
 * 
 * 클린 아키텍처 원칙:
 * - Domain Layer에서 코루틴 관련 유틸리티 제공
 * - 재사용 가능한 함수로 중복 코드 제거
 * - 성능 최적화된 구현
 * - 안전한 취소 처리
 */
object CoroutineUtils {
    
    /**
     * 코루틴이 취소되었는지 확인하고 취소된 경우 로그 출력
     * 
     * @param tag 로그 태그
     * @param operationName 작업 이름
     * @return 코루틴이 활성 상태이면 true, 취소되었으면 false
     */
    suspend fun checkCancellation(tag: String, operationName: String): Boolean {
        if (!currentCoroutineContext().isActive) {
            Log.d(tag, "코루틴이 취소됨 - $operationName 중단")
            return false
        }
        return true
    }
    
    /**
     * 코루틴이 취소되었는지 확인하고 취소된 경우 예외 발생
     * 
     * @param tag 로그 태그
     * @param operationName 작업 이름
     * @throws CancellationException 코루틴이 취소된 경우
     */
    suspend fun ensureActive(tag: String, operationName: String) {
        if (!currentCoroutineContext().isActive) {
            Log.d(tag, "코루틴이 취소됨 - $operationName 중단")
            throw kotlinx.coroutines.CancellationException("$operationName 작업이 취소됨")
        }
    }
    
    /**
     * 안전한 작업 실행 (타임아웃 포함)
     * 
     * @param tag 로그 태그
     * @param operationName 작업 이름
     * @param timeoutMs 타임아웃 (밀리초)
     * @param block 실행할 블록
     * @return 작업 성공 여부
     */
    suspend fun <T> runSafely(
        tag: String,
        operationName: String,
        timeoutMs: Long = 30000, // 기본 30초
        block: suspend () -> T
    ): T? {
        return try {
            withTimeoutOrNull(timeoutMs) {
                ensureActive(tag, operationName)
                block()
            }
        } catch (e: CancellationException) {
            Log.d(tag, "$operationName 작업이 취소됨")
            null
        } catch (e: Exception) {
            Log.e(tag, "$operationName 작업 중 오류 발생", e)
            null
        }
    }
    
    /**
     * 반복문에서 안전한 코루틴 취소를 확인하는 확장 함수
     * 
     * @param tag 로그 태그
     * @param operationName 작업 이름
     * @param block 실행할 블록
     */
    suspend fun <T> Iterable<T>.forEachWithSafeCancellationCheck(
        tag: String,
        operationName: String,
        block: suspend (T) -> Unit
    ) {
        for (item in this) {
            try {
                ensureActive(tag, operationName)
                block(item)
            } catch (e: CancellationException) {
                Log.d(tag, "$operationName 반복 작업이 취소됨")
                break
            }
        }
    }
    
    /**
     * 작업 실행 중 주기적으로 취소 확인
     * 
     * @param tag 로그 태그
     * @param operationName 작업 이름
     * @param checkIntervalMs 확인 간격 (밀리초)
     * @param block 실행할 블록
     */
    suspend fun <T> runWithPeriodicCancellationCheck(
        tag: String,
        operationName: String,
        checkIntervalMs: Long = 1000, // 기본 1초
        block: suspend () -> T
    ): T? {
        return try {
            withTimeoutOrNull(Long.MAX_VALUE) {
                val job = launch {
                    while (isActive) {
                        ensureActive(tag, operationName)
                        delay(checkIntervalMs)
                    }
                }
                
                try {
                    block()
                } finally {
                    job.cancel()
                }
            }
        } catch (e: CancellationException) {
            Log.d(tag, "$operationName 작업이 취소됨")
            null
        }
    }
    
    /**
     * 안전한 지연 (취소 가능한 delay)
     * 
     * @param tag 로그 태그
     * @param operationName 작업 이름
     * @param delayMs 지연 시간 (밀리초)
     * @return 지연 완료 여부
     */
    suspend fun safeDelay(
        tag: String,
        operationName: String,
        delayMs: Long
    ): Boolean {
        return try {
            withTimeoutOrNull(delayMs) {
                ensureActive(tag, operationName)
                delay(delayMs)
            }
            true
        } catch (e: CancellationException) {
            Log.d(tag, "$operationName 지연이 취소됨")
            false
        }
    }
} 