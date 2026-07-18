package com.na982.opichelper.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.manager.AppLogger
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 실제 assets JSON을 파싱하는 로더의 통합 검증 (Robolectric로 실제 AssetManager 사용).
 * 관용 파싱(필드 누락 항목만 건너뜀)이 정상 콘텐츠를 모두 유효한 QaItem으로 만들고,
 * 각 항목이 구조적으로 온전한지(카테고리·질문·답변·레벨 키) 확인한다.
 * 배포 콘텐츠에 필수 필드 누락/공백이 생기면 이 테스트가 드러낸다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LeveledQaDataLoaderTest {

    private lateinit var loader: LeveledQaDataLoader

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        loader = LeveledQaDataLoader(ctx, mockk<AppLogger>(relaxed = true), Gson())
    }

    private suspend fun assertLevelLoadsValidItems(level: UserLevel, minFileCount: Int) {
        val items = loader.loadQaItemsForLevel(level)

        assertTrue("$level 은 항목이 있어야 함", items.isNotEmpty())
        // 파일당 최소 1항목 → 총 항목 수는 파일 수 이상
        assertTrue("$level 항목 수(${items.size}) >= 파일 수($minFileCount)", items.size >= minFileCount)

        items.forEach { item ->
            assertTrue("카테고리 비어있음: ${item.id}", item.category.isNotBlank())
            assertTrue("영어 질문 비어있음: ${item.id}", item.questionEn.isNotBlank())
            assertTrue("한글 질문 비어있음: ${item.id}", item.questionKo.isNotBlank())
            val answer = item.answers[level]
            assertTrue("레벨 답변 없음: ${item.id}", answer != null)
            assertTrue("영어 답변 비어있음: ${item.id}", answer!!.answerEn.isNotBlank())
            assertTrue("한글 답변 비어있음: ${item.id}", answer.answerKo.isNotBlank())
        }
    }

    @Test
    fun `AL 레벨이 유효한 항목으로 로드된다`() = runTest {
        assertLevelLoadsValidItems(UserLevel.AL, minFileCount = 15)
    }

    @Test
    fun `IH 레벨이 유효한 항목으로 로드된다`() = runTest {
        assertLevelLoadsValidItems(UserLevel.IH, minFileCount = 16)
    }

    @Test
    fun `IH_RAW 레벨이 유효한 항목으로 로드된다`() = runTest {
        assertLevelLoadsValidItems(UserLevel.IH_RAW, minFileCount = 15)
    }

    @Test
    fun `IM 레벨이 유효한 항목으로 로드된다`() = runTest {
        assertLevelLoadsValidItems(UserLevel.IM, minFileCount = 15)
    }
}
