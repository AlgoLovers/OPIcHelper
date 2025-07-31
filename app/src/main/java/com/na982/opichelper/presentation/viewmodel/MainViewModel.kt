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
import com.na982.opichelper.domain.usecase.InitializeAppUseCase
import com.na982.opichelper.domain.usecase.GetCurrentAnswerUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import android.app.Application
import com.na982.opichelper.domain.entity.ButtonFunction

/**
 * 새로운 아키텍처에 맞는 MainViewModel
 * 이벤트 기반 아키텍처를 사용하여 상태 관리를 단순화
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appStateManager: AppStateManager,
    private val buttonEventHandler: ButtonEventHandler,
    private val qaDataManager: QaDataManager,
    private val recordingTimeManager: RecordingTimeManager,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val loadQaItemsUseCase: LoadQaItemsUseCase,
    private val initializeAppUseCase: InitializeAppUseCase,
    private val getCurrentAnswerUseCase: GetCurrentAnswerUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    // 앱 상태를 직접 관찰
    val appState: StateFlow<com.na982.opichelper.domain.state.AppState> = appStateManager.state
    
    init {
        Log.d("MainViewModelRefactored", "새로운 아키텍처 MainViewModel 초기화")
        // 앱 초기화는 MainActivity에서 호출
    }
    
    /**
     * 앱 초기화 (MainActivity에서 호출)
     */
    fun initializeApp(application: Application) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModelRefactored", "앱 초기화 시작")
                appStateManager.updateLoadingState(true)
                
                // InitializeAppUseCase를 통한 앱 초기화
                initializeAppUseCase.execute(application)
                
                Log.d("MainViewModelRefactored", "앱 초기화 완료")
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "앱 초기화 실패", e)
                appStateManager.updateErrorState(e.message)
                appStateManager.updateLoadingState(false)
            }
        }
    }
    
    /**
     * 카테고리 변경
     */
    fun changeCategory(category: String) {
        Log.d("MainViewModelRefactored", "카테고리 변경: $category")
        
        // 기존 작업 중단
        stopAllOperations()
        
        // 새로운 카테고리로 변경
        viewModelScope.launch {
            try {
                qaDataManager.selectCategory(category)
                val qaItem = qaDataManager.getCurrentQaItem()
                if (qaItem != null) {
                    val currentIndex = qaDataManager.getCurrentIndex()
                    val itemsInCategory = qaDataManager.getItemsInCategory(category)
                    updateCurrentQaItem(qaItem, category, currentIndex, itemsInCategory.size)
                } else {
                    Log.e("MainViewModelRefactored", "QA 아이템이 비어있음")
                }
                
                // 암기 모드 상태 초기화
                appStateManager.updateMemorizationModeState(
                    isRepeatListeningMode = false,
                    isEnglishWritingTestMode = false,
                    isFullMemorizationMode = false
                )
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "카테고리 변경 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 다음 QA 아이템으로 이동
     */
    fun nextQaItem() {
        viewModelScope.launch {
            try {
                qaDataManager.nextQaItem()
                val qaItem = qaDataManager.getCurrentQaItem()
                val category = qaDataManager.getCurrentCategory() ?: return@launch
                val currentIndex = qaDataManager.getCurrentIndex()
                val itemsInCategory = qaDataManager.getItemsInCategory(category)
                
                if (qaItem != null) {
                    updateCurrentQaItem(qaItem, category, currentIndex, itemsInCategory.size)
                }
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "다음 QA 아이템 이동 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 이전 QA 아이템으로 이동
     */
    fun previousQaItem() {
        viewModelScope.launch {
            try {
                qaDataManager.previousQaItem()
                val qaItem = qaDataManager.getCurrentQaItem()
                val category = qaDataManager.getCurrentCategory() ?: return@launch
                val currentIndex = qaDataManager.getCurrentIndex()
                val itemsInCategory = qaDataManager.getItemsInCategory(category)
                
                if (qaItem != null) {
                    updateCurrentQaItem(qaItem, category, currentIndex, itemsInCategory.size)
                }
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "이전 QA 아이템 이동 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 암기 레벨 선택
     */
    fun selectMemorizeLevel(level: String) {
        Log.d("MainViewModelRefactored", "암기 레벨 선택: $level")
        
        // 기존 작업 중단
        stopAllOperations()
        
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
     * 모든 작업을 중단하는 헬퍼 메서드
     */
    private fun stopAllOperations() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModelRefactored", "모든 작업 중단")
                
                // 모든 버튼에 대해 중지 이벤트 발생
                val stopEvents = listOf(
                    ButtonEvent.StopClick(ButtonFunction.QuestionPlay),
                    ButtonEvent.StopClick(ButtonFunction.AnswerPlay),
                    ButtonEvent.StopClick(ButtonFunction.MemorizeTest),
                    ButtonEvent.StopClick(ButtonFunction.RecordingPlay)
                )
                
                for (event in stopEvents) {
                    buttonEventHandler.handleEvent(event)
                }
                
                Log.d("MainViewModelRefactored", "모든 작업 중단 완료")
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "작업 중단 실패", e)
            }
        }
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
                    isFullMemorizationMode = currentState.selectedMemorizeLevel == "통암기",
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
        
        Log.d("MainViewModelRefactored", "암기 테스트 버튼 클릭 - 선택된 레벨: ${currentState.selectedMemorizeLevel}")
        
        viewModelScope.launch {
            try {
                val memorizeLevel = when (currentState.selectedMemorizeLevel) {
                    "반복듣기" -> MemorizeLevel.REPEAT_LISTENING
                    "영작테스트" -> MemorizeLevel.ENGLISH_WRITING
                    "통암기" -> MemorizeLevel.FULL_MEMORIZATION
                    else -> {
                        Log.w("MainViewModelRefactored", "알 수 없는 암기 레벨: ${currentState.selectedMemorizeLevel}")
                        return@launch
                    }
                }
                
                val answerKo = getCurrentAnswerKo(currentQaItem)
                val answerEn = getCurrentAnswer(currentQaItem)
                
                Log.d("MainViewModelRefactored", "암기 테스트 데이터 준비 - 레벨: $memorizeLevel, 카테고리: ${currentState.currentCategory}, 인덱스: ${currentState.currentIndex}")
                
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
        
        // 하이라이트는 TtsControllerImpl에서만 처리
    }
    
    /**
     * 현재 답변 가져오기 (UseCase 사용)
     */
    fun getCurrentAnswer(qaItem: QaItem): String {
        return getCurrentAnswerUseCase.getCurrentAnswer(qaItem)
    }
    
    /**
     * 현재 한글 답변 가져오기 (UseCase 사용)
     */
    fun getCurrentAnswerKo(qaItem: QaItem): String {
        return getCurrentAnswerUseCase.getCurrentAnswerKo(qaItem)
    }
    
    // UI에서 필요한 메서드들
    fun getButtonConfig(buttonFunction: com.na982.opichelper.domain.entity.ButtonFunction): com.na982.opichelper.domain.entity.ButtonConfig {
        val currentState = appState.value
        val buttonState = currentState.buttonStates[buttonFunction] ?: com.na982.opichelper.domain.entity.ButtonState.Idle
        
        Log.d("MainViewModel", "getButtonConfig: $buttonFunction -> $buttonState")
        
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
                // 반복듣기 모드에서 버튼 텍스트 동적 변경
                val buttonText = when {
                    currentState.selectedMemorizeLevel == "반복듣기" && buttonState == com.na982.opichelper.domain.entity.ButtonState.Playing -> "반복듣기중"
                    currentState.selectedMemorizeLevel == "반복듣기" -> "반복듣기"
                    currentState.selectedMemorizeLevel == "영작테스트" -> "영작테스트"
                    currentState.selectedMemorizeLevel == "통암기" -> "통암기"
                    else -> "암기 테스트"
                }
                
                Log.d("MainViewModel", "MemorizeTest 버튼 설정: 상태=$buttonState, 텍스트=$buttonText")
                
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = buttonText
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
        changeCategory(category)
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
    
    /**
     * 백그라운드로 이동할 때 호출
     */
    fun onBackgroundMove() {
        // TODO: 백그라운드 이동 시 필요한 처리
        Log.d("MainViewModelRefactored", "백그라운드로 이동")
    }
    
    /**
     * 포그라운드로 복귀할 때 호출
     */
    fun onForegroundReturn() {
        // TODO: 포그라운드 복귀 시 필요한 처리
        Log.d("MainViewModelRefactored", "포그라운드로 복귀")
    }
    
    /**
     * 모든 TTS를 동기적으로 중지
     */
    fun cleanupAllTtsSync() {
        // TODO: TTS 중지 로직 구현
        Log.d("MainViewModelRefactored", "모든 TTS 동기적 중지")
    }
    
    /**
     * 앱 종료 시 모든 리소스 정리
     */
    fun cleanupOnAppExit() {
        // TODO: 앱 종료 시 필요한 정리 작업 구현
        Log.d("MainViewModelRefactored", "앱 종료 시 리소스 정리")
    }
} 