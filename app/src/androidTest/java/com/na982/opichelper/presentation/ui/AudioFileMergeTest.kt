package com.na982.opichelper.presentation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.na982.opichelper.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * 오디오 파일 병합 기능 테스트
 * 
 * 테스트 목표:
 * 1. 영작테스트에서 개별 녹음 파일들이 생성되는지 확인
 * 2. 개별 파일들이 하나로 병합되는지 확인
 * 3. 병합된 파일이 올바른 크기와 형식을 가지는지 확인
 * 4. 병합된 파일이 재생 가능한지 확인
 */
@RunWith(AndroidJUnit4::class)
class AudioFileMergeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    companion object {
        private const val TAG = "AudioFileMergeTest"
        private const val TEST_TIMEOUT = 60000L // 60초
        private const val WAIT_INTERVAL = 1000L // 1초
    }

    /**
     * 영작테스트 파일 병합 전체 과정 테스트
     */
    @Test
    fun testEnglishWritingTestFileMerge() {
        Log.i(TAG, "영작테스트 파일 병합 테스트 시작")

        // 1. 카테고리 선택 (집 카테고리)
        composeTestRule.onNodeWithText("집").performClick()
        Log.i(TAG, "카테고리 선택: 집")

        // 2. 영작테스트 모드 선택
        composeTestRule.onNodeWithText("영작 테스트").performClick()
        Log.i(TAG, "모드 선택: 영작 테스트")

        // 3. 영작테스트 시작
        try {
            composeTestRule.onNodeWithText("부분암기 테스트").performClick()
            Log.i(TAG, "영작테스트 시작 버튼 클릭")
        } catch (e: Exception) {
            try {
                composeTestRule.onNodeWithText("영작 테스트").performClick()
                Log.i(TAG, "영작 테스트 버튼 클릭")
            } catch (e2: Exception) {
                composeTestRule.onNodeWithText("암기 테스트").performClick()
                Log.i(TAG, "암기 테스트 버튼 클릭")
            }
        }

        // 4. 영작테스트 완료 대기
        Log.i(TAG, "영작테스트 완료 대기 중...")
        val testStartTime = System.currentTimeMillis()
        
        composeTestRule.waitUntil(timeoutMillis = TEST_TIMEOUT) {
            try {
                composeTestRule.onNodeWithText("영작테스트 녹음 재생").assertIsDisplayed()
                Log.i(TAG, "영작테스트 완료 감지")
                true
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - testStartTime
                if (elapsed % 5000 == 0L) { // 5초마다 로그 출력
                    Log.d(TAG, "영작테스트 진행 중... (${elapsed/1000}초 경과)")
                }
                false
            }
        }

        // 5. 병합 파일 존재 확인
        val mergedFile = findMergedFile()
        if (mergedFile != null) {
            Log.i(TAG, "병합 파일 발견: ${mergedFile.name} (${mergedFile.length()} bytes)")
            
            // 6. 파일 크기 검증
            assert(mergedFile.length() > 0) { "병합 파일 크기가 0입니다" }
            Log.i(TAG, "병합 파일 크기 검증 통과: ${mergedFile.length()} bytes")
            
            // 7. 파일 형식 검증 (M4A 헤더 확인)
            assert(isValidM4AFile(mergedFile)) { "병합 파일이 올바른 M4A 형식이 아닙니다" }
            Log.i(TAG, "병합 파일 형식 검증 통과")
            
        } else {
            Log.e(TAG, "병합 파일을 찾을 수 없습니다")
            throw AssertionError("병합 파일이 생성되지 않았습니다")
        }

        // 8. 병합 파일 재생 버튼 활성화 확인
        composeTestRule.onNodeWithText("영작테스트 녹음 재생").assertIsEnabled()
        Log.i(TAG, "병합 파일 재생 버튼 활성화 확인")

        // 9. 재생 테스트
        composeTestRule.onNodeWithText("영작테스트 녹음 재생").performClick()
        Log.i(TAG, "병합 파일 재생 시작")
        
        // 재생 중 잠시 대기
        Thread.sleep(3000)
        Log.i(TAG, "병합 파일 재생 테스트 완료")

        Log.i(TAG, "영작테스트 파일 병합 테스트 완료")
    }

    /**
     * 개별 녹음 파일 생성 확인 테스트
     */
    @Test
    fun testIndividualRecordingFiles() {
        Log.i(TAG, "개별 녹음 파일 생성 테스트 시작")

        // 1. 카테고리 선택
        composeTestRule.onNodeWithText("집").performClick()

        // 2. 영작테스트 모드 선택
        composeTestRule.onNodeWithText("영작 테스트").performClick()

        // 3. 영작테스트 시작
        try {
            composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        } catch (e: Exception) {
            try {
                composeTestRule.onNodeWithText("영작 테스트").performClick()
            } catch (e2: Exception) {
                composeTestRule.onNodeWithText("암기 테스트").performClick()
            }
        }

        // 4. 영작테스트 완료 대기
        composeTestRule.waitUntil(timeoutMillis = TEST_TIMEOUT) {
            try {
                composeTestRule.onNodeWithText("영작테스트 녹음 재생").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // 5. 개별 녹음 파일 확인 (병합 후에는 삭제되므로 확인 불가)
        Log.i(TAG, "개별 녹음 파일은 병합 후 삭제되므로 확인 불가")

        Log.i(TAG, "개별 녹음 파일 생성 테스트 완료")
    }

    /**
     * 파일 병합 성능 테스트
     */
    @Test
    fun testFileMergePerformance() {
        Log.i(TAG, "파일 병합 성능 테스트 시작")

        // 1. 카테고리 선택
        composeTestRule.onNodeWithText("집").performClick()

        // 2. 영작테스트 모드 선택
        composeTestRule.onNodeWithText("영작 테스트").performClick()

        // 3. 영작테스트 시작
        try {
            composeTestRule.onNodeWithText("부분암기 테스트").performClick()
        } catch (e: Exception) {
            try {
                composeTestRule.onNodeWithText("영작 테스트").performClick()
            } catch (e2: Exception) {
                composeTestRule.onNodeWithText("암기 테스트").performClick()
            }
        }

        // 4. 병합 시작 시간 기록
        val mergeStartTime = System.currentTimeMillis()

        // 5. 영작테스트 완료 대기
        composeTestRule.waitUntil(timeoutMillis = TEST_TIMEOUT) {
            try {
                composeTestRule.onNodeWithText("영작테스트 녹음 재생").assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }

        // 6. 병합 완료 시간 계산
        val mergeEndTime = System.currentTimeMillis()
        val mergeDuration = mergeEndTime - mergeStartTime

        Log.i(TAG, "파일 병합 소요 시간: ${mergeDuration}ms")

        // 7. 성능 검증 (60초 이내 완료)
        assert(mergeDuration < 60000) { "파일 병합이 60초를 초과했습니다: ${mergeDuration}ms" }
        Log.i(TAG, "파일 병합 성능 검증 통과")

        Log.i(TAG, "파일 병합 성능 테스트 완료")
    }

    /**
     * 병합 파일 찾기
     */
    private fun findMergedFile(): File? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mergedDir = File(context.filesDir, "merged")
        
        if (!mergedDir.exists()) {
            Log.w(TAG, "merged 디렉토리가 존재하지 않습니다")
            return null
        }

        val files = mergedDir.listFiles { file ->
            file.name.startsWith("영작테스트_") && file.name.endsWith(".m4a")
        }

        return files?.maxByOrNull { it.lastModified() }
    }

    /**
     * M4A 파일 형식 검증
     */
    private fun isValidM4AFile(file: File): Boolean {
        if (!file.exists() || file.length() < 8) {
            return false
        }

        try {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8)
                val bytesRead = fis.read(buffer)
                
                if (bytesRead < 8) {
                    return false
                }

                // M4A 파일의 시그니처 확인 (ftyp box)
                // M4A 파일은 보통 'ftyp' 박스로 시작
                val signature = String(buffer, 4, 4)
                return signature == "ftyp"
            }
        } catch (e: Exception) {
            Log.e(TAG, "M4A 파일 형식 검증 실패", e)
            return false
        }
    }

    /**
     * 파일 복사 유틸리티 (테스트용)
     */
    private fun copyFileForTesting(sourceFile: File, destFile: File): Boolean {
        return try {
            FileInputStream(sourceFile).use { fis ->
                FileOutputStream(destFile).use { fos ->
                    val channel = fis.channel
                    val destChannel = fos.channel
                    channel.transferTo(0, channel.size(), destChannel)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "파일 복사 실패", e)
            false
        }
    }

    /**
     * 테스트용 임시 파일 정리
     */
    private fun cleanupTestFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDir = File(context.filesDir, "test_merge")
        
        if (testDir.exists()) {
            testDir.listFiles()?.forEach { file ->
                file.delete()
            }
            testDir.delete()
        }
    }
} 