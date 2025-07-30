package com.na982.opichelper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.event.ButtonEvent
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.repository.QaDataManager
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.usecase.GetCategoriesUseCase
import com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
import com.na982.opichelper.domain.usecase.SelectCategoryUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * 새로운 아키텍처에 맞는 MainViewModel
 * 이벤트 기반 아키텍처를 사용하여 상태 관리를 단순화
 */
@HiltViewModel
class MainViewModelRefactored @Inject constructor(
    private val appStateManager: AppStateManager,
    private val buttonEventHandler: ButtonEventHandler,
    private val qaDataManager: QaDataManager,
    private val recordingTimeManager: RecordingTimeManager,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val loadQaItemsUseCase: LoadQaItemsUseCase,
    private val selectCategoryUseCase: SelectCategoryUseCase
) : ViewModel() {
    
    // 앱 상태를 직접 관찰
    val appState: StateFlow<com.na982.opichelper.domain.state.AppState> = appStateManager.state
    
    init {
        Log.d("MainViewModelRefactored", "새로운 아키텍처 MainViewModel 초기화")
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // 카테고리 로드
                val categories = getCategoriesUseCase.invoke()
                Log.d("MainViewModelRefactored", "카테고리 로드 완료: ${categories.size}개")
                
                // 첫 번째 카테고리 선택
                if (categories.isNotEmpty()) {
                    selectCategory(categories.first())
                }
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "초기 데이터 로드 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 카테고리 선택
     */
    fun selectCategory(category: String) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModelRefactored", "카테고리 선택: $category")
                
                // 카테고리 선택
                val qaItems = selectCategoryUseCase.invoke(category)
                
                // 첫 번째 QA 아이템으로 상태 업데이트
                if (qaItems.isNotEmpty()) {
                    updateCurrentQaItem(qaItems.first(), category, 0, qaItems.size)
                }
                
                // 암기 모드 상태 초기화
                appStateManager.updateMemorizationModeState(
                    isRepeatListeningMode = false,
                    isEnglishWritingTestMode = false,
                    isFullMemorizationMode = false
                )
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "카테고리 선택 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 다음 QA 아이템으로 이동
     */
    fun nextQaItem() {
        val currentState = appState.value
        val currentCategory = currentState.currentCategory ?: return
        val currentIndex = currentState.currentIndex
        val totalCount = currentState.totalCount
        
        if (currentIndex < totalCount - 1) {
            viewModelScope.launch {
                try {
                    val qaItems = selectCategoryUseCase.invoke(currentCategory)
                    if (currentIndex + 1 < qaItems.size) {
                        updateCurrentQaItem(qaItems[currentIndex + 1], currentCategory, currentIndex + 1, totalCount)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModelRefactored", "다음 QA 아이템 이동 실패", e)
                    appStateManager.updateErrorState(e.message)
                }
            }
        }
    }
    
    /**
     * 이전 QA 아이템으로 이동
     */
    fun previousQaItem() {
        val currentState = appState.value
        val currentCategory = currentState.currentCategory ?: return
        val currentIndex = currentState.currentIndex
        
        if (currentIndex > 0) {
            viewModelScope.launch {
                try {
                    val qaItems = selectCategoryUseCase.invoke(currentCategory)
                    if (currentIndex - 1 >= 0) {
                        updateCurrentQaItem(qaItems[currentIndex - 1], currentCategory, currentIndex - 1, currentState.totalCount)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModelRefactored", "이전 QA 아이템 이동 실패", e)
                    appStateManager.updateErrorState(e.message)
                }
            }
        }
    }
    
    /**
     * 암기 레벨 선택
     */
    fun selectMemorizeLevel(level: String) {
        Log.d("MainViewModelRefactored", "암기 레벨 선택: $level")
        appStateManager.updateSelectedMemorizeLevel(level)
        
        // 암기 모드 상태 업데이트
        val isRepeatListeningMode = level == "반복듣기"
        val isEnglishWritingTestMode = level == "영작테스트"
        val isFullMemorizationMode = level == "통암기"
        
        appStateManager.updateMemorizationModeState(
            isRepeatListeningMode = isRepeatListeningMode,
            isEnglishWritingTestMode = isEnglishWritingTestMode,
            isFullMemorizationMode = isFullMemorizationMode
        )
    }
    
    /**
     * 질문 재생 버튼 클릭
     */
    fun handleQuestionPlayClick() {
        val currentState = appState.value
        val currentQaItem = currentState.currentQaItem ?: return
        
        viewModelScope.launch {
            try {
                val event = ButtonEvent.QuestionPlayClick(
                    question = currentQaItem.questionEn,
                    isFullMemorizationMode = currentState.isFullMemorizationModeSelected(),
                    category = currentState.currentCategory ?: "",
                    scriptIndex = currentState.currentIndex
                )
                
                Log.d("MainViewModelRefactored", "질문 재생 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "질문 재생 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 답변 재생 버튼 클릭
     */
    fun handleAnswerPlayClick() {
        val currentState = appState.value
        val currentQaItem = currentState.currentQaItem ?: return
        
        viewModelScope.launch {
            try {
                val answer = getCurrentAnswer(currentQaItem)
                val event = ButtonEvent.AnswerPlayClick(answer = answer)
                
                Log.d("MainViewModelRefactored", "답변 재생 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "답변 재생 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 암기 테스트 버튼 클릭
     */
    fun handleMemorizeTestClick() {
        val currentState = appState.value
        val currentQaItem = currentState.currentQaItem ?: return
        
        viewModelScope.launch {
            try {
                val memorizeLevel = when (currentState.selectedMemorizeLevel) {
                    "반복듣기" -> MemorizeLevel.REPEAT_LISTENING
                    "영작테스트" -> MemorizeLevel.ENGLISH_WRITING
                    "통암기" -> MemorizeLevel.FULL_MEMORIZATION
                    else -> return@launch
                }
                
                val answerKo = getCurrentAnswerKo(currentQaItem)
                val answerEn = getCurrentAnswer(currentQaItem)
                
                val event = ButtonEvent.MemorizeTestClick(
                    memorizeLevel = memorizeLevel,
                    category = currentState.currentCategory ?: "",
                    scriptIndex = currentState.currentIndex,
                    answerKo = answerKo,
                    answerEn = answerEn
                )
                
                Log.d("MainViewModelRefactored", "암기 테스트 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "암기 테스트 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 녹음 재생 버튼 클릭
     */
    fun handleRecordingPlayClick() {
        val currentState = appState.value
        
        viewModelScope.launch {
            try {
                val memorizeLevel = when (currentState.selectedMemorizeLevel) {
                    "반복듣기" -> MemorizeLevel.REPEAT_LISTENING
                    "영작테스트" -> MemorizeLevel.ENGLISH_WRITING
                    "통암기" -> MemorizeLevel.FULL_MEMORIZATION
                    else -> return@launch
                }
                
                val event = ButtonEvent.RecordingPlayClick(memorizeLevel = memorizeLevel)
                
                Log.d("MainViewModelRefactored", "녹음 재생 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "녹음 재생 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 중지 버튼 클릭
     */
    fun handleStopClick(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction) {
        viewModelScope.launch {
            try {
                val event = ButtonEvent.StopClick(buttonFunction = buttonFunction)
                
                Log.d("MainViewModelRefactored", "중지 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "중지 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 현재 QA 아이템 업데이트
     */
    private fun updateCurrentQaItem(qaItem: QaItem, category: String, index: Int, totalCount: Int) {
        appStateManager.updateCurrentQaItem(
            qaItem = qaItem,
            category = category,
            index = index,
            totalCount = totalCount
        )
        
        // 카드 상태 초기화
        appStateManager.updateCardState(
            isQuestionCardFlipped = false,
            isAnswerCardFlipped = false
        )
        
        // 하이라이트 상태 초기화
        appStateManager.updateHighlightState(
            questionHighlightIndex = null,
            answerHighlightIndex = null,
            answerKoHighlightIndex = null,
            recordingHighlightIndex = null
        )
    }
    
    /**
     * 현재 답변 가져오기
     */
    fun getCurrentAnswer(qaItem: QaItem): String {
        return qaItem.answers[com.na982.opichelper.domain.entity.UserLevel.IM]?.answerEn 
            ?: qaItem.answers[com.na982.opichelper.domain.entity.UserLevel.IH]?.answerEn 
            ?: ""
    }
    
    /**
     * 현재 한글 답변 가져오기
     */
    fun getCurrentAnswerKo(qaItem: QaItem): String {
        return qaItem.answers[com.na982.opichelper.domain.entity.UserLevel.IM]?.answerKo 
            ?: qaItem.answers[com.na982.opichelper.domain.entity.UserLevel.IH]?.answerKo 
            ?: ""
    }
    
    // UI에서 필요한 메서드들
    fun getButtonConfig(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction): com.na982.opichelper.domain.entity.ButtonConfig {
        val currentState = appState.value
        val buttonState = currentState.getButtonState(buttonFunction)
        
        return when (buttonFunction) {
            com.na982.opichelper.domain.entity.ButtonFunction.QuestionPlay -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "질문 재생"
                )
            }
            com.na982.opichelper.domain.entity.ButtonFunction.AnswerPlay -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "답변 재생"
                )
            }
            com.na982.opichelper.domain.entity.ButtonFunction.MemorizeTest -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "암기 테스트"
                )
            }
            com.na982.opichelper.domain.entity.ButtonFunction.RecordingPlay -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "녹음 재생"
                )
            }
            com.na982.opichelper.domain.entity.ButtonFunction.Stop -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "중지"
                )
            }
        }
    }
    
    fun checkEnglishWritingTestMergedFile() {
        // TODO: 구현 필요
        Log.d("MainViewModelRefactored", "영작테스트 병합 파일 확인")
    }
    
    fun isQuestionCardFlipped(): StateFlow<Boolean> {
        return MutableStateFlow(false)
    }
    
    fun hasEnglishWritingTestMergedFile(): StateFlow<Boolean> {
        return MutableStateFlow(false)
    }
    
    fun isEnglishWritingTestMergedFilePlaying(): StateFlow<Boolean> {
        return MutableStateFlow(false)
    }
    
    fun englishWritingTestMergedFileHighlightIndex(): StateFlow<Int?> {
        return MutableStateFlow(null)
    }
    
    // UI에서 필요한 추가 메서드들
    fun handleBackPress() {
        // TODO: 구현 필요
        Log.d("MainViewModelRefactored", "백키 처리")
    }
    
    fun handleSettingsEnter() {
        // TODO: 구현 필요
        Log.d("MainViewModelRefactored", "설정 진입")
    }
    
    fun handleCategoryChange(category: String) {
        selectCategory(category)
    }
    
    fun handleMemorizeLevelChange(level: String) {
        selectMemorizeLevel(level)
    }
    
    fun setSelectedMemorizeLevel(level: String) {
        selectMemorizeLevel(level)
    }
    
    fun handleScriptChange() {
        // TODO: 구현 필요
        Log.d("MainViewModelRefactored", "스크립트 변경")
    }
} 