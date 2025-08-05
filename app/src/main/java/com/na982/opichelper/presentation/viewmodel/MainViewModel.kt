package com.na982.opichelper.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.na982.opichelper.domain.entity.ButtonFunction
import com.na982.opichelper.domain.entity.MemorizeLevel
import com.na982.opichelper.domain.entity.QaItem
import com.na982.opichelper.domain.event.ButtonEvent
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager

import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.usecase.LoadCategoriesUseCase
import com.na982.opichelper.domain.usecase.GetLeveledAnswerUseCase
import com.na982.opichelper.domain.usecase.InitializeAppUseCase
import com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
import com.na982.opichelper.domain.manager.ICategoryManager
import com.na982.opichelper.domain.manager.IAudioControlManager
import com.na982.opichelper.domain.manager.IMemorizationManager
import com.na982.opichelper.domain.manager.MemorizationUiState
import com.na982.opichelper.domain.strategy.MemorizationLevelMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * 새로운 아키텍처에 맞는 MainViewModel
 * 이벤트 기반 아키텍처를 사용하여 상태 관리를 단순화
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appStateManager: AppStateManager,
    private val buttonEventHandler: ButtonEventHandler,
    private val qaDataRepository: QaDataRepository,
    private val recordingTimeManager: RecordingTimeManager,
    private val categoryManager: ICategoryManager,
    private val audioControlManager: IAudioControlManager,
    private val memorizationManager: IMemorizationManager,
    private val initializeAppUseCase: InitializeAppUseCase,
    private val getCurrentAnswerUseCase: GetLeveledAnswerUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val memorizationLevelMapper: MemorizationLevelMapper // Added MemorizationLevelMapper
) : ViewModel() {
    
    // 앱 상태를 직접 관찰
        val appState: StateFlow<com.na982.opichelper.domain.state.AppState> = appStateManager.state
    
    // 카테고리 관련 상태는 CategoryManager에서 가져오기
    val categories: StateFlow<List<String>> = categoryManager.categories
    val currentCategory: StateFlow<String?> = categoryManager.currentCategory
    val categoryLoading: StateFlow<Boolean> = categoryManager.isLoading
    val categoryError: StateFlow<String?> = categoryManager.error
    
    // 오디오 제어 관련 상태는 AppState에서 가져오기
    val isQuestionPlaying: StateFlow<Boolean> = appState.map { it.isQuestionPlaying }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )
    val isAnswerPlaying: StateFlow<Boolean> = appState.map { it.isAnswerPlaying }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )
    val isPlaying: StateFlow<Boolean> = appState.map { it.isPlaying }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )
    val audioError: StateFlow<String?> = audioControlManager.error
    
    // 암기 테스트 관련 상태는 MemorizationManager에서 가져오기
    val memorizationState: StateFlow<MemorizationUiState> = memorizationManager.uiState
    
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
     * 카테고리 변경 (CategoryManager에 위임)
     */
    fun changeCategory(category: String) {
        Log.d("MainViewModelRefactored", "카테고리 변경 위임: $category")
        categoryManager.changeCategory(category)
    }
    
    /**
     * 다음 QA 아이템으로 이동 (임시 구현)
     */
    fun nextQaItem() {
        viewModelScope.launch {
            try {
                qaDataRepository.nextQaItem()
                val qaItem = qaDataRepository.getCurrentQaItem()
                val category = qaDataRepository.getCurrentCategory() ?: return@launch
                val currentIndex = qaDataRepository.getCurrentIndex()
                val itemsInCategory = qaDataRepository.getItemsInCategory(category)
                
                if (qaItem != null) {
                    appStateManager.updateQaItemState(
                        qaItem = qaItem,
                        category = category,
                        index = currentIndex,
                        totalCount = itemsInCategory.size
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "다음 QA 아이템 이동 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 이전 QA 아이템으로 이동 (임시 구현)
     */
    fun previousQaItem() {
        viewModelScope.launch {
            try {
                qaDataRepository.previousQaItem()
                val qaItem = qaDataRepository.getCurrentQaItem()
                val category = qaDataRepository.getCurrentCategory() ?: return@launch
                val currentIndex = qaDataRepository.getCurrentIndex()
                val itemsInCategory = qaDataRepository.getItemsInCategory(category)
                
                if (qaItem != null) {
                    appStateManager.updateQaItemState(
                        qaItem = qaItem,
                        category = category,
                        index = currentIndex,
                        totalCount = itemsInCategory.size
                    )
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
        
        // 암기 모드 상태 업데이트 - ProgressManager에서 처리하도록 변경
        // val isRepeatListeningMode = level == "반복듣기"
        // val isEnglishWritingTestMode = level == "영작테스트"
        // val isFullMemorizationMode = level == "통암기"
        // 
        // appStateManager.updateMemorizationModeState(
        //     isRepeatListeningMode = isRepeatListeningMode,
        //     isEnglishWritingTestMode = isEnglishWritingTestMode,
        //     isFullMemorizationMode = isFullMemorizationMode
        // )
    }
    
    /**
     * 모든 작업을 중단하는 헬퍼 메서드 (AudioControlManager에 위임)
     */
    private fun stopAllOperations() {
        audioControlManager.stopAllAudio()
    }
    
    /**
     * 질문 재생 버튼 클릭 (ButtonEventHandler에 위임)
     */
    fun handleQuestionPlayClick() {
        val currentState = appState.value
        val currentQaItem = currentState.currentQaItem ?: return
        
        viewModelScope.launch {
            try {
                val event = ButtonEvent.QuestionPlayClick(currentQaItem)
                buttonEventHandler.handleEvent(event)
            } catch (e: Exception) {
                Log.e("MainViewModel", "질문 재생 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 답변 재생 버튼 클릭 (ButtonEventHandler에 위임)
     */
    fun handleAnswerPlayClick() {
        val currentState = appState.value
        val currentQaItem = currentState.currentQaItem ?: return
        
        viewModelScope.launch {
            try {
                val event = ButtonEvent.AnswerPlayClick(currentQaItem)
                buttonEventHandler.handleEvent(event)
            } catch (e: Exception) {
                Log.e("MainViewModel", "답변 재생 실패", e)
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
                // MemorizationLevelMapper를 사용하여 레벨 매핑
                val memorizeLevel = memorizationLevelMapper.mapToMemorizeLevel(currentState.selectedMemorizeLevel)
                if (memorizeLevel == null) {
                    Log.w("MainViewModelRefactored", "알 수 없는 암기 레벨: ${currentState.selectedMemorizeLevel}")
                    return@launch
                }
                
                val category = currentState.currentCategory ?: return@launch
                val scriptIndex = currentState.currentIndex
                val answerKo = currentQaItem.answers.values.first().answerKo
                val answerEn = currentQaItem.answers.values.first().answerEn
                
                Log.d("MainViewModelRefactored", "매칭된 MemorizeLevel: $memorizeLevel")
                val event = ButtonEvent.MemorizeTestClick(
                    memorizeLevel = memorizeLevel,
                    category = category,
                    scriptIndex = scriptIndex,
                    answerKo = answerKo,
                    answerEn = answerEn
                )
                
                Log.d("MainViewModelRefactored", "암기 테스트 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                Log.d("MainViewModelRefactored", "암기 테스트 이벤트 처리 완료")
                
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
        Log.d("MainViewModelRefactored", "녹음 재생 버튼 클릭 시작")
        val currentState = appState.value
        Log.d("MainViewModelRefactored", "현재 선택된 암기 레벨: ${currentState.selectedMemorizeLevel}")
        
        viewModelScope.launch {
            try {
                // MemorizationLevelMapper를 사용하여 레벨 매핑
                val memorizeLevel = memorizationLevelMapper.mapToMemorizeLevel(currentState.selectedMemorizeLevel)
                if (memorizeLevel == null) {
                    Log.w("MainViewModelRefactored", "알 수 없는 암기 레벨: ${currentState.selectedMemorizeLevel}")
                    return@launch
                }
                
                val currentQaItem = currentState.currentQaItem ?: return@launch
                val category = currentState.currentCategory ?: return@launch
                val scriptIndex = currentState.currentIndex
                val answerKo = currentQaItem.answers.values.first().answerKo
                val answerEn = currentQaItem.answers.values.first().answerEn
                
                Log.d("MainViewModelRefactored", "매칭된 MemorizeLevel: $memorizeLevel")
                val event = ButtonEvent.RecordingPlayClick(
                    memorizeLevel = memorizeLevel,
                    category = category,
                    scriptIndex = scriptIndex,
                    answerKo = answerKo,
                    answerEn = answerEn
                )
                
                Log.d("MainViewModelRefactored", "녹음 재생 이벤트 발생: $event")
                buttonEventHandler.handleEvent(event)
                Log.d("MainViewModelRefactored", "녹음 재생 이벤트 처리 완료")
                
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "녹음 재생 실패", e)
                appStateManager.updateErrorState(e.message)
            }
        }
    }
    
    /**
     * 중지 버튼 클릭
     */
    fun handleStopClick(buttonFunction: ButtonFunction) {
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
    fun getButtonConfig(buttonFunction: ButtonFunction): com.na982.opichelper.domain.entity.ButtonConfig {
        val currentState = appState.value
        val buttonState = currentState.buttonStates[buttonFunction] ?: com.na982.opichelper.domain.entity.ButtonState.Idle
        
        Log.d("MainViewModel", "getButtonConfig: $buttonFunction -> $buttonState")
        
        return when (buttonFunction) {
            ButtonFunction.QuestionPlay -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "질문 재생"
                )
            }
            ButtonFunction.AnswerPlay -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "답변 재생"
                )
            }
            ButtonFunction.MemorizeTest -> {
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
            ButtonFunction.RecordingPlay -> {
                com.na982.opichelper.domain.entity.ButtonConfig(
                    function = buttonFunction,
                    state = buttonState,
                    text = "녹음 재생"
                )
            }
            ButtonFunction.Stop -> {
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
    
    /**
     * 영작테스트 녹음 파일 존재 여부 확인
     */
    suspend fun hasEnglishWritingRecording(): Boolean {
        val currentState = appState.value
        val category = currentState.currentCategory ?: return false
        val scriptIndex = currentState.currentIndex
        
        return try {
            // TODO: RecordingFileRepository를 통해 파일 존재 여부 확인
            Log.d("MainViewModelRefactored", "영작테스트 녹음 파일 확인: category=$category, scriptIndex=$scriptIndex")
            false // 임시로 false 반환
        } catch (e: Exception) {
            Log.e("MainViewModelRefactored", "영작테스트 녹음 파일 확인 실패", e)
            false
        }
    }
    
    /**
     * 통암기 녹음 파일 존재 여부 확인
     */
    suspend fun hasFullMemorizationRecording(): Boolean {
        val currentState = appState.value
        val category = currentState.currentCategory ?: return false
        val scriptIndex = currentState.currentIndex
        
        return try {
            // TODO: RecordingFileRepository를 통해 파일 존재 여부 확인
            Log.d("MainViewModelRefactored", "통암기 녹음 파일 확인: category=$category, scriptIndex=$scriptIndex")
            false // 임시로 false 반환
        } catch (e: Exception) {
            Log.e("MainViewModelRefactored", "통암기 녹음 파일 확인 실패", e)
            false
        }
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
    
    fun englishWritingTestMergedFileHighlightIndex(): StateFlow<Int> {
        return MutableStateFlow(-1)
    }
    
    // UI에서 필요한 추가 메서드들
    fun handleBackPress() {
        Log.d("MainViewModelRefactored", "백키 처리 - ButtonEventHandler를 통한 처리")
        
        // Presentation Layer에서 Domain Layer의 ButtonEventHandler 호출
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Stop 이벤트를 ButtonEventHandler에 전달
                buttonEventHandler.handleEvent(ButtonEvent.StopClick(ButtonFunction.Stop))
                Log.d("MainViewModelRefactored", "백키 처리 완료")
            } catch (e: Exception) {
                Log.e("MainViewModelRefactored", "백키 처리 중 오류", e)
            }
        }
    }
    
    fun handleSettingsEnter() {
        // TODO: 구현 필요
        Log.d("MainViewModelRefactored", "설정 진입")
    }
    
    fun handleCategoryChange(category: String) {
        Log.d("MainViewModel", "카테고리 변경: $category")
        
        // 1. 재생 중인 오디오 중지 (일시 중지)
        audioControlManager.stopAllAudio()
        
        // 2. 카테고리 변경
        categoryManager.changeCategory(category)
    }
    
    fun handleMemorizeLevelChange(level: String) {
        Log.d("MainViewModel", "암기레벨 변경: $level")
        
        // 1. 재생 중인 오디오 중지 (일시 중지)
        audioControlManager.stopAllAudio()
        
        // 2. 암기레벨 변경
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