package com.na982.opichelper.di

import android.content.Context
import android.content.SharedPreferences
import com.na982.opichelper.data.audio.AudioPlayerImpl
import com.na982.opichelper.data.audio.AudioRecorderImpl
import com.na982.opichelper.data.audio.GoogleTtsPlayer
import com.na982.opichelper.data.audio.HighlightManagerImpl
import com.na982.opichelper.data.audio.RecordingAudioPlayerImpl
import com.na982.opichelper.data.audio.SamsungTtsPlayer
import com.na982.opichelper.data.repository.AudioFileManagerImpl
import com.na982.opichelper.data.repository.AuthRepositoryImpl
import com.na982.opichelper.data.repository.EnglishWritingTestRepositoryImpl
import com.na982.opichelper.data.repository.FullMemorizationRepositoryImpl
import com.na982.opichelper.data.repository.LeveledQaDataLoader
import com.na982.opichelper.data.repository.QaDataLoaderImpl
import com.na982.opichelper.data.repository.RecordingFileRepositoryImpl
import com.na982.opichelper.data.repository.RecordingTimeManagerImpl
import com.na982.opichelper.data.repository.RepeatListeningRepositoryImpl
import com.na982.opichelper.data.repository.UserPreferencesRepositoryImpl
import com.na982.opichelper.domain.audio.AudioPlayer
import com.na982.opichelper.domain.audio.AudioRecorder
import com.na982.opichelper.domain.audio.RecordingAudioPlayer
import com.na982.opichelper.domain.audio.TtsController
import com.na982.opichelper.domain.audio.TtsOrchestrator
import com.na982.opichelper.domain.audio.TtsPlayer
import com.na982.opichelper.domain.event.ButtonEventHandler
import com.na982.opichelper.domain.manager.WakeLockManager
import com.na982.opichelper.domain.manager.CategoryManager
import com.na982.opichelper.domain.manager.AudioControlManager
import com.na982.opichelper.domain.manager.MemorizationManager
import com.na982.opichelper.domain.manager.ICategoryManager
import com.na982.opichelper.domain.manager.IAudioControlManager
import com.na982.opichelper.domain.manager.IMemorizationManager
import com.na982.opichelper.domain.manager.ProgressManager
import com.na982.opichelper.domain.audio.AudioFileManager
import com.na982.opichelper.domain.repository.EnglishWritingTestRepository
import com.na982.opichelper.domain.repository.FullMemorizationRepository
import com.na982.opichelper.domain.repository.ProgressPersistenceService
import com.na982.opichelper.domain.repository.QaDataLoader
import com.na982.opichelper.domain.repository.QaDataRepository
import com.na982.opichelper.domain.repository.RecordingFileRepository
import com.na982.opichelper.domain.repository.RecordingTimeManager
import com.na982.opichelper.domain.repository.RepeatListeningRepository
import com.na982.opichelper.domain.repository.UserPreferencesRepository
import com.na982.opichelper.domain.state.AppStateManager
import com.na982.opichelper.data.state.AppStateManagerImpl
import com.na982.opichelper.domain.usecase.GetLeveledAnswerUseCase
import com.na982.opichelper.domain.usecase.InitializeAppUseCase
import com.na982.opichelper.domain.usecase.LoadCategoriesUseCase
import com.na982.opichelper.domain.usecase.LoadQaItemsUseCase
import com.na982.opichelper.domain.strategy.MemorizationStrategyFactory
import com.na982.opichelper.domain.strategy.RepeatListeningStrategy
import com.na982.opichelper.domain.strategy.EnglishWritingStrategy
import com.na982.opichelper.domain.strategy.FullMemorizationStrategy
import com.na982.opichelper.domain.strategy.MemorizationLevelMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import com.na982.opichelper.data.repository.LearningPreferencesRepositoryImpl
import com.na982.opichelper.data.repository.TtsSettingsRepositoryImpl
import com.na982.opichelper.domain.repository.LearningPreferencesRepository
import com.na982.opichelper.domain.repository.TtsSettingsRepository

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ===== Data Layer Implementations =====
    
    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorderImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioPlayer(): AudioPlayer {
        return AudioPlayerImpl()
    }
    
    @Provides
    @Singleton
    fun provideRecordingAudioPlayer(
        recordingTimeManager: RecordingTimeManager,
        highlightManager: com.na982.opichelper.domain.audio.HighlightManager,
        highlightEventHandler: com.na982.opichelper.domain.event.HighlightEventHandler
    ): RecordingAudioPlayer {
        return RecordingAudioPlayerImpl(recordingTimeManager, highlightManager, highlightEventHandler)
    }
    
    @Provides
    @Singleton
    fun provideHighlightManager(): com.na982.opichelper.domain.audio.HighlightManager {
        return HighlightManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideHighlightEventHandler(appStateManager: AppStateManager): com.na982.opichelper.domain.event.HighlightEventHandler {
        return com.na982.opichelper.domain.event.HighlightEventHandler(appStateManager)
    }

    @Provides
    @Singleton
    fun provideAudioFileManager(@ApplicationContext context: Context): AudioFileManager {
        return AudioFileManagerImpl(context)
    }
    
    // ===== TTS Players =====
    
    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return GoogleTtsPlayer(context)
    }
    
    @Provides
    @Singleton
    @Named("samsung")
    fun provideSamsungTtsPlayer(@ApplicationContext context: Context): TtsPlayer {
        return SamsungTtsPlayer(context)
    }
    
    @Provides
    @Singleton
    fun provideTtsOrchestrator(
        @ApplicationContext context: Context,
        @Named("google") googleTtsPlayer: TtsPlayer,
        @Named("samsung") samsungTtsPlayer: TtsPlayer
    ): TtsOrchestrator {
        return TtsOrchestrator(context, googleTtsPlayer, samsungTtsPlayer)
    }
    
    @Provides
    @Singleton
    fun provideTtsController(
        ttsOrchestrator: TtsOrchestrator,
        appStateManager: AppStateManager
    ): TtsController {
        return com.na982.opichelper.data.audio.TtsControllerImpl(
            ttsOrchestrator = ttsOrchestrator,
            appStateManager = appStateManager
        )
    }
    
    // ===== Repository Layer =====
    
    @Provides
    @Singleton
    fun provideQaDataLoader(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository
    ): QaDataLoader {
        return QaDataLoaderImpl(context, userPreferencesRepository)
    }
    
    @Provides
    @Singleton
    fun provideProgressPersistenceService(@ApplicationContext context: Context): ProgressPersistenceService {
        return ProgressPersistenceService(context)
    }
    
    @Provides
    @Singleton
    fun provideQaDataRepository(
        leveledQaDataLoader: LeveledQaDataLoader,
        userPreferencesRepository: UserPreferencesRepository
    ): com.na982.opichelper.domain.repository.QaDataRepository {
        return com.na982.opichelper.domain.repository.QaDataRepository(leveledQaDataLoader, userPreferencesRepository)
    }
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("opic_prefs", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideRecordingTimeManager(@ApplicationContext context: Context): RecordingTimeManager {
        return RecordingTimeManagerImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideRecordingFileRepository(
        audioFileManager: AudioFileManager,
        audioRecorder: AudioRecorder,
        recordingAudioPlayer: RecordingAudioPlayer,
        recordingTimeManager: RecordingTimeManager
    ): RecordingFileRepository {
        return RecordingFileRepositoryImpl(audioFileManager, audioRecorder, recordingAudioPlayer, recordingTimeManager)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): com.na982.opichelper.domain.repository.AuthRepository {
        return AuthRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideLearningPreferencesRepository(@ApplicationContext context: Context): LearningPreferencesRepository {
        return LearningPreferencesRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideTtsSettingsRepository(@ApplicationContext context: Context): TtsSettingsRepository {
        return TtsSettingsRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideLeveledQaDataLoader(@ApplicationContext context: Context): LeveledQaDataLoader {
        return LeveledQaDataLoader(context)
    }
    
    @Provides
    @Singleton
    fun provideEnglishWritingTestRepository(
        qaDataRepository: QaDataRepository,
        ttsController: TtsController,
        audioRecorder: AudioRecorder,
        audioFileManager: AudioFileManager,
        recordingTimeManager: RecordingTimeManager,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker
    ): EnglishWritingTestRepository {
        return EnglishWritingTestRepositoryImpl(
            qaDataRepository = qaDataRepository,
            ttsController = ttsController,
            audioRecorder = audioRecorder,
            audioFileManager = audioFileManager,
            recordingTimeManager = recordingTimeManager,
            progressTracker = progressTracker
        )
    }
    
    @Provides
    @Singleton
    fun provideRepeatListeningRepository(
        repeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker
    ): RepeatListeningRepository {
        return RepeatListeningRepositoryImpl(
            repeatListeningUseCase = repeatListeningUseCase,
            progressTracker = progressTracker
        )
    }
    
    @Provides
    @Singleton
    fun provideFullMemorizationRepository(
        ttsController: TtsController,
        audioRecorder: AudioRecorder,
        audioPlayer: AudioPlayer,
        audioFileManager: AudioFileManager,
        qaDataRepository: QaDataRepository
    ): FullMemorizationRepository {
        return FullMemorizationRepositoryImpl(
            ttsController = ttsController,
            audioRecorder = audioRecorder,
            audioPlayer = audioPlayer,
            audioFileManager = audioFileManager,
            qaDataRepository = qaDataRepository
        )
    }
    
    // ===== Manager Classes =====
    
    @Provides
    @Singleton
    fun provideWakeLockManager(@ApplicationContext context: Context): WakeLockManager {
        return WakeLockManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAppStateManager(): AppStateManager {
        return AppStateManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideButtonStateManager(): com.na982.opichelper.domain.button.ButtonStateManager {
        return com.na982.opichelper.domain.button.ButtonStateManager()
    }
    
    // Manager Classes
    @Provides
    @Singleton
    fun provideCategoryManager(
        qaDataRepository: QaDataRepository,
        appStateManager: AppStateManager,
        loadCategoriesUseCase: LoadCategoriesUseCase,
        loadQaItemsUseCase: LoadQaItemsUseCase
    ): ICategoryManager {
        return CategoryManager(
            qaDataRepository = qaDataRepository,
            appStateManager = appStateManager,
            loadCategoriesUseCase = loadCategoriesUseCase,
            loadQaItemsUseCase = loadQaItemsUseCase
        )
    }
    
    @Provides
    @Singleton
    fun provideAudioControlManager(
        ttsController: TtsController,
        recordingAudioPlayer: RecordingAudioPlayer,
        ttsOrchestrator: TtsOrchestrator
    ): IAudioControlManager {
        return AudioControlManager(
            ttsController = ttsController,
            recordingAudioPlayer = recordingAudioPlayer,
            ttsOrchestrator = ttsOrchestrator
        )
    }
    
    @Provides
    @Singleton
    fun provideMemorizationManager(
        executeRepeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase,
        executeEnglishWritingTestUseCase: com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase,
        executeFullMemorizationUseCase: com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase,
        getCurrentAnswerUseCase: GetLeveledAnswerUseCase,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker
    ): IMemorizationManager {
        return MemorizationManager(
            executeRepeatListeningUseCase = executeRepeatListeningUseCase,
            executeEnglishWritingTestUseCase = executeEnglishWritingTestUseCase,
            executeFullMemorizationUseCase = executeFullMemorizationUseCase,
            getCurrentAnswerUseCase = getCurrentAnswerUseCase,
            progressTracker = progressTracker
        )
    }
    
    // ===== UseCase Layer =====
    
    @Provides
    @Singleton
    fun provideRepeatListeningUseCase(
        ttsController: TtsController,
        progressTracker: com.na982.opichelper.domain.state.MemorizationProgressTracker,
        recordingTimeManager: RecordingTimeManager,
        appStateManager: AppStateManager
    ): com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase {
        return com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase(
            ttsController = ttsController,
            progressTracker = progressTracker,
            recordingTimeManager = recordingTimeManager,
            appStateManager = appStateManager
        )
    }
    
    @Provides
    @Singleton
    fun provideEnglishWritingTestUseCase(
        englishWritingTestRepository: EnglishWritingTestRepository
    ): com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase {
        return com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase(
            englishWritingTestRepository = englishWritingTestRepository
        )
    }
    
    @Provides
    @Singleton
    fun provideFullMemorizationUseCase(
        fullMemorizationRepository: FullMemorizationRepository
    ): com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase {
        return com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase(
            fullMemorizationRepository = fullMemorizationRepository
        )
    }
    
    @Provides
    @Singleton
    fun providePlayRecordingUseCase(
        recordingAudioPlayer: RecordingAudioPlayer,
        recordingFileRepository: RecordingFileRepository,
        recordingTimeManager: RecordingTimeManager,
        highlightManager: com.na982.opichelper.domain.audio.HighlightManager,
        highlightEventHandler: com.na982.opichelper.domain.event.HighlightEventHandler
    ): com.na982.opichelper.domain.usecase.PlayRecordingUseCase {
        return com.na982.opichelper.domain.usecase.PlayRecordingUseCase(
            recordingAudioPlayer = recordingAudioPlayer,
            recordingFileRepository = recordingFileRepository,
            recordingTimeManager = recordingTimeManager,
            highlightManager = highlightManager,
            highlightEventHandler = highlightEventHandler
        )
    }
    
    @Provides
    @Singleton
    fun provideInitializeAppUseCase(
        qaDataRepository: QaDataRepository,
        userPreferencesRepository: UserPreferencesRepository,
        appStateManager: AppStateManager
    ): InitializeAppUseCase {
        return InitializeAppUseCase(qaDataRepository, userPreferencesRepository, appStateManager)
    }
    
    @Provides
    @Singleton
    fun provideGetLeveledAnswerUseCase(
        userPreferencesRepository: UserPreferencesRepository
    ): GetLeveledAnswerUseCase {
        return GetLeveledAnswerUseCase(userPreferencesRepository)
    }
    
    // ===== Strategy Pattern =====
    
    @Provides
    @Singleton
    fun provideRepeatListeningStrategy(
        repeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
    ): RepeatListeningStrategy {
        return RepeatListeningStrategy(repeatListeningUseCase)
    }
    
    @Provides
    @Singleton
    fun provideEnglishWritingStrategy(
        englishWritingUseCase: com.na982.opichelper.domain.usecase.StartEnglishWritingTestUseCase
    ): EnglishWritingStrategy {
        return EnglishWritingStrategy(englishWritingUseCase)
    }
    
    @Provides
    @Singleton
    fun provideFullMemorizationStrategy(
        fullMemorizationUseCase: com.na982.opichelper.domain.usecase.StartFullMemorizationUseCase
    ): FullMemorizationStrategy {
        return FullMemorizationStrategy(fullMemorizationUseCase)
    }
    
    /**
     * MemorizationLevelMapper 제공
     */
    @Provides
    @Singleton
    fun provideMemorizationLevelMapper(): MemorizationLevelMapper {
        return MemorizationLevelMapper()
    }

    /**
     * MemorizationStrategyFactory 제공
     */
    @Provides
    @Singleton
    fun provideMemorizationStrategyFactory(
        repeatListeningStrategy: RepeatListeningStrategy,
        englishWritingStrategy: EnglishWritingStrategy,
        fullMemorizationStrategy: FullMemorizationStrategy
    ): MemorizationStrategyFactory {
        return MemorizationStrategyFactory(
            repeatListeningStrategy,
            englishWritingStrategy,
            fullMemorizationStrategy
        )
    }
    

    

    
    // ===== Event Handlers =====
    
    @Provides
    @Singleton
    fun provideButtonStateObserver(ttsOrchestrator: TtsOrchestrator): com.na982.opichelper.domain.button.ButtonStateObserver {
        return ttsOrchestrator
    }
    
    @Provides
    @Singleton
    fun provideButtonEventHandler(
        audioControlManager: IAudioControlManager,
        appStateManager: AppStateManager,
        strategyFactory: MemorizationStrategyFactory,
        playRecordingUseCase: com.na982.opichelper.domain.usecase.PlayRecordingUseCase,
        progressManager: ProgressManager,
        startRepeatListeningUseCase: com.na982.opichelper.domain.usecase.StartRepeatListeningUseCase
    ): ButtonEventHandler {
        return ButtonEventHandler(
            audioControlManager = audioControlManager,
            appStateManager = appStateManager,
            strategyFactory = strategyFactory,
            playRecordingUseCase = playRecordingUseCase,
            progressManager = progressManager,
            startRepeatListeningUseCase = startRepeatListeningUseCase
        )
    }
} 