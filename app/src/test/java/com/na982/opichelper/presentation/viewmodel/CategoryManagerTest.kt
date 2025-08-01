package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.entity.LeveledAnswer
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.usecase.LoadCategoriesUseCase
import com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * CategoryManager 테스트
 * 카테고리 관리 및 QA 아이템 로딩 기능 테스트
 */
class CategoryManagerTest {

    @Mock
    private lateinit var mockQaDataRepository: QaDataRepository

    @Mock
    private lateinit var mockAppStateManager: AppStateManager

    @Mock
    private lateinit var mockLoadCategoriesUseCase: LoadCategoriesUseCase

    @Mock
    private lateinit var mockLoadQaItemsUseCase: LoadQaItemsUseCase

    private lateinit var categoryManager: CategoryManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        categoryManager = CategoryManager(
            qaDataRepository = mockQaDataRepository,
            appStateManager = mockAppStateManager,
            loadCategoriesUseCase = mockLoadCategoriesUseCase,
            loadQaItemsUseCase = mockLoadQaItemsUseCase
        )
    }

    @Test
    fun `카테고리_목록_로드_성공`() = runTest {
        // Given: 카테고리 목록 반환
        val categories = listOf("bank", "restaurant", "transportation")
        whenever(mockLoadCategoriesUseCase()).thenReturn(categories)

        // When: 카테고리 목록 로드
        categoryManager.loadCategories()

        // Then: 카테고리 목록이 정상적으로 로드됨
        val loadedCategories = categoryManager.categories.first()
        assert(loadedCategories == categories)
    }

    @Test
    fun `카테고리_목록_로드_실패`() = runTest {
        // Given: 카테고리 로드 시 예외 발생
        whenever(mockLoadCategoriesUseCase()).thenThrow(RuntimeException("로드 실패"))

        // When: 카테고리 목록 로드
        categoryManager.loadCategories()

        // Then: 에러 상태가 설정됨
        val error = categoryManager.error.first()
        assert(error != null)
        assert(error!!.contains("로드 실패"))
    }

    @Test
    fun `카테고리_변경_성공`() = runTest {
        // Given: 카테고리 변경 및 QA 아이템 로드 성공
        val category = "bank"
        val qaItems = createMockQaItems()
        
        whenever(mockLoadQaItemsUseCase(category)).thenReturn(qaItems)

        // When: 카테고리 변경
        categoryManager.changeCategory(category)

        // Then: 카테고리가 정상적으로 변경됨
        val currentCategory = categoryManager.currentCategory.first()
        assert(currentCategory == category)
        
        // And: QaDataRepository에 카테고리 선택 알림
        verify(mockQaDataRepository).selectCategory(category)
        
        // And: 암기 모드 상태 초기화
        verify(mockAppStateManager).updateMemorizationModeState(
            isRepeatListeningMode = false,
            isEnglishWritingTestMode = false,
            isFullMemorizationMode = false
        )
    }

    @Test
    fun `카테고리_변경_시_기존_작업_중단`() = runTest {
        // Given: 카테고리 변경
        val category = "bank"
        val qaItems = createMockQaItems()
        
        whenever(mockLoadQaItemsUseCase(category)).thenReturn(qaItems)

        // When: 카테고리 변경
        categoryManager.changeCategory(category)

        // Then: TTS 상태가 초기화됨
        verify(mockAppStateManager).resetTtsState()
    }

    @Test
    fun `카테고리_변경_실패`() = runTest {
        // Given: 카테고리 변경 시 예외 발생
        val category = "bank"
        whenever(mockLoadQaItemsUseCase(category)).thenThrow(RuntimeException("변경 실패"))

        // When: 카테고리 변경
        categoryManager.changeCategory(category)

        // Then: 에러 상태가 설정됨
        val error = categoryManager.error.first()
        assert(error != null)
        assert(error!!.contains("변경 실패"))
    }

    @Test
    fun `QA_아이템_로드_성공_시_첫_아이템_설정`() = runTest {
        // Given: QA 아이템 목록 반환
        val category = "bank"
        val qaItems = createMockQaItems()
        
        whenever(mockLoadQaItemsUseCase(category)).thenReturn(qaItems)

        // When: 카테고리 변경
        categoryManager.changeCategory(category)

        // Then: 첫 번째 QA 아이템이 현재 아이템으로 설정됨
        verify(mockAppStateManager).updateQaItemState(
            qaItem = qaItems.first(),
            category = category,
            index = 0,
            totalCount = qaItems.size
        )
    }

    @Test
    fun `빈_QA_아이템_목록_처리`() = runTest {
        // Given: 빈 QA 아이템 목록
        val category = "bank"
        val emptyQaItems = emptyList<QaItem>()
        
        whenever(mockLoadQaItemsUseCase(category)).thenReturn(emptyQaItems)

        // When: 카테고리 변경
        categoryManager.changeCategory(category)

        // Then: QA 아이템 설정이 호출되지 않음
        verify(mockAppStateManager, never()).updateQaItemState(any(), any(), any(), any())
    }

    @Test
    fun `에러_상태_초기화`() = runTest {
        // Given: 에러 상태 설정
        categoryManager.changeCategory("invalid_category")

        // When: 에러 상태 초기화
        categoryManager.clearError()

        // Then: 에러 상태가 null로 초기화됨
        val error = categoryManager.error.first()
        assert(error == null)
    }

    @Test
    fun `현재_카테고리_가져오기`() = runTest {
        // Given: 카테고리 변경
        val category = "bank"
        val qaItems = createMockQaItems()
        
        whenever(mockLoadQaItemsUseCase(category)).thenReturn(qaItems)
        categoryManager.changeCategory(category)

        // When: 현재 카테고리 가져오기
        val currentCategory = categoryManager.getCurrentCategory()

        // Then: 정확한 카테고리 반환
        assert(currentCategory == category)
    }

    @Test
    fun `카테고리_목록_가져오기`() = runTest {
        // Given: 카테고리 목록 로드
        val categories = listOf("bank", "restaurant", "transportation")
        whenever(mockLoadCategoriesUseCase()).thenReturn(categories)
        categoryManager.loadCategories()

        // When: 카테고리 목록 가져오기
        val loadedCategories = categoryManager.getCategories()

        // Then: 정확한 카테고리 목록 반환
        assert(loadedCategories == categories)
    }

    @Test
    fun `로딩_상태_관리`() = runTest {
        // Given: 카테고리 로드 시작
        whenever(mockLoadCategoriesUseCase()).thenReturn(listOf("bank"))

        // When: 카테고리 목록 로드
        categoryManager.loadCategories()

        // Then: 로딩 상태가 true에서 false로 변경됨
        // (실제로는 비동기 처리이므로 테스트에서는 로딩 완료 후 상태 확인)
        val isLoading = categoryManager.isLoading.first()
        assert(!isLoading) // 로딩 완료 후 상태
    }

    @Test
    fun `초기_상태_확인`() = runTest {
        // Given: 초기 상태
        // When: 초기 상태 확인
        val categories = categoryManager.categories.first()
        val currentCategory = categoryManager.currentCategory.first()
        val isLoading = categoryManager.isLoading.first()
        val error = categoryManager.error.first()

        // Then: 초기 상태가 올바르게 설정됨
        assert(categories.isEmpty())
        assert(currentCategory == null)
        assert(!isLoading)
        assert(error == null)
    }

    private fun createMockQaItems(): List<QaItem> {
        return listOf(
            QaItem(
                id = "bank_001",
                category = "bank",
                questionEn = "How can I open a bank account?",
                questionKo = "은행 계좌를 어떻게 열 수 있나요?",
                answers = mapOf(
                    UserLevel.IM to LeveledAnswer(
                        answerEn = "You can open a bank account by visiting a bank branch.",
                        answerKo = "은행 지점을 방문하여 은행 계좌를 열 수 있습니다."
                    )
                )
            ),
            QaItem(
                id = "bank_002",
                category = "bank",
                questionEn = "What documents do I need?",
                questionKo = "어떤 서류가 필요하나요?",
                answers = mapOf(
                    UserLevel.IM to LeveledAnswer(
                        answerEn = "You need your ID and proof of address.",
                        answerKo = "신분증과 주소 증명서가 필요합니다."
                    )
                )
            )
        )
    }
} 