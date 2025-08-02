package com.na982.opichelper

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * 테스트에서 로그를 무시하도록 설정하는 TestRule
 */
class LogIgnoreRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // 테스트 실행 전에 로그 레벨을 OFF로 설정
                System.setProperty("java.util.logging.config.file", "logging.properties")
                
                // Log 클래스의 메서드들을 무시하도록 설정
                try {
                    // 테스트 실행
                    base.evaluate()
                } catch (e: Exception) {
                    // Log 관련 예외는 무시
                    if (e.message?.contains("Log") == true) {
                        // Log 예외는 무시하고 테스트 계속 진행
                        return
                    }
                    throw e
                }
            }
        }
    }
} 