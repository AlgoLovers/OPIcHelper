package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.audio.SentenceSplitter
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.entity.UserLevel
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.ScriptEditRepository
import com.na982.opichelper.domain.usecase.SentencePair
import com.na982.opichelper.domain.usecase.ValidateScriptEditUseCase
import com.na982.opichelper.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditScriptViewModel @Inject constructor(
    private val scriptEditRepository: ScriptEditRepository,
    private val validateScriptEditUseCase: ValidateScriptEditUseCase,
    private val qaDataManager: QaDataManager
) : ViewModel() {

    private val _sentencePairs = MutableStateFlow<List<SentencePair>>(emptyList())
    val sentencePairs: StateFlow<List<SentencePair>> = _sentencePairs.asStateFlow()

    private val _validationResult = MutableStateFlow(ValidationResult(emptyList(), true))
    val validationResult: StateFlow<ValidationResult> = _validationResult.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    fun loadSentences(qaItem: QaItem, isQuestion: Boolean, level: UserLevel) {
        _isModified.update { false }
        val textKo = if (isQuestion) qaItem.questionKo else qaItem.answers[level]?.answerKo ?: ""
        val textEn = if (isQuestion) qaItem.questionEn else qaItem.answers[level]?.answerEn ?: ""
        val koSentences = SentenceSplitter.split(textKo)
        val enSentences = SentenceSplitter.split(textEn)
        val pairs = koSentences.zip(enSentences).map { (ko, en) -> SentencePair(ko, en) }
        _sentencePairs.update { pairs }
        validate()
    }

    fun updatePair(index: Int, korean: String? = null, english: String? = null) {
        _sentencePairs.update { current ->
            if (index in current.indices) {
                val pair = current[index]
                current.toMutableList().apply {
                    this[index] = pair.copy(
                        korean = korean ?: pair.korean,
                        english = english ?: pair.english
                    )
                }
            } else current
        }
        validate()
    }

    fun addPair() {
        _sentencePairs.update { it + SentencePair("", "") }
    }

    fun removePair(index: Int) {
        _sentencePairs.update { current ->
            if (current.size > 1) {
                current.toMutableList().apply { removeAt(index) }
            } else current
        }
        validate()
    }

    fun validate() {
        _validationResult.update { validateScriptEditUseCase.validate(_sentencePairs.value) }
    }

    fun save(qaItem: QaItem, isQuestion: Boolean, level: UserLevel, scriptIndex: Int) {
        val koText = SentenceSplitter.join(_sentencePairs.value.map { it.korean })
        val enText = SentenceSplitter.join(_sentencePairs.value.map { it.english })
        val updatedItem = if (isQuestion) {
            qaItem.copy(questionKo = koText, questionEn = enText)
        } else {
            val currentAnswer = qaItem.answers[level] ?: return
            qaItem.copy(answers = qaItem.answers + (level to currentAnswer.copy(answerKo = koText, answerEn = enText)))
        }
        viewModelScope.launch {
            scriptEditRepository.updateQaItem(updatedItem, level, scriptIndex)
            qaDataManager.reload()
            _isModified.update { true }
        }
    }

    fun restoreOriginal(id: String) {
        viewModelScope.launch {
            scriptEditRepository.restoreOriginal(id)
            _isModified.update { true }
        }
    }
}
