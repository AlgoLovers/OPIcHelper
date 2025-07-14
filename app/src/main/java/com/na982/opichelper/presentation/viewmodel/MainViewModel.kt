package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.QuestionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import android.content.Context

data class MainUiState(
    val currentQaItem: QaItem? = null,
    val currentCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val categories: List<String> = emptyList()
)

class MainViewModel : AndroidViewModel {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val itemsByCategory: MutableMap<String, List<QaItem>> = mutableMapOf()
    private val itemIndexByCategory: MutableMap<String, Int> = mutableMapOf()

    // Primary constructor for test (inject data)
    constructor(itemsByCategory: Map<String, List<QaItem>>) : super(Application()) {
        this.itemsByCategory.putAll(itemsByCategory)
        for (category in itemsByCategory.keys) {
            itemIndexByCategory[category] = 0
        }
        _uiState.value = _uiState.value.copy(categories = itemsByCategory.keys.toList())
    }

    // Secondary constructor for production (load from assets)
    constructor(application: Application) : this(mutableMapOf()) {
        loadQaItemsFromAssets(application)
    }

    private fun loadQaItemsFromAssets(application: Application) {
        val context = application
        val gson = Gson()
        val assetFiles = context.assets.list("")?.filter { it.startsWith("qa_") && it.endsWith(".json") } ?: emptyList()
        val categories = assetFiles.map { it.removePrefix("qa_").removeSuffix(".json") }
        for ((i, fileName) in assetFiles.withIndex()) {
            val category = categories[i]
            try {
                val inputStream = context.assets.open(fileName)
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<List<QaItemAsset>>() {}.type
                val assetItems: List<QaItemAsset> = gson.fromJson(reader, type)
                val items = assetItems.map {
                    QaItem(
                        id = it.id ?: "",
                        category = category,
                        questionEn = it.question_en,
                        questionKo = it.question_ko,
                        answerEn = it.answer_en,
                        answerKo = it.answer_ko
                    )
                }
                itemsByCategory[category] = items
                itemIndexByCategory[category] = 0
            } catch (e: Exception) {
                itemsByCategory[category] = emptyList()
                itemIndexByCategory[category] = 0
            }
        }
        _uiState.value = _uiState.value.copy(categories = categories)
    }

    fun selectCategory(category: String) {
        val items = itemsByCategory[category] ?: emptyList()
        val index = itemIndexByCategory[category] ?: 0
        if (items.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                currentQaItem = items[index],
                currentCategory = category,
                isLoading = false,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentQaItem = null,
                currentCategory = category,
                isLoading = false,
                error = "해당 카테고리에 질문이 없습니다."
            )
        }
    }

    fun nextQaItem() {
        val category = _uiState.value.currentCategory ?: return
        val items = itemsByCategory[category] ?: return
        if (items.isEmpty()) return
        val currentIndex = itemIndexByCategory[category] ?: 0
        val nextIndex = (currentIndex + 1) % items.size
        itemIndexByCategory[category] = nextIndex
        _uiState.value = _uiState.value.copy(
            currentQaItem = items[nextIndex],
            isLoading = false,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    data class QaItemAsset(
        val id: String? = null,
        val question_en: String,
        val question_ko: String,
        val answer_en: String,
        val answer_ko: String
    )
} 