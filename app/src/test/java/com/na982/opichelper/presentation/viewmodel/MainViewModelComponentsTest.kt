package com.na982.opichelper.presentation.viewmodel

import com.na982.opichelper.domain.entity.QaItem
import org.junit.Assert.*
import org.junit.Test

class MainViewModelComponentsTest {

    private val testItemsByCategory = mapOf(
        "가족/친구" to listOf(
            QaItem(
                id = "1",
                category = "가족/친구",
                questionEn = "What is your family like?",
                questionKo = "당신의 가족은 어떤가요?",
                answerEn = "My family is very close-knit and supportive.",
                answerKo = "제 가족은 매우 화목하고 서로를 지원해줍니다."
            ),
            QaItem(
                id = "2",
                category = "가족/친구",
                questionEn = "How often do you see your friends?",
                questionKo = "친구들을 얼마나 자주 만나나요?",
                answerEn = "I see my friends at least once a week.",
                answerKo = "저는 친구들을 최소 일주일에 한 번씩 만납니다."
            )
        ),
        "패션" to listOf(
            QaItem(
                id = "3",
                category = "패션",
                questionEn = "What is your favorite style of clothing?",
                questionKo = "어떤 스타일의 옷을 좋아하나요?",
                answerEn = "I prefer casual and comfortable clothing.",
                answerKo = "저는 캐주얼하고 편안한 옷을 선호합니다."
            )
        )
    )

    @Test
    fun `카테고리 선택 시 해당 카테고리의 질문들이 로드된다`() {
        val viewModel = MainViewModel(testItemsByCategory)
        viewModel.selectCategory("가족/친구")
        
        val currentQaItem = viewModel.uiState.value.currentQaItem
        assertNotNull(currentQaItem)
        assertEquals("가족/친구", currentQaItem?.category)
        assertEquals("What is your family like?", currentQaItem?.questionEn)
        assertEquals("My family is very close-knit and supportive.", currentQaItem?.answerEn)
    }

    @Test
    fun `nextQaItem 호출 시 같은 카테고리 내에서 다음 질문으로 이동한다`() {
        val viewModel = MainViewModel(testItemsByCategory)
        viewModel.selectCategory("가족/친구")
        
        // 첫 번째 질문 확인
        val firstQaItem = viewModel.uiState.value.currentQaItem
        assertEquals("What is your family like?", firstQaItem?.questionEn)
        
        // 다음 질문으로 이동
        viewModel.nextQaItem()
        val secondQaItem = viewModel.uiState.value.currentQaItem
        assertEquals("How often do you see your friends?", secondQaItem?.questionEn)
        
        // 다시 첫 번째 질문으로 순환
        viewModel.nextQaItem()
        val thirdQaItem = viewModel.uiState.value.currentQaItem
        assertEquals("What is your family like?", thirdQaItem?.questionEn)
    }

    @Test
    fun `다른 카테고리 선택 시 해당 카테고리의 첫 번째 질문이 로드된다`() {
        val viewModel = MainViewModel(testItemsByCategory)
        
        // 첫 번째 카테고리 선택
        viewModel.selectCategory("가족/친구")
        assertEquals("What is your family like?", viewModel.uiState.value.currentQaItem?.questionEn)
        
        // 두 번째 카테고리 선택
        viewModel.selectCategory("패션")
        assertEquals("What is your favorite style of clothing?", viewModel.uiState.value.currentQaItem?.questionEn)
    }

    @Test
    fun `존재하지 않는 카테고리 선택 시 에러가 발생한다`() {
        val viewModel = MainViewModel(testItemsByCategory)
        viewModel.selectCategory("존재하지 않는 카테고리")
        
        assertNull(viewModel.uiState.value.currentQaItem)
        assertTrue(viewModel.uiState.value.error?.isNotEmpty() == true)
    }

    @Test
    fun `카테고리 목록이 올바르게 로드된다`() {
        val viewModel = MainViewModel(testItemsByCategory)
        
        val categories = viewModel.uiState.value.categories
        assertTrue(categories.contains("가족/친구"))
        assertTrue(categories.contains("패션"))
        assertEquals(2, categories.size)
    }

    @Test
    fun `현재 카테고리가 올바르게 설정된다`() {
        val viewModel = MainViewModel(testItemsByCategory)
        
        // 초기 상태에서는 카테고리가 선택되지 않음
        assertNull(viewModel.uiState.value.currentCategory)
        
        // 카테고리 선택
        viewModel.selectCategory("가족/친구")
        assertEquals("가족/친구", viewModel.uiState.value.currentCategory)
        
        // 다른 카테고리 선택
        viewModel.selectCategory("패션")
        assertEquals("패션", viewModel.uiState.value.currentCategory)
    }
} 