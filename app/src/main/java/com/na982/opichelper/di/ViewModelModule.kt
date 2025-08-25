package com.na982.opichelper.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.manager.IAudioControlManager
import com.na982.opichelper.domain.manager.ICategoryManager
import com.na982.opichelper.domain.manager.IMemorizationManager
import com.na982.opichelper.domain.manager.AudioControlManager
import com.na982.opichelper.domain.manager.CategoryManager
import com.na982.opichelper.domain.manager.MemorizationManager
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.domain.state.MemorizationProgressTracker
import com.na982.opichelper.domain.usecase.GetLeveledAnswerUseCase
import com.na982.opichelper.domain.usecase.LoadCategoriesUseCase
import com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
import com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
import com.na982.opichelper.domain.button.ButtonStateManager
import com.na982.opichelper.domain.strategy.MemorizationLevelMapper
import com.na982.opichelper.domain.strategy.MemorizationStrategyFactory
import com.na982.opichelper.domain.strategy.RepeatListeningStrategy
import com.na982.opichelper.domain.strategy.EnglishWritingStrategy
import com.na982.opichelper.domain.strategy.FullMemorizationStrategy
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.button.ButtonStateObserver
import com.na982.opichelper.domain.manager.ProgressManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import com.na982.opichelper.domain.usecase.PlayRecordingUseCase
// Removed duplicate/ambiguous AudioRecorder import
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ViewModel 전용 의존성을 관리하는 모듈
 * ViewModelComponent 범위에서만 사용됨
 * ViewModel과 생명주기를 같이 하는 객체들을 관리
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    
    // ===== Button & State Management =====
    
    @Provides
    @ViewModelScoped
    fun provideButtonStateManager(): ButtonStateManager {
        return ButtonStateManager()
    }
    
    @Provides
    @ViewModelScoped
    fun provideLoadCategoriesUseCase(
        qaDataRepository: com.na982.opichelper.domain.repository.QaDataRepository
    ): com.na982.opichelper.domain.usecase.LoadCategoriesUseCase {
        return com.na982.opichelper.domain.usecase.LoadCategoriesUseCase(qaDataRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideLoadQaItemsUseCase(
        qaDataRepository: com.na982.opichelper.domain.repository.QaDataRepository
    ): com.na982.opichelper.domain.usecase.LoadQaItemsUseCase {
        return com.na982.opichelper.domain.usecase.LoadQaItemsUseCase(qaDataRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideCategoryManager(
        qaDataRepository: com.na982.opichelper.domain.repository.QaDataRepository,
        appStateManager: AppStateManager,
        loadCategoriesUseCase: com.na982.opichelper.domain.usecase.LoadCategoriesUseCase,
        loadQaItemsUseCase: com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
    ): ICategoryManager {
        return com.na982.opichelper.domain.manager.CategoryManager(
            qaDataRepository = qaDataRepository,
            appStateManager = appStateManager,
            loadCategoriesUseCase = loadCategoriesUseCase,
            loadQaItemsUseCase = loadQaItemsUseCase
        )
    }
    
    // MemorizationProgressTracker는 AppModule에서 @Singleton으로 제공됨
    
    // MemorizationManager는 AppModule에서 @Singleton으로 제공됨
    
    // IMemorizationManager는 MemorizationManager가 직접 구현하므로 별도 제공 불필요
    
    // ===== Repository Layer =====
    

    
    // RecordingFileRepository는 ActivityComponent 의존성 때문에 ViewModelModule에서 제공 불가
    // → Activity 의존성을 제거하여 ViewModelComponent에서 제공하도록 변경
    
    @Provides
    @ViewModelScoped
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return com.na982.opichelper.data.audio.AudioRecorderImpl(context)
    }
    
    @Provides
    @ViewModelScoped
    fun provideRecordingAudioPlayer(
        recordingTimeManager: RecordingTimeManager,
        highlightManager: com.na982.opichelper.domain.audio.HighlightManager,
        highlightEventHandler: com.na982.opichelper.domain.event.HighlightEventHandler
    ): com.na982.opichelper.domain.audio.RecordingAudioPlayer {
        return com.na982.opichelper.data.audio.RecordingAudioPlayerImpl(
            recordingTimeManager,
            highlightManager,
            highlightEventHandler
        )
    }
    
    @Provides
    @ViewModelScoped
    fun provideRecordingFileRepository(
        audioFileManager: AudioFileManager,
        audioRecorder: AudioRecorder,
        recordingAudioPlayer: com.na982.opichelper.domain.audio.RecordingAudioPlayer,
        recordingTimeManager: RecordingTimeManager
    ): RecordingFileRepository {
        return com.na982.opichelper.data.repository.RecordingFileRepositoryImpl(
            audioFileManager = audioFileManager,
            audioRecorder = audioRecorder,
            recordingAudioPlayer = recordingAudioPlayer,
            recordingTimeManager = recordingTimeManager
        )
    }
    
    @Provides
    @ViewModelScoped
    fun provideEnglishWritingTestRepository(
        qaDataRepository: com.na982.opichelper.domain.repository.QaDataRepository,
        ttsController: TtsController,
        audioRecorder: AudioRecorder,
        audioFileManager: AudioFileManager,
        recordingTimeManager: RecordingTimeManager,
        progressTracker: MemorizationProgressTracker
    ): EnglishWritingTestRepository {
        return com.na982.opichelper.data.repository.EnglishWritingTestRepositoryImpl(
            qaDataRepository = qaDataRepository,
            ttsController = ttsController,
            audioRecorder = audioRecorder,
            audioFileManager = audioFileManager,
            recordingTimeManager = recordingTimeManager,
            progressTracker = progressTracker
        )
    }
    
    @Provides
    @ViewModelScoped
    fun provideFullMemorizationRepository(
        ttsController: TtsController,
        audioRecorder: AudioRecorder,
        audioPlayer: AudioPlayer,
        audioFileManager: AudioFileManager,
        qaDataRepository: com.na982.opichelper.domain.repository.QaDataRepository
    ): FullMemorizationRepository {
        return com.na982.opichelper.data.repository.FullMemorizationRepositoryImpl(
            ttsController = ttsController,
            audioRecorder = audioRecorder,
            audioPlayer = audioPlayer,
            audioFileManager = audioFileManager,
            qaDataRepository = qaDataRepository
        )
    }
    
    @Provides
    @ViewModelScoped
    fun providePlayRecordingUseCase(
        recordingAudioPlayer: com.na982.opichelper.domain.audio.RecordingAudioPlayer,
        recordingFileRepository: RecordingFileRepository,
        recordingTimeManager: RecordingTimeManager,
        highlightManager: com.na982.opichelper.domain.audio.HighlightManager,
        highlightEventHandler: com.na982.opichelper.domain.event.HighlightEventHandler
    ): PlayRecordingUseCase {
        return PlayRecordingUseCase(
            recordingAudioPlayer = recordingAudioPlayer,
            recordingFileRepository = recordingFileRepository,
            recordingTimeManager = recordingTimeManager,
            highlightManager = highlightManager,
            highlightEventHandler = highlightEventHandler
        )
    }
    @Provides
    @ViewModelScoped
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): com.na982.opichelper.domain.repository.AuthRepository {
        return com.na982.opichelper.data.repository.AuthRepositoryImpl(context)
    }
    
    // UserPreferencesRepository는 AppModule에서 @Singleton으로 제공됨
    
    @Provides
    @ViewModelScoped
    fun provideLearningPreferencesRepository(
        @ApplicationContext context: Context
    ): com.na982.opichelper.domain.repository.LearningPreferencesRepository {
        return com.na982.opichelper.data.repository.LearningPreferencesRepositoryImpl.getInstance(context)
    }
    
    @Provides
    @ViewModelScoped
    fun provideTtsSettingsRepository(
        @ApplicationContext context: Context
    ): com.na982.opichelper.domain.repository.TtsSettingsRepository {
        return com.na982.opichelper.data.repository.TtsSettingsRepositoryImpl.getInstance(context)
    }
    
    // QaDataLoader는 AppModule에서 @Singleton으로 제공됨
    
    // AppModule에서 제공됨
    
    @Provides
    @ViewModelScoped
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): android.content.SharedPreferences {
        return context.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
    }
    
    // AppModule에서 제공됨
    
    // EnglishWritingTestRepository는 ActivityComponent 의존성 때문에 ViewModelModule에서 제공 불가
    // 대신 ViewModel에서 직접 생성하거나 다른 방법 사용
    
    @Provides
    @ViewModelScoped
    fun provideRepeatListeningRepository(
        repeatListeningUseCase: StartRepeatListeningUseCase,
        progressTracker: MemorizationProgressTracker
    ): com.na982.opichelper.domain.repository.RepeatListeningRepository {
        return com.na982.opichelper.data.repository.RepeatListeningRepositoryImpl(
            repeatListeningUseCase = repeatListeningUseCase,
            progressTracker = progressTracker
        )
    }
    
    // FullMemorizationRepository는 ActivityComponent 의존성 때문에 ViewModelModule에서 제공 불가
    
        // ===== UseCase Layer =====
    // UseCase들은 @ViewModelScoped로 직접 주입되므로 별도 제공 불필요
    
        // ===== Strategy Pattern =====
    // Strategy들은 @ViewModelScoped로 직접 주입되므로 별도 제공 불필요
    
    // ===== Event Handlers =====
    // Event Handler들은 @ViewModelScoped로 직접 주입되므로 별도 제공 불필요
    
    @Provides
    @ViewModelScoped
    fun provideIMemorizationManager(
        memorizationManager: MemorizationManager
    ): IMemorizationManager {
        return memorizationManager
    }
    
    @Provides
    @ViewModelScoped
    fun provideIAudioControlManager(
        ttsController: TtsController
    ): IAudioControlManager {
        // ActivityComponent의 AudioControlManager 대신 ViewModelComponent에서 사용할 수 있는 간단한 구현 제공
        return object : IAudioControlManager {
            override val error: StateFlow<String?> = MutableStateFlow(null)
            
            override fun playQuestion(qaItem: com.na982.opichelper.domain.entity.QaItem, onCompletion: () -> Unit) {
                // TTS만 사용하여 질문 재생
                CoroutineScope(Dispatchers.Main).launch {
                    ttsController.playQuestion(qaItem.questionEn)
                    onCompletion()
                }
            }
            
            override fun playAnswer(qaItem: com.na982.opichelper.domain.entity.QaItem, onCompletion: () -> Unit) {
                // TTS만 사용하여 답변 재생
                CoroutineScope(Dispatchers.Main).launch {
                    ttsController.playAnswer(qaItem.answerEnSentences.joinToString(" "))
                    onCompletion()
                }
            }
            
            override fun stopAllAudio() {
                CoroutineScope(Dispatchers.Main).launch {
                    ttsController.stopTts()
                }
            }
            
            override fun stopSpecificAudio(buttonFunction: String) {
                CoroutineScope(Dispatchers.Main).launch {
                    ttsController.stopTts()
                }
            }
            
            override fun releaseAllAudio() {
                CoroutineScope(Dispatchers.Main).launch {
                    ttsController.stopTts()
                }
            }
            
            override fun reinitializeTtsPlayers() {
                // TTS 재초기화는 ViewModel 범위에서 불필요
            }
            
            override suspend fun cleanupTtsSync() {
                ttsController.stopTts()
            }
        }
    }
}
